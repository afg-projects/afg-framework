package io.github.afgprojects.framework.integration.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockType;
import io.github.afgprojects.framework.integration.redis.lock.LockProperties;
import io.github.afgprojects.framework.integration.redis.lock.RedisDistributedLock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisDistributedLock 集成测试
 *
 * <p>基于真实 Redis 容器测试分布式锁的核心操作
 */
@DisplayName("RedisDistributedLock 分布式锁测试")
class RedisDistributedLockTest extends BaseRedisTest {

    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        LockProperties properties = new LockProperties();
        properties.setKeyPrefix("test:lock");
        distributedLock = new RedisDistributedLock(getRedissonClient(), properties);
    }

    @Nested
    @DisplayName("tryLock 操作")
    class TryLock {

        @Test
        @DisplayName("tryLock 应成功获取未被持有的锁")
        void shouldAcquireLock_whenLockIsFree() {
            boolean acquired = distributedLock.tryLock("test-key-1", 1000, 5000);

            assertThat(acquired).isTrue();

            // 清理
            distributedLock.unlock("test-key-1");
        }

        @Test
        @DisplayName("tryLock 对同一把可重入锁在同一线程内可再次获取")
        void shouldReentrant_whenSameThreadAcquiresAgain() {
            boolean firstAcquired = distributedLock.tryLock("test-key-reentrant", 0, 10000);
            assertThat(firstAcquired).isTrue();

            // 同一线程可重入获取
            boolean secondAcquired = distributedLock.tryLock("test-key-reentrant", 0, 10000);
            assertThat(secondAcquired).isTrue();

            // 需要两次 unlock
            distributedLock.unlock("test-key-reentrant");
            distributedLock.unlock("test-key-reentrant");
        }

        @Test
        @DisplayName("tryLock 不同 key 应独立获取")
        void shouldAcquireDifferentKeysIndependently() {
            boolean firstAcquired = distributedLock.tryLock("test-key-a", 0, 5000);
            boolean secondAcquired = distributedLock.tryLock("test-key-b", 0, 5000);

            assertThat(firstAcquired).isTrue();
            assertThat(secondAcquired).isTrue();

            distributedLock.unlock("test-key-a");
            distributedLock.unlock("test-key-b");
        }

        @Test
        @DisplayName("tryLock 释放后应可以再次获取")
        void shouldAcquireAgain_afterUnlock() {
            distributedLock.tryLock("test-key-3", 0, 5000);
            distributedLock.unlock("test-key-3");

            boolean reacquired = distributedLock.tryLock("test-key-3", 0, 5000);
            assertThat(reacquired).isTrue();

            distributedLock.unlock("test-key-3");
        }

        @Test
        @DisplayName("tryLock 支持 REENTRANT 类型")
        void shouldAcquireReentrantLock() {
            boolean acquired = distributedLock.tryLock("test-reentrant", 1000, 5000, LockType.REENTRANT);
            assertThat(acquired).isTrue();

            distributedLock.unlock("test-reentrant", LockType.REENTRANT);
        }

        @Test
        @DisplayName("tryLock 支持 FAIR 类型")
        void shouldAcquireFairLock() {
            boolean acquired = distributedLock.tryLock("test-fair", 1000, 5000, LockType.FAIR);
            assertThat(acquired).isTrue();

            distributedLock.unlock("test-fair", LockType.FAIR);
        }
    }

    @Nested
    @DisplayName("读写锁操作")
    class ReadWriteLock {

        @Test
        @DisplayName("tryReadLock 应成功获取读锁")
        void shouldAcquireReadLock() {
            boolean acquired = distributedLock.tryReadLock("test-rw-key", 1000, 5000);
            assertThat(acquired).isTrue();

            distributedLock.unlockReadLock("test-rw-key");
        }

        @Test
        @DisplayName("tryWriteLock 应成功获取写锁")
        void shouldAcquireWriteLock() {
            boolean acquired = distributedLock.tryWriteLock("test-rw-key-2", 1000, 5000);
            assertThat(acquired).isTrue();

            distributedLock.unlockWriteLock("test-rw-key-2");
        }

        @Test
        @DisplayName("多个读锁可以同时持有")
        void shouldAllowMultipleReadLocks() {
            boolean read1 = distributedLock.tryReadLock("test-rw-concurrent", 1000, 10000);
            boolean read2 = distributedLock.tryReadLock("test-rw-concurrent", 1000, 10000);

            assertThat(read1).isTrue();
            assertThat(read2).isTrue();

            distributedLock.unlockReadLock("test-rw-concurrent");
            distributedLock.unlockReadLock("test-rw-concurrent");
        }

        @Test
        @DisplayName("写锁与读锁互斥")
        void shouldBlockWriteLock_whenReadLockIsHeld() {
            boolean readAcquired = distributedLock.tryReadLock("test-rw-mutex", 1000, 10000);
            assertThat(readAcquired).isTrue();

            // 写锁应该无法获取（waitTime=0）
            boolean writeAcquired = distributedLock.tryWriteLock("test-rw-mutex", 0, 10000);
            assertThat(writeAcquired).isFalse();

            distributedLock.unlockReadLock("test-rw-mutex");
        }
    }

    @Nested
    @DisplayName("isLocked / isHeldByCurrentThread 操作")
    class LockStateQuery {

        @Test
        @DisplayName("isLocked 应正确反映锁状态")
        void shouldReflectLockState() {
            assertThat(distributedLock.isLocked("test-state-key")).isFalse();

            distributedLock.tryLock("test-state-key", 0, 30000);
            assertThat(distributedLock.isLocked("test-state-key")).isTrue();

            distributedLock.unlock("test-state-key");
            assertThat(distributedLock.isLocked("test-state-key")).isFalse();
        }

        @Test
        @DisplayName("isHeldByCurrentThread 应正确判断当前线程持有")
        void shouldDetectCurrentThreadHolding() {
            assertThat(distributedLock.isHeldByCurrentThread("test-holder-key")).isFalse();

            distributedLock.tryLock("test-holder-key", 0, 30000);
            assertThat(distributedLock.isHeldByCurrentThread("test-holder-key")).isTrue();

            distributedLock.unlock("test-holder-key");
            assertThat(distributedLock.isHeldByCurrentThread("test-holder-key")).isFalse();
        }
    }

    @Nested
    @DisplayName("lock 操作（阻塞获取）")
    class BlockingLock {

        @Test
        @DisplayName("lock 应阻塞直到获取锁")
        void shouldBlockUntilAcquired() {
            distributedLock.lock("test-blocking-key");

            assertThat(distributedLock.isLocked("test-blocking-key")).isTrue();
            assertThat(distributedLock.isHeldByCurrentThread("test-blocking-key")).isTrue();

            distributedLock.unlock("test-blocking-key");
        }
    }

    @Nested
    @DisplayName("unlock 操作")
    class Unlock {

        @Test
        @DisplayName("unlock 未持有的锁不应抛异常")
        void shouldNotThrow_whenUnlockingUnheldLock() {
            // 解锁一个从未获取的锁应该静默处理
            distributedLock.unlock("test-unheld-key");
        }
    }

    @AfterEach
    void cleanup() {
        // 清理所有测试 key
        getRedissonClient().getKeys().getKeysByPattern("test:lock:*").forEach(key -> {
            getRedissonClient().getBucket(key).delete();
        });
        getRedissonClient().getKeys().getKeysByPattern("test:*").forEach(key -> {
            try {
                getRedissonClient().getBucket(key).delete();
            } catch (Exception ignored) {
                // some keys might not be bucket-compatible
            }
        });
    }
}
