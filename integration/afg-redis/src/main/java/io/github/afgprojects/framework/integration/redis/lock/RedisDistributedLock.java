package io.github.afgprojects.framework.integration.redis.lock;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockType;
import io.github.afgprojects.framework.core.lock.exception.LockException;

/**
 * Redis 分布式锁实现
 * <p>
 * 基于 Redisson 实现分布式锁，支持：
 * <ul>
 *   <li>可重入锁（Reentrant Lock）</li>
 *   <li>公平锁（Fair Lock）</li>
 *   <li>读写锁（Read-Write Lock）</li>
 *   <li>Watchdog 自动续期</li>
 * </ul>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private DistributedLock distributedLock;
 *
 * // 尝试获取锁
 * boolean acquired = distributedLock.tryLock("my-lock", 5000, 30000);
 * if (acquired) {
 *     try {
 *         // 执行业务逻辑
 *     } finally {
 *         distributedLock.unlock("my-lock");
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedisDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    /**
     * Redisson 客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 锁配置属性
     */
    private final LockProperties properties;

    /**
     * 构造 Redis 分布式锁
     *
     * @param redissonClient Redisson 客户端
     * @param properties     锁配置属性
     */
    public RedisDistributedLock(@NonNull RedissonClient redissonClient, @NonNull LockProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 简化构造方法，使用默认配置
     *
     * @param redissonClient Redisson 客户端
     * @param keyPrefix      锁键前缀
     */
    public RedisDistributedLock(@NonNull RedissonClient redissonClient, String keyPrefix) {
        this.redissonClient = redissonClient;
        this.properties = new LockProperties();
        if (keyPrefix != null && !keyPrefix.isEmpty()) {
            this.properties.setKeyPrefix(keyPrefix);
        }
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime) {
        return tryLock(key, waitTime, leaseTime, LockType.REENTRANT);
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, LockType lockType) {
        String lockKey = buildLockKey(key);

        try {
            return switch (lockType) {
                case REENTRANT -> tryReentrantLock(lockKey, waitTime, leaseTime);
                case FAIR -> tryFairLock(lockKey, waitTime, leaseTime);
                case READ -> tryReadLock(lockKey, waitTime, leaseTime);
                case WRITE -> tryWriteLock(lockKey, waitTime, leaseTime);
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while acquiring lock: {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Failed to acquire lock: {}", lockKey);
            return false;
        }
    }

    @Override
    public void lock(String key) {
        lock(key, LockType.REENTRANT);
    }

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    public void lock(String key, LockType lockType) {
        String lockKey = buildLockKey(key);

        try {
            switch (lockType) {
                case REENTRANT -> {
                    RLock lock = redissonClient.getLock(lockKey);
                    lock.lock();
                    log.debug("Acquired reentrant lock: {}", lockKey);
                }
                case FAIR -> {
                    RLock lock = redissonClient.getFairLock(lockKey);
                    lock.lock();
                    log.debug("Acquired fair lock: {}", lockKey);
                }
                case READ -> {
                    RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
                    rwLock.readLock().lock();
                    log.debug("Acquired read lock: {}", lockKey);
                }
                case WRITE -> {
                    RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
                    rwLock.writeLock().lock();
                    log.debug("Acquired write lock: {}", lockKey);
                }
            }
        } catch (Exception e) {
            throw new LockException(lockKey, "Failed to acquire lock", e);
        }
    }

    @Override
    public void unlock(String key) {
        unlock(key, LockType.REENTRANT);
    }

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    public void unlock(String key, LockType lockType) {
        String lockKey = buildLockKey(key);

        try {
            switch (lockType) {
                case REENTRANT, FAIR -> {
                    RLock lock = redissonClient.getLock(lockKey);
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released lock: {}", lockKey);
                    }
                }
                case READ -> unlockReadLockInternal(lockKey);
                case WRITE -> unlockWriteLockInternal(lockKey);
            }
        } catch (Exception e) {
            log.error("Failed to release lock: {}", lockKey);
            throw new LockException(lockKey, "Failed to release lock", e);
        }
    }

    @Override
    public boolean isLocked(String key) {
        String lockKey = buildLockKey(key);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    @Override
    public boolean isHeldByCurrentThread(String key) {
        String lockKey = buildLockKey(key);
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    @Override
    public boolean tryReadLock(String key, long waitTime, long leaseTime) {
        String lockKey = buildLockKey(key);

        try {
            RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
            RLock readLock = rwLock.readLock();

            boolean acquired;
            if (leaseTime > 0) {
                acquired = readLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            } else {
                // 使用 watchdog
                acquired = readLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
            }

            if (acquired) {
                log.debug("Acquired read lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while acquiring read lock: {}", lockKey);
            return false;
        }
    }

    @Override
    public boolean tryWriteLock(String key, long waitTime, long leaseTime) {
        String lockKey = buildLockKey(key);

        try {
            RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
            RLock writeLock = rwLock.writeLock();

            boolean acquired;
            if (leaseTime > 0) {
                acquired = writeLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            } else {
                // 使用 watchdog
                acquired = writeLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
            }

            if (acquired) {
                log.debug("Acquired write lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted while acquiring write lock: {}", lockKey);
            return false;
        }
    }

    @Override
    public void unlockReadLock(String key) {
        String lockKey = buildLockKey(key);
        unlockReadLockInternal(lockKey);
    }

    @Override
    public void unlockWriteLock(String key) {
        String lockKey = buildLockKey(key);
        unlockWriteLockInternal(lockKey);
    }

    /**
     * 尝试获取可重入锁
     */
    private boolean tryReentrantLock(String lockKey, long waitTime, long leaseTime) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired;
        if (leaseTime > 0) {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        } else {
            // 使用 watchdog 自动续期
            acquired = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
        }

        if (acquired) {
            log.debug("Acquired reentrant lock: {}", lockKey);
        }
        return acquired;
    }

    /**
     * 尝试获取公平锁
     */
    private boolean tryFairLock(String lockKey, long waitTime, long leaseTime) throws InterruptedException {
        RLock lock = redissonClient.getFairLock(lockKey);

        boolean acquired;
        if (leaseTime > 0) {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        } else {
            // 使用 watchdog 自动续期
            acquired = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
        }

        if (acquired) {
            log.debug("Acquired fair lock: {}", lockKey);
        }
        return acquired;
    }

    /**
     * 内部释放读锁
     */
    private void unlockReadLockInternal(String lockKey) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        if (readLock.isHeldByCurrentThread()) {
            readLock.unlock();
            log.debug("Released read lock: {}", lockKey);
        }
    }

    /**
     * 内部释放写锁
     */
    private void unlockWriteLockInternal(String lockKey) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        if (writeLock.isHeldByCurrentThread()) {
            writeLock.unlock();
            log.debug("Released write lock: {}", lockKey);
        }
    }

    /**
     * 构建完整的锁键
     *
     * @param key 原始键
     * @return 完整的锁键
     */
    @NonNull
    private String buildLockKey(@Nullable String key) {
        String prefix = properties.getKeyPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + ":" + key;
        }
        return key != null ? key : "";
    }
}
