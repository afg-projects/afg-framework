package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.scheduler.InMemoryTaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.exception.SchedulerException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * LocalTaskScheduler 测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalTaskScheduler Tests")
class LocalTaskSchedulerTest {

    private LocalTaskScheduler scheduler;
    private MeterRegistry meterRegistry;
    private SchedulerProperties properties;
    private TaskExecutionLogStorage logStorage;
    private TaskExecutionMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new SchedulerProperties();
        properties.setThreadPoolSize(2);
        logStorage = new InMemoryTaskExecutionLogStorage(100);
        metrics = new TaskExecutionMetrics(meterRegistry, properties.getMetrics());
        scheduler = new LocalTaskScheduler(properties, metrics, logStorage);
    }

    @Test
    @DisplayName("Should schedule task at fixed rate")
    void shouldScheduleAtFixedRate() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        scheduler.scheduleAtFixedRate("test-task", () -> {
            counter.incrementAndGet();
            latch.countDown();
        }, Duration.ofMillis(100));

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(counter.get()).isGreaterThanOrEqualTo(3);

        scheduler.cancel("test-task");
    }

    @Test
    @DisplayName("Should schedule task with fixed delay")
    void shouldScheduleWithFixedDelay() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        scheduler.scheduleWithFixedDelay("test-task", () -> {
            counter.incrementAndGet();
            latch.countDown();
        }, Duration.ofMillis(100));

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(counter.get()).isGreaterThanOrEqualTo(2);

        scheduler.cancel("test-task");
    }

    @Test
    @DisplayName("Should schedule one-time task")
    void shouldScheduleOnce() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.scheduleOnce("test-task", () -> {
            counter.incrementAndGet();
            latch.countDown();
        }, Instant.now().plusMillis(100));

        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject duplicate task ID")
    void shouldRejectDuplicateTaskId() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        assertThatThrownBy(() ->
            scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1))
        ).isInstanceOf(SchedulerException.class)
         .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should cancel task")
    void shouldCancelTask() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        boolean result = scheduler.cancel("test-task");

        assertThat(result).isTrue();
        assertThat(scheduler.hasTask("test-task")).isFalse();
    }

    @Test
    @DisplayName("Should return false when canceling non-existent task")
    void shouldReturnFalseWhenCancelingNonExistentTask() {
        boolean result = scheduler.cancel("non-existent");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should check if task exists")
    void shouldCheckTaskExists() {
        assertThat(scheduler.hasTask("test-task")).isFalse();

        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        assertThat(scheduler.hasTask("test-task")).isTrue();
    }

    @Test
    @DisplayName("Should get task status")
    void shouldGetTaskStatus() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        TaskStatus status = scheduler.getTaskStatus("test-task");

        assertThat(status).isEqualTo(TaskStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should pause task")
    void shouldPauseTask() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        scheduler.pause("test-task");

        assertThat(scheduler.getTaskStatus("test-task")).isEqualTo(TaskStatus.PAUSED);
    }

    @Test
    @DisplayName("Should throw when resuming task without definition")
    void shouldThrowWhenResumingTaskWithoutDefinition() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        scheduler.pause("test-task");

        // resume 需要任务定义，没有定义时会抛出异常
        assertThatThrownBy(() -> scheduler.resume("test-task"))
            .isInstanceOf(SchedulerException.class)
            .hasMessageContaining("Task definition not found");
    }

    @Test
    @DisplayName("Should throw when pausing non-existent task")
    void shouldThrowWhenPausingNonExistentTask() {
        assertThatThrownBy(() -> scheduler.pause("non-existent"))
            .isInstanceOf(SchedulerException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should record execution logs")
    void shouldRecordExecutionLogs() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.scheduleOnce("test-task", latch::countDown, Instant.now().plusMillis(50));

        latch.await(5, TimeUnit.SECONDS);

        // 等待日志记录
        Thread.sleep(100);

        long count = logStorage.countByTaskId("test-task");
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
        scheduler.scheduleAtFixedRate("test-task", () -> {}, Duration.ofSeconds(1));

        scheduler.shutdown();

        // 调度器关闭后不应接受新任务
        // 但这不会抛异常，只是不会执行
    }
}
