package io.github.afgprojects.framework.core.properties.ratelimit;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 限流配置。
 */
@Data
public class AfgCoreRateLimitProperties {

    /**
     * 是否启用限流。
     */
    private boolean enabled = true;

    /**
     * 默认每秒请求数。
     */
    private long defaultRate = 10;

    /**
     * 默认突发容量。
     */
    private long defaultBurst = 0;

    /**
     * 默认限流算法。
     */
    private RateLimitAlgorithm defaultAlgorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * Redis key 前缀。
     */
    private String keyPrefix = "rateLimit";

    /**
     * 存储类型。
     */
    private String storageType = "local";

    /**
     * 回退配置。
     */
    private AfgCoreRateLimitFallbackProperties fallback = new AfgCoreRateLimitFallbackProperties();

    /**
     * 白名单配置。
     */
    private AfgCoreRateLimitWhitelistProperties whitelist = new AfgCoreRateLimitWhitelistProperties();

    /**
     * 响应头配置。
     */
    private AfgCoreRateLimitResponseHeadersProperties responseHeaders = new AfgCoreRateLimitResponseHeadersProperties();

    /**
     * 本地限流配置。
     */
    private AfgCoreRateLimitLocalProperties local = new AfgCoreRateLimitLocalProperties();

    /**
     * 维度配置映射。
     * key 为维度名称（ip, user, tenant, api）。
     */
    private Map<String, AfgCoreRateLimitDimensionProperties> dimensions = new HashMap<>();
}
