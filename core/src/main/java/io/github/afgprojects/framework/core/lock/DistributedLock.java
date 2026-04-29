package io.github.afgprojects.framework.core.lock;

/**
 * 分布式锁接口
 * <p>
 * 定义统一的分布式锁操作接口，支持多种锁类型
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private DistributedLock distributedLock;
 *
 * // 尝试获取锁
 * boolean acquired = distributedLock.tryLock("my-lock", 5, 30);
 * if (acquired) {
 *     try {
 *         // 执行业务逻辑
 *     } finally {
 *         distributedLock.unlock("my-lock");
 *     }
 * }
 * }</pre>
 */
public interface DistributedLock {

    /**
     * 尝试获取锁
     * <p>
     * 如果锁可用，立即获取并返回 true。
     * 如果锁不可用，等待指定时间，直到获取锁或超时。
     * </p>
     *
     * @param key       锁的键
     * @param waitTime  等待时间（毫秒），0 表示立即返回
     * @param leaseTime 持有时间（毫秒），-1 表示使用 watchdog 自动续期
     * @return 是否成功获取锁
     */
    boolean tryLock(String key, long waitTime, long leaseTime);

    /**
     * 尝试获取指定类型的锁
     *
     * @param key       锁的键
     * @param waitTime  等待时间（毫秒）
     * @param leaseTime 持有时间（毫秒），-1 表示使用 watchdog 自动续期
     * @param lockType  锁类型
     * @return 是否成功获取锁
     */
    boolean tryLock(String key, long waitTime, long leaseTime, LockType lockType);

    /**
     * 获取锁（阻塞直到获取成功）
     * <p>
     * 如果锁不可用，当前线程会阻塞直到获取锁。
     * 使用 watchdog 机制自动续期。
     * </p>
     *
     * @param key 锁的键
     */
    void lock(String key);

    /**
     * 获取指定类型的锁（阻塞直到获取成功）
     *
     * @param key      锁的键
     * @param lockType 锁类型
     */
    void lock(String key, LockType lockType);

    /**
     * 释放锁
     * <p>
     * 释放当前线程持有的锁。如果当前线程未持有该锁，操作无效。
     * </p>
     *
     * @param key 锁的键
     */
    void unlock(String key);

    /**
     * 释放指定类型的锁
     *
     * @param key      锁的键
     * @param lockType 锁类型
     */
    void unlock(String key, LockType lockType);

    /**
     * 检查锁是否被持有
     *
     * @param key 锁的键
     * @return 锁是否被任何线程持有
     */
    boolean isLocked(String key);

    /**
     * 检查当前线程是否持有锁
     *
     * @param key 锁的键
     * @return 当前线程是否持有该锁
     */
    boolean isHeldByCurrentThread(String key);

    /**
     * 获取读锁
     * <p>
     * 读锁是共享锁，多个线程可以同时持有读锁。
     * </p>
     *
     * @param key       锁的键
     * @param waitTime  等待时间（毫秒）
     * @param leaseTime 持有时间（毫秒）
     * @return 是否成功获取读锁
     */
    boolean tryReadLock(String key, long waitTime, long leaseTime);

    /**
     * 获取写锁
     * <p>
     * 写锁是排他锁，同一时间只能有一个线程持有写锁。
     * </p>
     *
     * @param key       锁的键
     * @param waitTime  等待时间（毫秒）
     * @param leaseTime 持有时间（毫秒）
     * @return 是否成功获取写锁
     */
    boolean tryWriteLock(String key, long waitTime, long leaseTime);

    /**
     * 释放读锁
     *
     * @param key 锁的键
     */
    void unlockReadLock(String key);

    /**
     * 释放写锁
     *
     * @param key 锁的键
     */
    void unlockWriteLock(String key);
}
