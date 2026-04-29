package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TaskExecutionLog 测试
 */
@DisplayName("TaskExecutionLog Tests")
class TaskExecutionLogTest {

    @Test
    @DisplayName("Should create running log")
    void shouldCreateRunningLog() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");

        assertThat(log.executionId()).isEqualTo("exec-1");
        assertThat(log.taskId()).isEqualTo("task-1");
        assertThat(log.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.RUNNING);
        assertThat(log.endTime()).isNull();
    }

    @Test
    @DisplayName("Should mark success")
    void shouldMarkSuccess() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        TaskExecutionLog success = log.markSuccess();

        assertThat(success.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.SUCCESS);
        assertThat(success.endTime()).isNotNull();
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.isFailed()).isFalse();
    }

    @Test
    @DisplayName("Should mark failure")
    void shouldMarkFailure() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        TaskExecutionLog failed = log.markFailed("Something went wrong", "stack trace");

        assertThat(failed.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("Something went wrong");
        assertThat(failed.errorStack()).isEqualTo("stack trace");
        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.isFailed()).isTrue();
    }

    @Test
    @DisplayName("Should mark timeout")
    void shouldMarkTimeout() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        TaskExecutionLog timeout = log.markTimeout();

        assertThat(timeout.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.TIMEOUT);
        assertThat(timeout.errorMessage()).isEqualTo("Task execution timeout");
        assertThat(timeout.isFailed()).isTrue();
    }

    @Test
    @DisplayName("Should mark cancelled")
    void shouldMarkCancelled() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        TaskExecutionLog cancelled = log.markCancelled();

        assertThat(cancelled.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should mark skipped")
    void shouldMarkSkipped() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        TaskExecutionLog skipped = log.markSkipped("Lock not acquired");

        assertThat(skipped.status()).isEqualTo(TaskExecutionLog.ExecutionStatus.SKIPPED);
        assertThat(skipped.errorMessage()).isEqualTo("Lock not acquired");
    }

    @Test
    @DisplayName("Should increment retry count")
    void shouldIncrementRetry() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");

        assertThat(log.retried()).isEqualTo(0);

        log = log.incrementRetry();
        assertThat(log.retried()).isEqualTo(1);

        log = log.incrementRetry();
        assertThat(log.retried()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate duration")
    void shouldCalculateDuration() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");

        // 运行中的任务，duration 是到当前时间的差
        Duration duration = log.duration();
        assertThat(duration).isNotNull();
        assertThat(duration.toMillis()).isGreaterThanOrEqualTo(0);

        // 完成后，duration 是开始到结束的差
        TaskExecutionLog completed = log.markSuccess();
        Duration completedDuration = completed.duration();
        assertThat(completedDuration).isNotNull();
    }
}
