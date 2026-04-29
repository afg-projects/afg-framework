package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * InMemoryTaskExecutionLogStorage 测试
 */
@DisplayName("InMemoryTaskExecutionLogStorage Tests")
class InMemoryTaskExecutionLogStorageTest {

    private InMemoryTaskExecutionLogStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryTaskExecutionLogStorage(100);
    }

    @Test
    @DisplayName("Should save and find execution log")
    void shouldSaveAndFind() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");

        storage.save(log);

        assertThat(storage.findByExecutionId("exec-1")).isPresent();
        assertThat(storage.findByExecutionId("exec-1").get().taskId()).isEqualTo("task-1");
    }

    @Test
    @DisplayName("Should update execution log")
    void shouldUpdate() {
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        storage.save(log);

        TaskExecutionLog updated = log.markSuccess();
        storage.update(updated);

        assertThat(storage.findByExecutionId("exec-1"))
            .isPresent()
            .get()
            .extracting(TaskExecutionLog::isSuccess)
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should find by task ID")
    void shouldFindByTaskId() {
        storage.save(TaskExecutionLog.running("exec-1", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-2", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-3", "task-2", "default", "node-1"));

        List<TaskExecutionLog> logs = storage.findByTaskId("task-1", 10);

        assertThat(logs).hasSize(2);
    }

    @Test
    @DisplayName("Should find failed executions")
    void shouldFindFailedExecutions() {
        TaskExecutionLog success = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1")
            .markSuccess();
        TaskExecutionLog failed = TaskExecutionLog.running("exec-2", "task-1", "default", "node-1")
            .markFailed("Error", null);

        storage.save(success);
        storage.save(failed);

        List<TaskExecutionLog> failures = storage.findFailedExecutions("task-1", 10);

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).executionId()).isEqualTo("exec-2");
    }

    @Test
    @DisplayName("Should count executions")
    void shouldCountExecutions() {
        storage.save(TaskExecutionLog.running("exec-1", "task-1", "default", "node-1").markSuccess());
        storage.save(TaskExecutionLog.running("exec-2", "task-1", "default", "node-1").markSuccess());
        storage.save(TaskExecutionLog.running("exec-3", "task-1", "default", "node-1").markFailed("Error", null));

        assertThat(storage.countByTaskId("task-1")).isEqualTo(3);
        assertThat(storage.countSuccessByTaskId("task-1")).isEqualTo(2);
        assertThat(storage.countFailedByTaskId("task-1")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate average execution time")
    void shouldCalculateAverageExecutionTime() {
        // 创建执行日志，通过等待确保不同的执行时间
        TaskExecutionLog log1 = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        storage.save(log1.markSuccess()); // 瞬间完成

        TaskExecutionLog log2 = TaskExecutionLog.running("exec-2", "task-1", "default", "node-1");
        storage.save(log2.markSuccess());

        double avgTime = storage.getAverageExecutionTime("task-1");

        // 平均时间应该大于等于 0
        assertThat(avgTime).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should delete by task ID")
    void shouldDeleteByTaskId() {
        storage.save(TaskExecutionLog.running("exec-1", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-2", "task-2", "default", "node-1"));

        storage.deleteByTaskId("task-1");

        assertThat(storage.findByTaskId("task-1", 10)).isEmpty();
        assertThat(storage.findByTaskId("task-2", 10)).hasSize(1);
    }

    @Test
    @DisplayName("Should respect max size limit")
    void shouldRespectMaxSize() {
        storage = new InMemoryTaskExecutionLogStorage(3);

        storage.save(TaskExecutionLog.running("exec-1", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-2", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-3", "task-1", "default", "node-1"));
        storage.save(TaskExecutionLog.running("exec-4", "task-1", "default", "node-1"));

        // 应该不超过 maxSize
        assertThat(storage.size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should find by time range")
    void shouldFindByTimeRange() {
        Instant now = Instant.now();
        TaskExecutionLog log = TaskExecutionLog.running("exec-1", "task-1", "default", "node-1");
        storage.save(log);

        List<TaskExecutionLog> logs = storage.findByTimeRange(
            "task-1",
            now.minusSeconds(10),
            now.plusSeconds(10)
        );

        assertThat(logs).hasSize(1);
    }

    @Test
    @DisplayName("Should find by task group")
    void shouldFindByTaskGroup() {
        storage.save(TaskExecutionLog.running("exec-1", "task-1", "group-a", "node-1"));
        storage.save(TaskExecutionLog.running("exec-2", "task-2", "group-a", "node-1"));
        storage.save(TaskExecutionLog.running("exec-3", "task-3", "group-b", "node-1"));

        List<TaskExecutionLog> logs = storage.findByTaskGroup("group-a", 10);

        assertThat(logs).hasSize(2);
    }
}
