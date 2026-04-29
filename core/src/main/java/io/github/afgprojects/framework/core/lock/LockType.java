package io.github.afgprojects.framework.core.lock;

/**
 * 锁类型枚举
 * <p>
 * 定义支持的分布式锁类型
 * </p>
 */
public enum LockType {

    /**
     * 可重入锁（默认）
     * <p>
     * 最常用的锁类型，支持同一线程多次获取锁
     * </p>
     */
    REENTRANT,

    /**
     * 公平锁
     * <p>
     * 按照请求顺序获取锁，避免线程饥饿
     * </p>
     */
    FAIR,

    /**
     * 读锁（共享锁）
     * <p>
     * 用于读写锁场景，多个线程可同时持有读锁
     * </p>
     */
    READ,

    /**
     * 写锁（排他锁）
     * <p>
     * 用于读写锁场景，同一时间只有一个线程可持有写锁
     * </p>
     */
    WRITE
}
