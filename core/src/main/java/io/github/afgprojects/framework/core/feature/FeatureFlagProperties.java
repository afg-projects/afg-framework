package io.github.afgprojects.framework.core.feature;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 功能开关配置属性
 */
@ConfigurationProperties(prefix = "afg.feature")
public class FeatureFlagProperties {

    /**
     * 是否启用功能开关
     */
    private boolean enabled = true;

    /**
     * 存储类型
     */
    private StorageType storageType = StorageType.MEMORY;

    /**
     * Redis 配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * 默认灰度策略
     */
    private GrayscaleStrategy defaultStrategy = GrayscaleStrategy.ALL;

    /**
     * 功能开关缓存过期时间（秒）
     */
    private long cacheExpireSeconds = 60;

    /**
     * 是否启用本地缓存
     */
    private boolean localCacheEnabled = true;

    /**
     * 本地缓存最大大小
     */
    private int localCacheMaxSize = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public GrayscaleStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(GrayscaleStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public long getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public void setCacheExpireSeconds(long cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
    }

    public boolean isLocalCacheEnabled() {
        return localCacheEnabled;
    }

    public void setLocalCacheEnabled(boolean localCacheEnabled) {
        this.localCacheEnabled = localCacheEnabled;
    }

    public int getLocalCacheMaxSize() {
        return localCacheMaxSize;
    }

    public void setLocalCacheMaxSize(int localCacheMaxSize) {
        this.localCacheMaxSize = localCacheMaxSize;
    }

    /**
     * 存储类型枚举
     */
    public enum StorageType {
        /**
         * 内存存储（单机模式）
         */
        MEMORY,

        /**
         * Redis 存储（分布式模式）
         */
        REDIS,

        /**
         * Redisson 分布式存储（推荐）
         */
        REDISSON
    }

    /**
     * Redis 配置
     */
    public static class RedisConfig {
        /**
         * Redis 键前缀
         */
        private String keyPrefix = "afg:feature:";

        /**
         * 功能开关映射键
         */
        private String flagsMapKey = "flags";

        /**
         * 灰度规则映射键
         */
        private String rulesMapKey = "rules";

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getFlagsMapKey() {
            return flagsMapKey;
        }

        public void setFlagsMapKey(String flagsMapKey) {
            this.flagsMapKey = flagsMapKey;
        }

        public String getRulesMapKey() {
            return rulesMapKey;
        }

        public void setRulesMapKey(String rulesMapKey) {
            this.rulesMapKey = rulesMapKey;
        }
    }

    /**
     * 功能开关定义（用于配置文件初始化）
     */
    public static class FeatureDefinition {
        /**
         * 功能名称
         */
        private String name;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 灰度策略
         */
        private GrayscaleStrategy strategy;

        /**
         * 百分比
         */
        private int percentage;

        /**
         * 用户ID白名单
         */
        private Set<Long> userIds;

        /**
         * 租户ID白名单
         */
        private Set<Long> tenantIds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public GrayscaleStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(GrayscaleStrategy strategy) {
            this.strategy = strategy;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        public Set<Long> getUserIds() {
            return userIds;
        }

        public void setUserIds(Set<Long> userIds) {
            this.userIds = userIds;
        }

        public Set<Long> getTenantIds() {
            return tenantIds;
        }

        public void setTenantIds(Set<Long> tenantIds) {
            this.tenantIds = tenantIds;
        }
    }
}
