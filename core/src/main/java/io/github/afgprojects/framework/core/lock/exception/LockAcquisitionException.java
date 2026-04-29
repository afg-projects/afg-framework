package io.github.afgprojects.framework.core.lock.exception;

/**
 * 锁获取失败异常
 * <p>
 * 当无法在指定时间内获取锁时抛出
 * </p>
 */
public class LockAcquisitionException extends LockException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造锁获取失败异常
     *
     * @param lockKey 锁的键
     */
    public LockAcquisitionException(String lockKey) {
        super(lockKey, "Failed to acquire lock: " + lockKey);
    }

    /**
     * 构造锁获取失败异常
     *
     * @param lockKey 锁的键
     * @param message 异常消息
     */
    public LockAcquisitionException(String lockKey, String message) {
        super(lockKey, message);
    }

    /**
     * 构造锁获取失败异常
     *
     * @param lockKey 锁的键
     * @param message 异常消息
     * @param cause   原因异常
     */
    public LockAcquisitionException(String lockKey, String message, Throwable cause) {
        super(lockKey, message, cause);
    }
}
