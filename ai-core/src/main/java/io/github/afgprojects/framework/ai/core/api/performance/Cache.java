package io.github.afgprojects.framework.ai.core.api.performance;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 缓存接口
 *
 * <p>用于缓存 AI 响应和 Embedding：
 * <ul>
 *   <li>响应缓存（相同请求返回缓存结果）</li>
 *   <li>Embedding 缓存（相同文本返回缓存向量）</li>
 *   <li>TTL 过期</li>
 *   <li>缓存失效策略</li>
 * </ul>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface Cache<K, V> {

    /**
     * 获取缓存值
     *
     * @param key 键
     * @return 缓存值，如果不存在返回 empty
     */
    @NonNull
    Optional<V> get(@NonNull K key);

    /**
     * 获取缓存值（带加载器）
     *
     * @param key    键
     * @param loader 加载器（缓存未命中时调用）
     * @return 缓存值
     */
    @NonNull
    V get(@NonNull K key, @NonNull CacheLoader<K, V> loader);

    /**
     * 获取缓存值（带加载器和 TTL）
     *
     * @param key    键
     * @param loader 加载器
     * @param ttl    过期时间
     * @return 缓存值
     */
    @NonNull
    V get(@NonNull K key, @NonNull CacheLoader<K, V> loader, @NonNull Duration ttl);

    /**
     * 存储缓存值
     *
     * @param key   键
     * @param value 值
     */
    void put(@NonNull K key, @NonNull V value);

    /**
     * 存储缓存值（带 TTL）
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     */
    void put(@NonNull K key, @NonNull V value, @NonNull Duration ttl);

    /**
     * 如果不存在则存储
     *
     * @param key   键
     * @param value 值
     * @return 已存在的值，如果不存在返回 null
     */
    @Nullable
    V putIfAbsent(@NonNull K key, @NonNull V value);

    /**
     * 删除缓存值
     *
     * @param key 键
     * @return 被删除的值，如果不存在返回 empty
     */
    @NonNull
    Optional<V> remove(@NonNull K key);

    /**
     * 检查是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    boolean containsKey(@NonNull K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    long size();

    /**
     * 获取缓存统计
     *
     * @return 缓存统计
     */
    @NonNull
    CacheStats getStats();

    /**
     * 使缓存失效（根据条件）
     *
     * @param predicate 失效条件
     * @return 失效的条目数
     */
    long invalidate(@NonNull CachePredicate<K, V> predicate);

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    interface CacheLoader<K, V> {
        /**
         * 加载值
         *
         * @param key 键
         * @return 值
         * @throws Exception 加载异常
         */
        V load(K key) throws Exception;
    }

    /**
     * 缓存谓词接口
     */
    @FunctionalInterface
    interface CachePredicate<K, V> {
        /**
         * 测试是否满足条件
         *
         * @param key   键
         * @param value 值
         * @return 是否满足
         */
        boolean test(K key, V value);
    }

    /**
     * 缓存统计接口
     */
    interface CacheStats {

        /**
         * 获取命中次数
         *
         * @return 命中次数
         */
        long getHitCount();

        /**
         * 获取未命中次数
         *
         * @return 未命中次数
         */
        long getMissCount();

        /**
         * 获取命中率
         *
         * @return 命中率（0-1）
         */
        double getHitRate();

        /**
         * 获取加载成功次数
         *
         * @return 加载成功次数
         */
        long getLoadSuccessCount();

        /**
         * 获取加载失败次数
         *
         * @return 加载失败次数
         */
        long getLoadFailureCount();

        /**
         * 获取平均加载时间
         *
         * @return 平均加载时间（纳秒）
         */
        double getAverageLoadTime();

        /**
         * 获取驱逐次数
         *
         * @return 驱逐次数
         */
        long getEvictionCount();

        /**
         * 获取当前大小
         *
         * @return 当前大小
         */
        long getSize();

        /**
         * 获取最大大小
         *
         * @return 最大大小，如果无限制返回 -1
         */
        long getMaxSize();
    }

    /**
     * 缓存条目接口
     */
    interface CacheEntry<V> {

        /**
         * 获取值
         *
         * @return 值
         */
        @NonNull
        V getValue();

        /**
         * 获取创建时间
         *
         * @return 创建时间（毫秒时间戳）
         */
        long getCreatedAt();

        /**
         * 获取过期时间
         *
         * @return 过期时间（毫秒时间戳），如果永不过期返回 null
         */
        @Nullable
        Long getExpiresAt();

        /**
         * 是否已过期
         *
         * @return 是否已过期
         */
        boolean isExpired();

        /**
         * 获取访问次数
         *
         * @return 访问次数
         */
        long getAccessCount();

        /**
         * 获取最后访问时间
         *
         * @return 最后访问时间（毫秒时间戳）
         */
        long getLastAccessTime();

        /**
         * 获取元数据
         *
         * @return 元数据
         */
        @NonNull
        Map<String, String> getMetadata();
    }
}