package io.github.afgprojects.framework.core.api.ratelimit;

import io.github.afgprojects.framework.core.web.ratelimit.RateLimitProperties;
import io.github.afgprojects.framework.core.web.ratelimit.RateLimitProperties.DimensionConfig;

/**
 * 限流构建器
 * <p>
 * 提供 Builder 风格的限流配置 API。
 * </p>
 *
 * <pre>{@code
 * RateLimitResult result = rateLimiter.builder()
 *     .key("api:login")
 *     .dimension(RateLimitDimension.IP)
 *     .rate(10)
 *     .burst(20)
 *     .algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
 *     .tryAcquire();
 * }</pre>
 */
public class RateLimiterBuilder {

    private final RateLimiter rateLimiter;

    private String key;
    private RateLimitDimension dimension = RateLimitDimension.API;
    private long rate = -1;
    private long burst = -1;
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
    private long windowSize = 1;

    /**
     * 构造函数
     *
     * @param rateLimiter 限流器
     */
    public RateLimiterBuilder(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * 设置限流 key
     *
     * @param key 限流 key
     * @return this
     */
    public RateLimiterBuilder key(String key) {
        this.key = key;
        return this;
    }

    /**
     * 设置限流维度
     *
     * @param dimension 限流维度
     * @return this
     */
    public RateLimiterBuilder dimension(RateLimitDimension dimension) {
        this.dimension = dimension;
        return this;
    }

    /**
     * 设置每秒请求数
     *
     * @param rate 每秒请求数
     * @return this
     */
    public RateLimiterBuilder rate(long rate) {
        this.rate = rate;
        return this;
    }

    /**
     * 设置突发容量
     *
     * @param burst 突发容量
     * @return this
     */
    public RateLimiterBuilder burst(long burst) {
        this.burst = burst;
        return this;
    }

    /**
     * 设置限流算法
     *
     * @param algorithm 限流算法
     * @return this
     */
    public RateLimiterBuilder algorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    /**
     * 设置时间窗口大小
     *
     * @param windowSize 窗口大小（秒）
     * @return this
     */
    public RateLimiterBuilder windowSize(long windowSize) {
        this.windowSize = windowSize;
        return this;
    }

    /**
     * 尝试获取许可
     *
     * @return 限流结果
     */
    public RateLimitResult tryAcquire() {
        // 检查白名单
        if (rateLimiter.isInWhitelist(dimension)) {
            long effectiveBurst = burst > 0 ? burst : resolveBurst(resolveRate());
            return RateLimitResult.allowed(effectiveBurst, effectiveBurst,
                System.currentTimeMillis() + 1000);
        }

        // 构建完整 key
        String fullKey = buildKey();

        // 解析参数
        long effectiveRate = resolveRate();
        long effectiveBurst = resolveBurst(effectiveRate);
        long effectiveWindowSize = resolveWindowSize();
        RateLimitAlgorithm effectiveAlgorithm = resolveAlgorithm();

        return rateLimiter.doTryAcquire(fullKey, effectiveRate, effectiveBurst,
            effectiveAlgorithm, effectiveWindowSize);
    }

    /**
     * 构建完整的限流 key
     */
    private String buildKey() {
        RateLimitProperties properties = rateLimiter.getProperties();
        return properties.getKeyPrefix() + ":" + key + ":" +
            dimension.name().toLowerCase() + ":" +
            rateLimiter.resolveDimensionValue(dimension);
    }

    /**
     * 解析每秒请求数
     */
    private long resolveRate() {
        if (rate > 0) {
            return rate;
        }

        // 从维度配置中获取默认值
        String dimensionKey = dimension.name().toLowerCase();
        DimensionConfig config = rateLimiter.getProperties().getDimensions().get(dimensionKey);
        if (config != null && config.getRate() > 0) {
            return config.getRate();
        }

        return rateLimiter.getProperties().getDefaultRate();
    }

    /**
     * 解析突发容量
     */
    private long resolveBurst(long effectiveRate) {
        if (burst > 0) {
            return burst;
        }

        // 从维度配置中获取默认值
        String dimensionKey = dimension.name().toLowerCase();
        DimensionConfig config = rateLimiter.getProperties().getDimensions().get(dimensionKey);
        if (config != null && config.getBurst() > 0) {
            return config.getBurst();
        }

        // 默认突发容量为 rate * 2
        if (rateLimiter.getProperties().getDefaultBurst() > 0) {
            return rateLimiter.getProperties().getDefaultBurst();
        }

        return effectiveRate * 2;
    }

    /**
     * 解析时间窗口大小
     */
    private long resolveWindowSize() {
        if (windowSize > 1) {
            return windowSize;
        }

        // 从维度配置中获取默认值
        String dimensionKey = dimension.name().toLowerCase();
        DimensionConfig config = rateLimiter.getProperties().getDimensions().get(dimensionKey);
        if (config != null && config.getWindowSize() > 0) {
            return config.getWindowSize();
        }

        return 1;
    }

    /**
     * 解析限流算法
     */
    private RateLimitAlgorithm resolveAlgorithm() {
        if (algorithm != RateLimitAlgorithm.TOKEN_BUCKET) {
            return algorithm;
        }

        // 从维度配置中获取默认值
        String dimensionKey = dimension.name().toLowerCase();
        DimensionConfig config = rateLimiter.getProperties().getDimensions().get(dimensionKey);
        if (config != null && config.getAlgorithm() != null) {
            return config.getAlgorithm();
        }

        return rateLimiter.getProperties().getDefaultAlgorithm();
    }
}
