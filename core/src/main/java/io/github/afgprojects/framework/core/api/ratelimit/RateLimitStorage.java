package io.github.afgprojects.framework.core.api.ratelimit;

/**
 * 限流存储接口
 * <p>
 * 存储后端实现此接口，提供限流能力。
 * 支持原生算法实现（高性能）和原子操作原语（通用算法）。
 */
public interface RateLimitStorage {

    /**
     * 获取存储类型
     *
     * @return 存储类型标识（如 redis, local, hazelcast）
     */
    String getStorageType();

    // ==================== 原生算法实现（高性能）====================

    /**
     * 令牌桶算法 - 尝试获取令牌
     *
     * @param key   限流 key
     * @param rate  每秒令牌数
     * @param burst 桶容量
     * @return 限流结果
     */
    RateLimitResult tryAcquireTokenBucket(String key, long rate, long burst);

    /**
     * 滑动窗口算法 - 尝试获取许可
     *
     * @param key        限流 key
     * @param rate       窗口内最大请求数
     * @param windowSize 窗口大小（秒）
     * @return 限流结果
     */
    RateLimitResult tryAcquireSlidingWindow(String key, long rate, long windowSize);

    // ==================== 原子操作原语（通用算法用）====================

    /**
     * 原子递增
     *
     * @param key   键
     * @param delta 增量
     * @param ttl   过期时间（秒）
     * @return 递增后的值
     */
    long increment(String key, long delta, long ttl);

    /**
     * 原子比较并设置
     *
     * @param key    键
     * @param expect 期望值
     * @param update 新值
     * @return 是否成功
     */
    boolean compareAndSet(String key, long expect, long update);

    /**
     * 获取值
     *
     * @param key 键
     * @return 当前值，不存在返回 0
     */
    long get(String key);

    /**
     * 设置过期时间
     *
     * @param key 键
     * @param ttl 过期时间（秒）
     */
    void expire(String key, long ttl);

    /**
     * 删除键
     *
     * @param key 键
     */
    void delete(String key);

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);
}
