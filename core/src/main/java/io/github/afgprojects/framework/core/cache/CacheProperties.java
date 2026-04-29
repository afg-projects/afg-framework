package io.github.afgprojects.framework.core.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 缓存配置属性
 * <p>
 * 配置示例：
 * <pre>{@code
 * afg:
 *   cache:
 *     enabled: true
 *     type: multi-level
 *     default-ttl: 3600000
 *     local:
 *       maximum-size: 10000
 *       expire-after-write: 5m
 *     distributed:
 *       enabled: true
 *       key-prefix: "afg:cache:"
 * }</pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "afg.cache")
public class CacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 缓存类型：local、distributed、multi-level
     */
    private CacheType type = CacheType.LOCAL;

    /**
     * 默认过期时间（毫秒）
     */
    private long defaultTtl = 0;

    /**
     * 是否缓存 null 值（防穿透）
     */
    private boolean cacheNull = true;

    /**
     * 空值缓存过期时间（毫秒）
     */
    private long nullValueTtl = 60000;

    /**
     * 本地缓存配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * 分布式缓存配置
     */
    private DistributedConfig distributed = new DistributedConfig();

    /**
     * 命名缓存配置
     */
    private Map<String, CacheConfig> caches = new HashMap<>();

    /**
     * 缓存类型枚举
     */
    public enum CacheType {
        /**
         * 本地缓存
         */
        LOCAL,
        /**
         * 分布式缓存
         */
        DISTRIBUTED,
        /**
         * 多级缓存
         */
        MULTI_LEVEL
    }

    /**
     * 本地缓存配置
     */
    @Data
    public static class LocalConfig {

        /**
         * 是否启用本地缓存
         */
        private boolean enabled = true;

        /**
         * 初始容量
         */
        private int initialCapacity = 128;

        /**
         * 最大容量
         */
        private int maximumSize = 10000;

        /**
         * 写入后过期时间
         */
        private Duration expireAfterWrite;

        /**
         * 访问后过期时间
         */
        private Duration expireAfterAccess;

        /**
         * 是否开启统计
         */
        private boolean recordStats = true;
    }

    /**
     * 分布式缓存配置
     */
    @Data
    public static class DistributedConfig {

        /**
         * 是否启用分布式缓存
         */
        private boolean enabled = true;

        /**
         * 缓存键前缀
         */
        private String keyPrefix = "afg:cache:";

        /**
         * 默认过期时间（毫秒），0 表示永不过期
         */
        private long defaultTtl = 0;
    }

    /**
     * 转换为 CacheConfig
     *
     * @return CacheConfig 实例
     */
    public CacheConfig toCacheConfig() {
        CacheConfig config = new CacheConfig();
        config.defaultTtl(defaultTtl);
        config.cacheNull(cacheNull);
        config.nullValueTtl(nullValueTtl);
        config.initialCapacity(local.getInitialCapacity());
        config.maximumSize(local.getMaximumSize());
        config.expireAfterWrite(local.getExpireAfterWrite());
        config.expireAfterAccess(local.getExpireAfterAccess());
        config.recordStats(local.isRecordStats());
        return config;
    }

    /**
     * 获取指定缓存的配置
     *
     * @param cacheName 缓存名称
     * @return 缓存配置
     */
    public CacheConfig getCacheConfig(String cacheName) {
        CacheConfig customConfig = caches.get(cacheName);
        if (customConfig != null) {
            return customConfig;
        }
        return toCacheConfig();
    }
}