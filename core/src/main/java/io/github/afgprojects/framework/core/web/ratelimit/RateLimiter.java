package io.github.afgprojects.framework.core.web.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.ratelimit.RateLimitProperties.DimensionConfig;
import io.github.afgprojects.framework.core.web.ratelimit.RateLimitProperties.Local;

/**
 * 限流器
 * <p>
 * 基于 Redisson RRateLimiter 实现分布式令牌桶限流。
 * 支持多种限流维度：IP、用户、租户、接口。
 * 支持两种限流算法：令牌桶、滑动窗口。
 * </p>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final @Nullable RedissonClient redissonClient;
    private final RateLimitProperties properties;
    private final RateLimitWhitelistChecker whitelistChecker;

    // 本地限流缓存（用于单机模式或降级）
    private final @Nullable Cache<String, RateLimitCounter> localRateLimiter;

    /**
     * 构造函数
     *
     * @param redissonClient Redisson 客户端（可为 null，使用本地限流）
     * @param properties     限流配置属性
     */
    public RateLimiter(@Nullable RedissonClient redissonClient, RateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.whitelistChecker = new RateLimitWhitelistChecker(properties);

        // 初始化本地限流缓存
        Local localConfig = properties.getLocal();
        if (localConfig.isEnabled() || redissonClient == null) {
            this.localRateLimiter = Caffeine.newBuilder()
                    .maximumSize(localConfig.getCacheSize())
                    .expireAfterWrite(Duration.ofSeconds(localConfig.getExpireAfterSeconds()))
                    .build();
        } else {
            this.localRateLimiter = null;
        }
    }

    /**
     * 尝试获取令牌
     *
     * @param annotation 限流注解
     * @return 限流结果
     */
    public RateLimitResult tryAcquire(RateLimit annotation) {
        // 检查白名单
        if (whitelistChecker.isInWhitelist(annotation.dimension())) {
            long rate = resolveRate(annotation);
            long burst = resolveBurst(annotation, rate);
            return RateLimitResult.allowed(burst, burst, System.currentTimeMillis() + 1000);
        }

        String key = buildKey(annotation);
        long rate = resolveRate(annotation);
        long burst = resolveBurst(annotation, rate);
        RateLimitAlgorithm algorithm = resolveAlgorithm(annotation);
        long windowSize = resolveWindowSize(annotation);

        try {
            RateLimitResult result;

            if (redissonClient != null) {
                result = distributedRateLimit(key, rate, burst, algorithm, windowSize);
            } else if (localRateLimiter != null) {
                result = localRateLimit(key, rate, burst, algorithm, windowSize);
            } else {
                // 既没有 Redis 也没有本地限流，放行
                log.warn("No rate limiter available, allowing request: {}", key);
                return RateLimitResult.allowed(burst, burst, System.currentTimeMillis() + 1000);
            }

            if (!result.allowed()) {
                log.warn("Rate limit exceeded for key: {}", key);
            }

            return result;
        } catch (Exception e) {
            log.error("Rate limiter error for key: {}", key, e);
            // 根据配置的故障模式决定行为
            if (properties.getFallback().getFailureMode() == RateLimitProperties.FailureMode.REJECT) {
                log.warn("Rate limiter failure mode is REJECT, blocking request: {}", key);
                return RateLimitResult.rejected(0, 0, System.currentTimeMillis() + 1000);
            }
            // 默认：限流器异常时放行，避免影响业务
            return RateLimitResult.allowed(burst, burst, System.currentTimeMillis() + 1000);
        }
    }

    /**
     * 分布式限流
     *
     * @param key       限流 key
     * @param rate      速率
     * @param burst     突发容量
     * @param algorithm 算法
     * @param windowSize 窗口大小
     * @return 限流结果
     */
    private RateLimitResult distributedRateLimit(String key, long rate, long burst,
            RateLimitAlgorithm algorithm, long windowSize) {
        assert redissonClient != null;

        if (algorithm == RateLimitAlgorithm.SLIDING_WINDOW) {
            return distributedSlidingWindow(key, rate, windowSize);
        }

        // 令牌桶算法
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, 1, RateIntervalUnit.SECONDS);

        boolean acquired = rateLimiter.tryAcquire(1);

        // 获取剩余令牌数
        long remaining = 0;
        try {
            // Redisson RRateLimiter 没有直接获取剩余令牌的方法，使用近似值
            remaining = acquired ? Math.max(0, burst - 1) : 0;
        } catch (Exception e) {
            log.debug("Failed to get remaining tokens for key: {}", key);
        }

        if (acquired) {
            return RateLimitResult.allowed(remaining, burst, System.currentTimeMillis() + 1000);
        } else {
            return RateLimitResult.rejected(burst, System.currentTimeMillis() + 1000, 1000 / rate + 1);
        }
    }

    /**
     * 分布式滑动窗口限流
     *
     * @param key        限流 key
     * @param rate       速率
     * @param windowSize 窗口大小（秒）
     * @return 限流结果
     */
    private RateLimitResult distributedSlidingWindow(String key, long rate, long windowSize) {
        assert redissonClient != null;

        long windowSizeMs = windowSize * 1000;
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeMs;

        String counterKey = key + ":counter";
        String timestampKey = key + ":timestamp";

        try {
            // 使用 Redis 原子操作实现滑动窗口
            Long currentCount = redissonClient.getAtomicLong(counterKey).get();
            Long lastTimestamp = redissonClient.getAtomicLong(timestampKey).get();

            if (lastTimestamp == null || lastTimestamp < windowStart) {
                // 窗口过期，重置计数器
                redissonClient.getAtomicLong(counterKey).set(1);
                redissonClient.getAtomicLong(timestampKey).set(now);
                redissonClient.getAtomicLong(counterKey).expire(windowSize, TimeUnit.SECONDS);
                redissonClient.getAtomicLong(timestampKey).expire(windowSize, TimeUnit.SECONDS);
                return RateLimitResult.allowed(rate - 1, rate, now + windowSizeMs);
            }

            currentCount = currentCount == null ? 0 : currentCount;

            if (currentCount < rate) {
                // 在限流范围内，增加计数
                redissonClient.getAtomicLong(counterKey).incrementAndGet();
                return RateLimitResult.allowed(rate - currentCount - 1, rate, lastTimestamp + windowSizeMs);
            } else {
                // 超过限流
                long retryAfter = lastTimestamp + windowSizeMs - now;
                return RateLimitResult.rejected(rate, lastTimestamp + windowSizeMs, retryAfter);
            }
        } catch (Exception e) {
            log.error("Sliding window rate limiter error for key: {}", key, e);
            return RateLimitResult.allowed(rate, rate, now + windowSizeMs);
        }
    }

    /**
     * 本地限流
     *
     * @param key        限流 key
     * @param rate       速率
     * @param burst      突发容量
     * @param algorithm  算法
     * @param windowSize 窗口大小
     * @return 限流结果
     */
    private RateLimitResult localRateLimit(String key, long rate, long burst,
            RateLimitAlgorithm algorithm, long windowSize) {
        assert localRateLimiter != null;

        RateLimitCounter counter = localRateLimiter.get(key, k -> {
            RateLimitCounter newCounter = new RateLimitCounter();
            // 初始化时令牌桶是满的
            newCounter.tokens = burst;
            return newCounter;
        });
        long now = System.currentTimeMillis();

        if (algorithm == RateLimitAlgorithm.SLIDING_WINDOW) {
            return localSlidingWindow(counter, rate, windowSize, now);
        }

        // 令牌桶算法
        return localTokenBucket(counter, rate, burst, now);
    }

    /**
     * 本地令牌桶限流
     *
     * @param counter 计数器
     * @param rate    速率
     * @param burst   突发容量
     * @param now     当前时间
     * @return 限流结果
     */
    private RateLimitResult localTokenBucket(RateLimitCounter counter, long rate, long burst, long now) {
        synchronized (counter) {
            long elapsed = now - counter.lastRefillTime;
            long tokensToAdd = (elapsed * rate) / 1000;

            counter.tokens = Math.min(burst, counter.tokens + tokensToAdd);
            counter.lastRefillTime = now;

            if (counter.tokens >= 1) {
                counter.tokens--;
                return RateLimitResult.allowed((long) counter.tokens, burst, now + 1000);
            } else {
                long retryAfter = (long) Math.ceil(1000.0 / rate);
                return RateLimitResult.rejected(burst, now + retryAfter, retryAfter);
            }
        }
    }

    /**
     * 本地滑动窗口限流
     *
     * @param counter    计数器
     * @param rate       速率
     * @param windowSize 窗口大小（秒）
     * @param now        当前时间
     * @return 限流结果
     */
    private RateLimitResult localSlidingWindow(RateLimitCounter counter, long rate, long windowSize, long now) {
        synchronized (counter) {
            long windowSizeMs = windowSize * 1000;
            long windowStart = now - windowSizeMs;

            // 清理过期的请求
            counter.requests.removeIf(timestamp -> timestamp < windowStart);

            if (counter.requests.size() < rate) {
                counter.requests.add(now);
                return RateLimitResult.allowed(rate - counter.requests.size() - 1, rate, now + windowSizeMs);
            } else {
                long oldestRequest = counter.requests.isEmpty() ? now : counter.requests.get(0);
                long retryAfter = oldestRequest + windowSizeMs - now;
                return RateLimitResult.rejected(rate, now + windowSizeMs, retryAfter);
            }
        }
    }

    /**
     * 构建限流 key
     *
     * @param annotation 限流注解
     * @return 完整的限流 key
     */
    public String buildKey(RateLimit annotation) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(properties.getKeyPrefix()).append(":");
        keyBuilder.append(annotation.key()).append(":");
        keyBuilder.append(annotation.dimension().name().toLowerCase()).append(":");
        keyBuilder.append(getDimensionValue(annotation.dimension()));

        return keyBuilder.toString();
    }

    /**
     * 获取限流维度值
     *
     * @param dimension 限流维度
     * @return 维度值
     */
    private String getDimensionValue(RateLimitDimension dimension) {
        // API 维度不需要上下文
        if (dimension == RateLimitDimension.API) {
            return "global";
        }

        RequestContext context = AfgRequestContextHolder.getContext();

        if (context == null) {
            return "unknown";
        }

        return switch (dimension) {
            case IP -> {
                String clientIp = context.getClientIp();
                yield clientIp != null ? clientIp : "unknown";
            }
            case USER -> {
                Long userId = context.getUserId();
                yield userId != null ? userId.toString() : "anonymous";
            }
            case TENANT -> {
                Long tenantId = context.getTenantId();
                yield tenantId != null ? tenantId.toString() : "default";
            }
            case API -> "global";
        };
    }

    /**
     * 解析每秒请求数
     *
     * @param annotation 限流注解
     * @return 每秒请求数
     */
    private long resolveRate(RateLimit annotation) {
        if (annotation.rate() > 0) {
            return annotation.rate();
        }

        // 从维度配置中获取默认值
        String dimensionKey = annotation.dimension().name().toLowerCase();
        DimensionConfig config = properties.getDimensions().get(dimensionKey);
        if (config != null && config.getRate() > 0) {
            return config.getRate();
        }

        return properties.getDefaultRate();
    }

    /**
     * 解析突发容量
     *
     * @param annotation 限流注解
     * @param rate       每秒请求数
     * @return 突发容量
     */
    private long resolveBurst(RateLimit annotation, long rate) {
        if (annotation.burst() > 0) {
            return annotation.burst();
        }

        // 从维度配置中获取默认值
        String dimensionKey = annotation.dimension().name().toLowerCase();
        DimensionConfig config = properties.getDimensions().get(dimensionKey);
        if (config != null && config.getBurst() > 0) {
            return config.getBurst();
        }

        // 默认突发容量为 rate * 2
        if (properties.getDefaultBurst() > 0) {
            return properties.getDefaultBurst();
        }

        return rate * 2;
    }

    /**
     * 解析限流算法
     *
     * @param annotation 限流注解
     * @return 限流算法
     */
    private RateLimitAlgorithm resolveAlgorithm(RateLimit annotation) {
        if (annotation.algorithm() != RateLimitAlgorithm.TOKEN_BUCKET) {
            return annotation.algorithm();
        }

        // 从维度配置中获取默认值
        String dimensionKey = annotation.dimension().name().toLowerCase();
        DimensionConfig config = properties.getDimensions().get(dimensionKey);
        if (config != null && config.getAlgorithm() != null) {
            return config.getAlgorithm();
        }

        return properties.getDefaultAlgorithm();
    }

    /**
     * 解析时间窗口大小
     *
     * @param annotation 限流注解
     * @return 时间窗口大小（秒）
     */
    private long resolveWindowSize(RateLimit annotation) {
        if (annotation.windowSize() > 0) {
            return annotation.windowSize();
        }

        // 从维度配置中获取默认值
        String dimensionKey = annotation.dimension().name().toLowerCase();
        DimensionConfig config = properties.getDimensions().get(dimensionKey);
        if (config != null && config.getWindowSize() > 0) {
            return config.getWindowSize();
        }

        return 1;
    }

    /**
     * 获取限流失败错误码
     *
     * @return 错误码
     */
    public int getRateLimitErrorCode() {
        return CommonErrorCode.RATE_LIMIT_EXCEEDED.getCode();
    }

    /**
     * 获取限流失败消息
     *
     * @param annotation 限流注解
     * @return 错误消息
     */
    public String getRateLimitMessage(RateLimit annotation) {
        String message = annotation.message();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return properties.getFallback().getDefaultMessage();
    }

    /**
     * 获取当前限流配置
     *
     * @param annotation 限流注解
     * @return 限流配置信息
     */
    @Nullable
    public RateLimitInfo getRateLimitInfo(RateLimit annotation) {
        String key = buildKey(annotation);
        try {
            if (redissonClient != null) {
                RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
                if (rateLimiter.isExists()) {
                    return new RateLimitInfo(key, resolveRate(annotation), resolveBurst(annotation, resolveRate(annotation)));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get rate limit info for key: {}", key, e);
        }
        return null;
    }

    /**
     * 重置限流器
     *
     * @param annotation 限流注解
     * @return 是否重置成功
     */
    public boolean reset(RateLimit annotation) {
        String key = buildKey(annotation);
        try {
            if (redissonClient != null) {
                RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
                rateLimiter.delete();
            }
            if (localRateLimiter != null) {
                localRateLimiter.invalidate(key);
            }
            log.info("Rate limiter reset for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to reset rate limiter for key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取白名单检查器
     *
     * @return 白名单检查器
     */
    public RateLimitWhitelistChecker getWhitelistChecker() {
        return whitelistChecker;
    }

    /**
     * 限流信息记录
     */
    public record RateLimitInfo(String key, long rate, long burst) {}

    /**
     * 本地限流计数器
     */
    private static class RateLimitCounter {
        volatile double tokens;
        volatile long lastRefillTime = System.currentTimeMillis();
        final java.util.List<Long> requests = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}
