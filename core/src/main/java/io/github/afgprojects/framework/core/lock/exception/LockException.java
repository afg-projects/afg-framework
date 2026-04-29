package io.github.afgprojects.framework.core.lock.exception;

/**
 * 锁异常
 * <p>
 * 分布式锁操作过程中发生的异常
 * </p>
 */
public class LockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 锁的键
     */
    private final String lockKey;

    /**
     * 构造锁异常
     *
     * @param lockKey 锁的键
     * @param message 异常消息
     */
    public LockException(String lockKey, String message) {
        super(message);
        this.lockKey = lockKey;
    }

    /**
     * 构造锁异常
     *
     * @param lockKey 锁的键
     * @param message 异常消息
     * @param cause   原因异常
     */
    public LockException(String lockKey, String message, Throwable cause) {
        super(message, cause);
        this.lockKey = lockKey;
    }

    /**
     * 获取锁的键
     *
     * @return 锁的键
     */
    public String getLockKey() {
        return lockKey;
    }
}
