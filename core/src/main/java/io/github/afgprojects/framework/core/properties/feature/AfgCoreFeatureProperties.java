package io.github.afgprojects.framework.core.properties.feature;

import lombok.Data;

/**
 * 功能开关配置。
 */
@Data
public class AfgCoreFeatureProperties {

    /**
     * 是否启用功能开关。
     */
    private boolean enabled = true;

    /**
     * 存储类型。
     */
    private FeatureStorageType storageType = FeatureStorageType.MEMORY;

    /**
     * Redis 配置。
     */
    private AfgCoreFeatureRedisProperties redis = new AfgCoreFeatureRedisProperties();

    /**
     * 默认灰度策略。
     */
    private GrayscaleStrategy defaultStrategy = GrayscaleStrategy.ALL;

    /**
     * 功能开关缓存过期时间（秒）。
     */
    private long cacheExpireSeconds = 60;

    /**
     * 是否启用本地缓存。
     */
    private boolean localCacheEnabled = true;

    /**
     * 本地缓存最大大小。
     */
    private int localCacheMaxSize = 1000;
}
