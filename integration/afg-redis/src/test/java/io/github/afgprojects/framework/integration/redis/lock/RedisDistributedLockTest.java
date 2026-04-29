package io.github.afgprojects.framework.integration.redis.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.lock.LockType;
import io.github.afgprojects.framework.core.lock.exception.LockException;

/**
 * RedisDistributedLock 测试
 */
@DisplayName("RedisDistributedLock 测试")
class RedisDistributedLockTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock reentrantLock;

    @Mock
    private RLock fairLock;

    @Mock
    private RReadWriteLock readWriteLock;

    @Mock
    private RLock readLock;

    @Mock
    private RLock writeLock;

    private LockProperties properties;
    private RedisDistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new LockProperties();
        distributedLock = new RedisDistributedLock(redissonClient, properties);

        // Mock Redisson locks
        when(redissonClient.getLock(anyString())).thenReturn(reentrantLock);
        when(redissonClient.getFairLock(anyString())).thenReturn(fairLock);
        when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
        when(readWriteLock.readLock()).thenReturn(readLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
    }

    @Nested
    @DisplayName("tryLock 测试")
    class TryLockTests {

        @Test
        @DisplayName("应该成功获取可重入锁")
        void shouldAcquireReentrantLock() throws InterruptedException {
            // given
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1);

            // then
            assertThat(result).isTrue();
            verify(redissonClient).getLock("afg:lock:" + key);
        }

        @Test
        @DisplayName("应该成功获取公平锁")
        void shouldAcquireFairLock() throws InterruptedException {
            // given
            String key = "fair-lock";
            when(fairLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1, LockType.FAIR);

            // then
            assertThat(result).isTrue();
            verify(redissonClient).getFairLock("afg:lock:" + key);
        }

        @Test
        @DisplayName("应该成功获取读锁")
        void shouldAcquireReadLock() throws InterruptedException {
            // given
            String key = "read-lock";
            when(readLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1, LockType.READ);

            // then
            assertThat(result).isTrue();
            verify(readWriteLock).readLock();
        }

        @Test
        @DisplayName("应该成功获取写锁")
        void shouldAcquireWriteLock() throws InterruptedException {
            // given
            String key = "write-lock";
            when(writeLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1, LockType.WRITE);

            // then
            assertThat(result).isTrue();
            verify(readWriteLock).writeLock();
        }

        @Test
        @DisplayName("获取锁失败应该返回 false")
        void shouldReturnFalseWhenLockNotAcquired() throws InterruptedException {
            // given
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("指定 leaseTime 时应该使用指定的租约时间")
        void shouldUseLeaseTimeWhenSpecified() throws InterruptedException {
            // given
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryLock(key, 5000, 30000);

            // then
            assertThat(result).isTrue();
            verify(reentrantLock).tryLock(eq(5000L), eq(30000L), any(TimeUnit.class));
        }

        @Test
        @DisplayName("线程中断时应该返回 false")
        void shouldReturnFalseWhenInterrupted() throws InterruptedException {
            // given
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException());

            // when
            boolean result = distributedLock.tryLock(key, 5000, -1);

            // then
            assertThat(result).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            // 清除中断标志
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("lock 测试")
    class LockTests {

        @Test
        @DisplayName("应该成功获取可重入锁（阻塞）")
        void shouldAcquireReentrantLockBlocking() {
            // given
            String key = "test-lock";

            // when
            distributedLock.lock(key);

            // then
            verify(redissonClient).getLock("afg:lock:" + key);
            verify(reentrantLock).lock();
        }

        @Test
        @DisplayName("应该成功获取公平锁（阻塞）")
        void shouldAcquireFairLockBlocking() {
            // given
            String key = "fair-lock";

            // when
            distributedLock.lock(key, LockType.FAIR);

            // then
            verify(redissonClient).getFairLock("afg:lock:" + key);
            verify(fairLock).lock();
        }

        @Test
        @DisplayName("应该成功获取读锁（阻塞）")
        void shouldAcquireReadLockBlocking() {
            // given
            String key = "read-lock";

            // when
            distributedLock.lock(key, LockType.READ);

            // then
            verify(readLock).lock();
        }

        @Test
        @DisplayName("应该成功获取写锁（阻塞）")
        void shouldAcquireWriteLockBlocking() {
            // given
            String key = "write-lock";

            // when
            distributedLock.lock(key, LockType.WRITE);

            // then
            verify(writeLock).lock();
        }
    }

    @Nested
    @DisplayName("unlock 测试")
    class UnlockTests {

        @Test
        @DisplayName("应该成功释放可重入锁")
        void shouldReleaseReentrantLock() {
            // given
            String key = "test-lock";
            when(reentrantLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            distributedLock.unlock(key);

            // then
            verify(reentrantLock).unlock();
        }

        @Test
        @DisplayName("当前线程未持有锁时不应该释放")
        void shouldNotReleaseWhenNotHeldByCurrentThread() {
            // given
            String key = "test-lock";
            when(reentrantLock.isHeldByCurrentThread()).thenReturn(false);

            // when
            distributedLock.unlock(key);

            // then
            verify(reentrantLock, org.mockito.Mockito.never()).unlock();
        }

        @Test
        @DisplayName("应该成功释放读锁")
        void shouldReleaseReadLock() {
            // given
            String key = "read-lock";
            when(readLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            distributedLock.unlock(key, LockType.READ);

            // then
            verify(readLock).unlock();
        }

        @Test
        @DisplayName("应该成功释放写锁")
        void shouldReleaseWriteLock() {
            // given
            String key = "write-lock";
            when(writeLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            distributedLock.unlock(key, LockType.WRITE);

            // then
            verify(writeLock).unlock();
        }

        @Test
        @DisplayName("释放锁异常时应该抛出 LockException")
        void shouldThrowLockExceptionOnError() {
            // given
            String key = "test-lock";
            when(reentrantLock.isHeldByCurrentThread()).thenReturn(true);
            org.mockito.Mockito.doThrow(new RuntimeException("Redis error"))
                    .when(reentrantLock).unlock();

            // when & then
            assertThatThrownBy(() -> distributedLock.unlock(key))
                    .isInstanceOf(LockException.class)
                    .hasMessageContaining("Failed to release lock");
        }
    }

    @Nested
    @DisplayName("isLocked 测试")
    class IsLockedTests {

        @Test
        @DisplayName("锁被持有时应该返回 true")
        void shouldReturnTrueWhenLocked() {
            // given
            String key = "test-lock";
            when(reentrantLock.isLocked()).thenReturn(true);

            // when
            boolean result = distributedLock.isLocked(key);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("锁未被持有时应该返回 false")
        void shouldReturnFalseWhenNotLocked() {
            // given
            String key = "test-lock";
            when(reentrantLock.isLocked()).thenReturn(false);

            // when
            boolean result = distributedLock.isLocked(key);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isHeldByCurrentThread 测试")
    class IsHeldByCurrentThreadTests {

        @Test
        @DisplayName("当前线程持有时应该返回 true")
        void shouldReturnTrueWhenHeldByCurrentThread() {
            // given
            String key = "test-lock";
            when(reentrantLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            boolean result = distributedLock.isHeldByCurrentThread(key);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("当前线程未持有时应该返回 false")
        void shouldReturnFalseWhenNotHeldByCurrentThread() {
            // given
            String key = "test-lock";
            when(reentrantLock.isHeldByCurrentThread()).thenReturn(false);

            // when
            boolean result = distributedLock.isHeldByCurrentThread(key);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("读写锁专用方法测试")
    class ReadWriteLockTests {

        @Test
        @DisplayName("tryReadLock 应该成功获取读锁")
        void shouldAcquireReadLockWithDedicatedMethod() throws InterruptedException {
            // given
            String key = "read-lock";
            when(readLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryReadLock(key, 5000, -1);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("tryWriteLock 应该成功获取写锁")
        void shouldAcquireWriteLockWithDedicatedMethod() throws InterruptedException {
            // given
            String key = "write-lock";
            when(writeLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            boolean result = distributedLock.tryWriteLock(key, 5000, -1);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("unlockReadLock 应该释放读锁")
        void shouldReleaseReadLockWithDedicatedMethod() {
            // given
            String key = "read-lock";
            when(readLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            distributedLock.unlockReadLock(key);

            // then
            verify(readLock).unlock();
        }

        @Test
        @DisplayName("unlockWriteLock 应该释放写锁")
        void shouldReleaseWriteLockWithDedicatedMethod() {
            // given
            String key = "write-lock";
            when(writeLock.isHeldByCurrentThread()).thenReturn(true);

            // when
            distributedLock.unlockWriteLock(key);

            // then
            verify(writeLock).unlock();
        }
    }

    @Nested
    @DisplayName("键前缀测试")
    class KeyPrefixTests {

        @Test
        @DisplayName("应该使用配置的键前缀")
        void shouldUseConfiguredKeyPrefix() throws InterruptedException {
            // given
            properties.setKeyPrefix("myapp");
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            distributedLock.tryLock(key, 5000, -1);

            // then
            verify(redissonClient).getLock("myapp:" + key);
        }

        @Test
        @DisplayName("键前缀为空时应该直接使用键")
        void shouldUseKeyDirectlyWhenPrefixIsEmpty() throws InterruptedException {
            // given
            properties.setKeyPrefix("");
            String key = "test-lock";
            when(reentrantLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            // when
            distributedLock.tryLock(key, 5000, -1);

            // then
            verify(redissonClient).getLock(key);
        }
    }
}
