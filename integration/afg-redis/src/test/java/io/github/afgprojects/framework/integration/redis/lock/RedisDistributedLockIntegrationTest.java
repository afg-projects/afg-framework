package io.github.afgprojects.framework.integration.redis.lock;

import io.github.afgprojects.framework.core.lock.LockType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisDistributedLock 集成测试
 * <p>
 * 使用真实的 Redis 容器测试分布式锁功能。
 * </p>
 */
@Testcontainers
@DisplayName("RedisDistributedLock 集成测试")
class RedisDistributedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedissonClient redissonClient;
    private RedisDistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        redissonClient = Redisson.create(config);

        LockProperties properties = new LockProperties();
        properties.setKeyPrefix("test-lock");
        distributedLock = new RedisDistributedLock(redissonClient, properties);
    }

    @AfterEach
    void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Nested
    @DisplayName("基本锁操作测试")
    class BasicLockTests {

        @Test
        @DisplayName("应该成功获取和释放锁")
        void shouldAcquireAndReleaseLock() throws InterruptedException {
            String lockKey = "basic-lock";

            boolean acquired = distributedLock.tryLock(lockKey, 5000, 30000);

            assertThat(acquired).isTrue();
            assertThat(distributedLock.isLocked(lockKey)).isTrue();
            assertThat(distributedLock.isHeldByCurrentThread(lockKey)).isTrue();

            distributedLock.unlock(lockKey);

            assertThat(distributedLock.isLocked(lockKey)).isFalse();
        }

        @Test
        @DisplayName("锁未被持有时应该返回 false")
        void shouldReturnFalseWhenNotLocked() {
            String lockKey = "non-held-lock";

            assertThat(distributedLock.isLocked(lockKey)).isFalse();
            assertThat(distributedLock.isHeldByCurrentThread(lockKey)).isFalse();
        }

        @Test
        @DisplayName("应该成功获取阻塞锁")
        void shouldAcquireBlockingLock() {
            String lockKey = "blocking-lock";

            distributedLock.lock(lockKey);

            assertThat(distributedLock.isLocked(lockKey)).isTrue();
            assertThat(distributedLock.isHeldByCurrentThread(lockKey)).isTrue();

            distributedLock.unlock(lockKey);
        }
    }

    @Nested
    @DisplayName("可重入锁测试")
    class ReentrantLockTests {

        @Test
        @DisplayName("应该支持可重入锁")
        void shouldSupportReentrantLock() throws InterruptedException {
            String lockKey = "reentrant-lock";

            // 第一次获取锁
            boolean acquired1 = distributedLock.tryLock(lockKey, 5000, 30000);
            assertThat(acquired1).isTrue();

            // 同一线程再次获取锁（可重入）
            boolean acquired2 = distributedLock.tryLock(lockKey, 5000, 30000);
            assertThat(acquired2).isTrue();

            // 释放两次
            distributedLock.unlock(lockKey);
            assertThat(distributedLock.isLocked(lockKey)).isTrue();

            distributedLock.unlock(lockKey);
            assertThat(distributedLock.isLocked(lockKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("并发竞争测试")
    class ConcurrentLockTests {

        @Test
        @DisplayName("多个线程竞争同一把锁应该互斥")
        void shouldMutuallyExcludeConcurrentThreads() throws InterruptedException {
            String lockKey = "concurrent-lock";
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger concurrentAccess = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (distributedLock.tryLock(lockKey, 1000, 5000)) {
                            try {
                                int current = concurrentAccess.incrementAndGet();
                                maxConcurrent.updateAndGet(max -> Math.max(max, current));
                                successCount.incrementAndGet();
                                Thread.sleep(100); // 模拟业务处理
                            } finally {
                                concurrentAccess.decrementAndGet();
                                distributedLock.unlock(lockKey);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // 所有线程执行完成
            assertThat(successCount.get()).isGreaterThan(0);
            // 最大并发数应该为 1（互斥）
            assertThat(maxConcurrent.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("获取不到锁应该超时返回 false")
        void shouldReturnFalseOnTimeout() throws InterruptedException {
            String lockKey = "timeout-lock";

            // 线程1 持有锁
            boolean acquired = distributedLock.tryLock(lockKey, 5000, 30000);
            assertThat(acquired).isTrue();

            // 线程2 尝试获取锁，应该超时失败
            ExecutorService executor = Executors.newSingleThreadExecutor();
            AtomicInteger result = new AtomicInteger(-1);

            executor.submit(() -> {
                boolean acquired2 = distributedLock.tryLock(lockKey, 100, 30000);
                result.set(acquired2 ? 1 : 0);
            });

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(result.get()).isEqualTo(0);

            distributedLock.unlock(lockKey);
        }
    }

    @Nested
    @DisplayName("公平锁测试")
    class FairLockTests {

        @Test
        @DisplayName("应该成功获取公平锁")
        void shouldAcquireFairLock() throws InterruptedException {
            String lockKey = "fair-lock";

            boolean acquired = distributedLock.tryLock(lockKey, 5000, 30000,
                    LockType.FAIR);

            assertThat(acquired).isTrue();
            assertThat(distributedLock.isLocked(lockKey)).isTrue();

            distributedLock.unlock(lockKey);
        }
    }

    @Nested
    @DisplayName("读写锁测试")
    class ReadWriteLockTests {

        @Test
        @DisplayName("应该成功获取读锁")
        void shouldAcquireReadLock() throws InterruptedException {
            String lockKey = "rw-lock";

            boolean acquired = distributedLock.tryReadLock(lockKey, 5000, 30000);

            assertThat(acquired).isTrue();

            distributedLock.unlockReadLock(lockKey);
        }

        @Test
        @DisplayName("应该成功获取写锁")
        void shouldAcquireWriteLock() throws InterruptedException {
            String lockKey = "rw-lock-write";

            boolean acquired = distributedLock.tryWriteLock(lockKey, 5000, 30000);

            assertThat(acquired).isTrue();

            distributedLock.unlockWriteLock(lockKey);
        }

        @Test
        @DisplayName("多个线程应该可以同时获取读锁")
        void multipleThreadsCanHoldReadLockSimultaneously() throws InterruptedException {
            String lockKey = "shared-read-lock";
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (distributedLock.tryReadLock(lockKey, 1000, 5000)) {
                            try {
                                successCount.incrementAndGet();
                                Thread.sleep(200);
                            } finally {
                                distributedLock.unlockReadLock(lockKey);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // 所有线程都应该成功获取读锁
            assertThat(successCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("写锁应该与读锁互斥")
        void writeLockShouldBeExclusiveWithReadLock() throws InterruptedException {
            String lockKey = "exclusive-rw-lock";

            // 先获取读锁
            boolean readAcquired = distributedLock.tryReadLock(lockKey, 1000, 5000);
            assertThat(readAcquired).isTrue();

            // 尝试获取写锁应该失败（因为读锁被持有）
            boolean writeAcquired = distributedLock.tryWriteLock(lockKey, 100, 5000);
            assertThat(writeAcquired).isFalse();

            distributedLock.unlockReadLock(lockKey);

            // 现在应该可以获取写锁
            writeAcquired = distributedLock.tryWriteLock(lockKey, 1000, 5000);
            assertThat(writeAcquired).isTrue();

            distributedLock.unlockWriteLock(lockKey);
        }
    }
}
