package io.github.afgprojects.framework.core.cache;

import java.time.Duration;

import org.jspecify.annotations.NonNull;

/**
 * 缓存配置
 * <p>
 * 用于创建缓存实例的配置参数
 * </p>
 */
public class CacheConfig {

    /**
     * 默认过期时间（毫秒）
     */
    private long defaultTtl = 0;

    /**
     * 最大容量
     */
    private int maximumSize = 10000;

    /**
     * 是否缓存 null 值（防穿透）
     */
    private boolean cacheNull = true;

    /**
     * 空值缓存过期时间（毫秒），默认 60 秒
     */
    private long nullValueTtl = 60000;

    /**
     * 初始容量
     */
    private int initialCapacity = 128;

    /**
     * 写入后过期时间
     */
    private Duration expireAfterWrite;

    /**
     * 访问后过期时间
     */
    private Duration expireAfterAccess;

    /**
     * 统计信息开关
     */
    private boolean recordStats = true;

    /**
     * 创建默认配置
     *
     * @return 默认配置
     */
    @NonNull
    public static CacheConfig defaultConfig() {
        return new CacheConfig();
    }

    /**
     * 获取默认过期时间
     *
     * @return 默认过期时间（毫秒）
     */
    public long getDefaultTtl() {
        return defaultTtl;
    }

    /**
     * 设置默认过期时间
     *
     * @param defaultTtl 默认过期时间（毫秒）
     * @return this
     */
    @NonNull
    public CacheConfig defaultTtl(long defaultTtl) {
        this.defaultTtl = defaultTtl;
        return this;
    }

    /**
     * 获取最大容量
     *
     * @return 最大容量
     */
    public int getMaximumSize() {
        return maximumSize;
    }

    /**
     * 设置最大容量
     *
     * @param maximumSize 最大容量
     * @return this
     */
    @NonNull
    public CacheConfig maximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }

    /**
     * 是否缓存 null 值
     *
     * @return 是否缓存 null 值
     */
    public boolean isCacheNull() {
        return cacheNull;
    }

    /**
     * 设置是否缓存 null 值
     *
     * @param cacheNull 是否缓存 null 值
     * @return this
     */
    @NonNull
    public CacheConfig cacheNull(boolean cacheNull) {
        this.cacheNull = cacheNull;
        return this;
    }

    /**
     * 获取空值缓存过期时间
     *
     * @return 空值缓存过期时间（毫秒）
     */
    public long getNullValueTtl() {
        return nullValueTtl;
    }

    /**
     * 设置空值缓存过期时间
     *
     * @param nullValueTtl 空值缓存过期时间（毫秒）
     * @return this
     */
    @NonNull
    public CacheConfig nullValueTtl(long nullValueTtl) {
        this.nullValueTtl = nullValueTtl;
        return this;
    }

    /**
     * 获取初始容量
     *
     * @return 初始容量
     */
    public int getInitialCapacity() {
        return initialCapacity;
    }

    /**
     * 设置初始容量
     *
     * @param initialCapacity 初始容量
     * @return this
     */
    @NonNull
    public CacheConfig initialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
        return this;
    }

    /**
     * 获取写入后过期时间
     *
     * @return 写入后过期时间
     */
    public Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    /**
     * 设置写入后过期时间
     *
     * @param expireAfterWrite 写入后过期时间
     * @return this
     */
    @NonNull
    public CacheConfig expireAfterWrite(Duration expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
        return this;
    }

    /**
     * 获取访问后过期时间
     *
     * @return 访问后过期时间
     */
    public Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    /**
     * 设置访问后过期时间
     *
     * @param expireAfterAccess 访问后过期时间
     * @return this
     */
    @NonNull
    public CacheConfig expireAfterAccess(Duration expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
        return this;
    }

    /**
     * 是否开启统计
     *
     * @return 是否开启统计
     */
    public boolean isRecordStats() {
        return recordStats;
    }

    /**
     * 设置是否开启统计
     *
     * @param recordStats 是否开启统计
     * @return this
     */
    @NonNull
    public CacheConfig recordStats(boolean recordStats) {
        this.recordStats = recordStats;
        return this;
    }
}
