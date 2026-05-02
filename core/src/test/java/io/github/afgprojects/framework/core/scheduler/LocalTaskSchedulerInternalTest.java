package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;

/**
 * LocalTaskScheduler 内部类测试
 */
@DisplayName("LocalTaskScheduler 内部类测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalTaskSchedulerInternalTest {

    @Mock
    private SchedulerProperties properties;

    @Mock
    private TaskExecutionMetrics metrics;

    @Mock
    private TaskExecutionLogStorage logStorage;

    @Mock
    private SchedulerProperties.LogStorageConfig logStorageConfig;

    private LocalTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getThreadPoolSize()).thenReturn(2);
        lenient().when(properties.getRetryMultiplier()).thenReturn(2.0);
        lenient().when(properties.getLogStorage()).thenReturn(logStorageConfig);
        lenient().when(logStorageConfig.isLogSuccess()).thenReturn(true);
        lenient().when(logStorageConfig.isLogErrorStack()).thenReturn(true);
        lenient().when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-123");
        scheduler = new LocalTaskScheduler(properties, metrics, logStorage);
    }

    @Nested
    @DisplayName("MonitoredRunnable 测试")
    class MonitoredRunnableTests {

        @Test
        @DisplayName("执行成功时应该完成任务")
        void shouldCompleteTaskSuccessfully() throws InterruptedException {
            // given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger executionCount = new AtomicInteger(0);
            Runnable task = () -> {
                executionCount.incrementAndGet();
                latch.countDown();
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("test-task", 1000);

            // when
            scheduler.schedule(definition, task);

            // then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executionCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("执行失败时应该捕获异常")
        void shouldCaptureExceptionOnFailure() throws InterruptedException {
            // given
            CountDownLatch latch = new CountDownLatch(1);
            Runnable task = () -> {
                latch.countDown();
                throw new RuntimeException("Test error");
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("test-task-fail", 10000)
                    .withRetry(0, 0);

            // when
            scheduler.schedule(definition, task);

            // then - 任务应该执行完成（即使抛出异常）
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }
    }

    @Nested
    @DisplayName("RetryRunnable 测试")
    class RetryRunnableTests {

        @Test
        @DisplayName("失败后应该重试指定次数")
        void shouldRetryAfterFailure() throws InterruptedException {
            // given
            int maxRetries = 2;
            CountDownLatch latch = new CountDownLatch(maxRetries + 1); // 初始执行 + 重试次数
            AtomicInteger attemptCount = new AtomicInteger(0);

            Runnable task = () -> {
                attemptCount.incrementAndGet();
                latch.countDown();
                throw new RuntimeException("Test error");
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("retry-task", 10000)
                    .withRetry(maxRetries, 100);

            // when
            scheduler.schedule(definition, task);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(attemptCount.get()).isEqualTo(maxRetries + 1);
        }

        @Test
        @DisplayName("重试成功后应该停止重试")
        void shouldStopRetryOnSuccess() throws InterruptedException {
            // given
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger attemptCount = new AtomicInteger(0);

            Runnable task = () -> {
                int attempt = attemptCount.incrementAndGet();
                latch.countDown();
                if (attempt < 2) {
                    throw new RuntimeException("Test error");
                }
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("retry-success-task", 10000)
                    .withRetry(3, 100);

            // when
            scheduler.schedule(definition, task);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // 等待确保没有更多重试
            Thread.sleep(300);
            assertThat(attemptCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("达到最大重试次数后应该停止")
        void shouldStopAfterMaxRetries() throws InterruptedException {
            // given
            int maxRetries = 2;
            CountDownLatch latch = new CountDownLatch(maxRetries + 1);
            AtomicInteger attemptCount = new AtomicInteger(0);

            Runnable task = () -> {
                attemptCount.incrementAndGet();
                latch.countDown();
                throw new RuntimeException("Test error");
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("max-retry-task", 10000)
                    .withRetry(maxRetries, 50);

            // when
            scheduler.schedule(definition, task);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // 等待确保没有更多重试
            Thread.sleep(300);
            assertThat(attemptCount.get()).isEqualTo(maxRetries + 1);
        }
    }

    @Nested
    @DisplayName("指数退避测试")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("重试延迟应该按指数增长")
        void shouldIncreaseDelayExponentially() throws InterruptedException {
            // given
            long baseDelay = 100;
            int maxRetries = 2;
            CountDownLatch latch = new CountDownLatch(maxRetries + 1);
            long[] executionTimes = new long[maxRetries + 1];
            AtomicInteger index = new AtomicInteger(0);

            Runnable task = () -> {
                executionTimes[index.getAndIncrement()] = System.currentTimeMillis();
                latch.countDown();
                throw new RuntimeException("Test error");
            };

            TaskDefinition definition = TaskDefinition.ofFixedRate("backoff-task", 10000)
                    .withRetry(maxRetries, baseDelay);

            // when
            scheduler.schedule(definition, task);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // 验证延迟增长
            // 第一次重试延迟: baseDelay * 2^0 = 100ms
            // 第二次重试延迟: baseDelay * 2^1 = 200ms
            long firstRetryDelay = executionTimes[1] - executionTimes[0];
            long secondRetryDelay = executionTimes[2] - executionTimes[1];

            // 允许一定误差
            assertThat(firstRetryDelay).isGreaterThanOrEqualTo(baseDelay - 50);
            assertThat(secondRetryDelay).isGreaterThanOrEqualTo(baseDelay * 2 - 50);
        }
    }
}