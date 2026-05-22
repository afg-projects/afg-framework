package io.github.afgprojects.framework.core.api.ratelimit;

import lombok.extern.slf4j.Slf4j;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.config.AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm;

/**
 * 限流器入口
 * <p>
 * 提供统一的限流操作入口，使用 Builder 风格 API。
 * </p>
 *
 * <pre>{@code
 * RateLimitResult result = rateLimiter.builder()
 *     .key("api:login")
 *     .dimension(RateLimitDimension.IP)
 *     .rate(10)
 *     .burst(20)
 *     .tryAcquire();
 * }</pre>
 */
@Slf4j
public class RateLimiter {

    private final RateLimitStorage storage;
    private final AfgCoreProperties properties;
    private final WhitelistStrategy whitelistStrategy;
    private final DimensionResolver dimensionResolver;

    /**
     * 构造函数
     *
     * @param storage            限流存储
     * @param properties         核心配置属性
     * @param whitelistStrategy  白名单策略
     * @param dimensionResolver  维度解析器
     */
    public RateLimiter(RateLimitStorage storage, AfgCoreProperties properties,
                       WhitelistStrategy whitelistStrategy, DimensionResolver dimensionResolver) {
        this.storage = storage;
        this.properties = properties;
        this.whitelistStrategy = whitelistStrategy;
        this.dimensionResolver = dimensionResolver;
    }

    /**
     * 创建限流构建器
     *
     * @return 限流构建器
     */
    public RateLimiterBuilder builder() {
        return new RateLimiterBuilder(this);
    }

    /**
     * 执行限流检查
     *
     * @param key        完整的限流 key
     * @param rate       每秒请求数
     * @param burst      突发容量
     * @param algorithm  限流算法
     * @param windowSize 窗口大小（秒）
     * @return 限流结果
     */
    RateLimitResult doTryAcquire(String key, long rate, long burst,
                                  RateLimitAlgorithm algorithm, long windowSize) {
        try {
            return switch (algorithm) {
                case TOKEN_BUCKET -> storage.tryAcquireTokenBucket(key, rate, burst);
                case SLIDING_WINDOW -> storage.tryAcquireSlidingWindow(key, rate, windowSize);
                case FIXED_WINDOW, LEAKY_BUCKET -> storage.tryAcquireSlidingWindow(key, rate, windowSize);
            };
        } catch (Exception e) {
            log.error("Rate limiter error for key: {}", key, e);
            // 根据配置的故障模式决定行为
            if (properties.getRateLimit().getFallback().getFailureMode() == AfgCoreProperties.RateLimitConfig.FailureMode.REJECT) {
                log.warn("Rate limiter failure mode is REJECT, blocking request: {}", key);
                return RateLimitResult.rejected(burst, System.currentTimeMillis() + 1000, 1000);
            }
            // 默认：限流器异常时放行，避免影响业务
            return RateLimitResult.allowed(burst, burst, System.currentTimeMillis() + 1000);
        }
    }

    /**
     * 检查是否在白名单中
     *
     * @param dimension 限流维度
     * @return 是否在白名单中
     */
    boolean isInWhitelist(RateLimitDimension dimension) {
        return whitelistStrategy.isInWhitelist(dimension);
    }

    /**
     * 解析维度值
     *
     * @param dimension 限流维度
     * @return 维度值
     */
    String resolveDimensionValue(RateLimitDimension dimension) {
        return dimensionResolver.resolve(dimension);
    }

    /**
     * 获取配置属性
     *
     * @return 配置属性
     */
    AfgCoreProperties getProperties() {
        return properties;
    }
}
