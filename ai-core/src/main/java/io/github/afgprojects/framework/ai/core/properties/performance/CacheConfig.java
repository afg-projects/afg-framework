package io.github.afgprojects.framework.ai.core.properties.performance;

import lombok.Data;

/**
 * 缓存配置。
 */
@Data
public class CacheConfig {

    /**
     * 缓存最大条目数。
     */
    private int maxSize = 1000;

    /**
     * 缓存过期时间（秒）。
     */
    private long ttlSeconds = 300;
}
