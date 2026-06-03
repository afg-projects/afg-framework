package io.github.afgprojects.framework.core.properties.cache;

import lombok.Data;

/**
 * 缓存配置。
 */
@Data
public class AfgCoreCacheProperties {

    /**
     * 是否启用缓存。
     */
    private boolean enabled = true;

    /**
     * 缓存类型：local、distributed、multi-level。
     */
    private CacheType type = CacheType.LOCAL;

    /**
     * 默认过期时间（毫秒）。
     */
    private long defaultTtl = 0;

    /**
     * 是否缓存 null 值（防穿透）。
     */
    private boolean cacheNull = true;

    /**
     * 空值缓存过期时间（毫秒）。
     */
    private long nullValueTtl = 60000;

    /**
     * 本地缓存配置。
     */
    private AfgCoreCacheLocalProperties local = new AfgCoreCacheLocalProperties();

    /**
     * 分布式缓存配置。
     */
    private AfgCoreCacheDistributedProperties distributed = new AfgCoreCacheDistributedProperties();
}
