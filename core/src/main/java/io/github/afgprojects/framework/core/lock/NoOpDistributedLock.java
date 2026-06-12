package io.github.afgprojects.framework.core.lock;

/**
 * NoOp 分布式锁实现
 * <p>
 * 本地降级实现，所有锁操作总是成功获取并立即释放。
 * 适用于未配置 Redis 等分布式锁后端的场景，注解式加锁会"透传"（不阻塞业务逻辑）。
 * <p>
 * 由 {@code LockAutoConfiguration} 在无其他 {@link DistributedLock} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpDistributedLock implements DistributedLock {

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime) {
        return true;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, LockType lockType) {
        return true;
    }

    @Override
    public void lock(String key) {
        // no-op: 总是立即获取成功
    }

    @Override
    public void lock(String key, LockType lockType) {
        // no-op
    }

    @Override
    public void unlock(String key) {
        // no-op
    }

    @Override
    public void unlock(String key, LockType lockType) {
        // no-op
    }

    @Override
    public boolean isLocked(String key) {
        return false;
    }

    @Override
    public boolean isHeldByCurrentThread(String key) {
        return false;
    }

    @Override
    public boolean tryReadLock(String key, long waitTime, long leaseTime) {
        return true;
    }

    @Override
    public boolean tryWriteLock(String key, long waitTime, long leaseTime) {
        return true;
    }

    @Override
    public void unlockReadLock(String key) {
        // no-op
    }

    @Override
    public void unlockWriteLock(String key) {
        // no-op
    }
}
