package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.scheduler.InMemoryTaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.exception.SchedulerException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * DynamicTaskManager Tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicTaskManager Tests")
class DynamicTaskManagerTest {

    private LocalTaskScheduler taskScheduler;
    private DynamicTaskManager taskManager;
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
        taskScheduler = new LocalTaskScheduler(properties, metrics, logStorage);
        taskManager = new DynamicTaskManager(taskScheduler, null);
    }

    @Test
    @DisplayName("Should register cron task")
    void shouldRegisterCronTask() {
        TaskDefinition definition = TaskDefinition.ofCron("test-cron-task", "0 0 5 * * ?")
            .withDescription("Test cron task");

        TaskScheduler.ScheduleHandle handle = taskManager.registerTask(definition, () -> {});

        assertThat(handle).isNotNull();
        assertThat(handle.taskId()).isEqualTo("test-cron-task");
        assertThat(taskManager.hasTask("test-cron-task")).isTrue();
        assertThat(taskManager.getTaskStatus("test-cron-task")).isEqualTo(TaskStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should register fixed rate task")
    void shouldRegisterFixedRateTask() {
        TaskDefinition definition = TaskDefinition.ofFixedRate("test-fixed-task", 5000);

        TaskScheduler.ScheduleHandle handle = taskManager.registerTask(definition, () -> {});

        assertThat(handle).isNotNull();
        assertThat(taskManager.hasTask("test-fixed-task")).isTrue();
    }

    @Test
    @DisplayName("Should register fixed delay task")
    void shouldRegisterFixedDelayTask() {
        TaskDefinition definition = TaskDefinition.ofFixedDelay("test-delay-task", 3000);

        TaskScheduler.ScheduleHandle handle = taskManager.registerTask(definition, () -> {});

        assertThat(handle).isNotNull();
        assertThat(taskManager.hasTask("test-delay-task")).isTrue();
    }

    @Test
    @DisplayName("Should reject duplicate task registration")
    void shouldRejectDuplicateTask() {
        TaskDefinition definition = TaskDefinition.ofFixedRate("test-task", 5000);
        taskManager.registerTask(definition, () -> {});

        assertThatThrownBy(() ->
            taskManager.registerTask(definition, () -> {})
        ).isInstanceOf(SchedulerException.class)
         .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Should cancel task")
    void shouldCancelTask() {
        TaskDefinition definition = TaskDefinition.ofFixedRate("test-task", 5000);
        taskManager.registerTask(definition, () -> {});

        boolean result = taskManager.cancelTask("test-task");

        assertThat(result).isTrue();
        assertThat(taskManager.hasTask("test-task")).isFalse();
    }

    @Test
    @DisplayName("Should return false when canceling non-existent task")
    void shouldReturnFalseWhenCancelingNonExistent() {
        boolean result = taskManager.cancelTask("non-existent");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should pause and resume task")
    void shouldPauseAndResumeTask() {
        TaskDefinition definition = TaskDefinition.ofFixedRate("test-task", 10000);
        taskManager.registerTask(definition, () -> {});

        taskManager.pauseTask("test-task");
        assertThat(taskScheduler.getTaskStatus("test-task")).isEqualTo(TaskStatus.PAUSED);

        taskManager.resumeTask("test-task");
        assertThat(taskScheduler.getTaskStatus("test-task")).isEqualTo(TaskStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should throw when pausing non-existent task")
    void shouldThrowWhenPausingNonExistent() {
        assertThatThrownBy(() -> taskManager.pauseTask("non-existent"))
            .isInstanceOf(SchedulerException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should update task configuration")
    void shouldUpdateTaskConfig() {
        TaskDefinition original = TaskDefinition.ofFixedRate("test-task", 5000);
        taskManager.registerTask(original, () -> {});

        TaskDefinition updated = TaskDefinition.ofFixedRate("test-task", 3000);
        taskManager.updateTaskConfig(updated);

        TaskDefinition retrieved = taskManager.getTaskDefinition("test-task");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.fixedRate()).isEqualTo(3000);
    }

    @Test
    @DisplayName("Should get all task statuses")
    void shouldGetAllTaskStatuses() {
        taskManager.registerTask(TaskDefinition.ofFixedRate("task-1", 5000), () -> {});
        taskManager.registerTask(TaskDefinition.ofFixedRate("task-2", 3000), () -> {});

        Map<String, TaskStatus> statuses = taskManager.getAllTaskStatuses();

        assertThat(statuses).hasSize(2);
        assertThat(statuses.get("task-1")).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(statuses.get("task-2")).isEqualTo(TaskStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should get all task definitions")
    void shouldGetAllTaskDefinitions() {
        taskManager.registerTask(TaskDefinition.ofFixedRate("task-1", 5000), () -> {});
        taskManager.registerTask(TaskDefinition.ofCron("task-2", "0 0 5 * * ?"), () -> {});

        var definitions = taskManager.getAllTaskDefinitions();

        assertThat(definitions).hasSize(2);
        assertThat(definitions.stream().anyMatch(d -> d.taskId().equals("task-1"))).isTrue();
        assertThat(definitions.stream().anyMatch(d -> d.taskId().equals("task-2"))).isTrue();
    }

    @Test
    @DisplayName("Should handle invalid task definition")
    void shouldHandleInvalidTaskDefinition() {
        TaskDefinition invalid = new TaskDefinition(
            "invalid-task", "default", null, -1, -1, 0, null, true, -1, 0, 0, null
        );

        assertThatThrownBy(() ->
            taskManager.registerTask(invalid, () -> {})
        ).isInstanceOf(SchedulerException.class)
         .hasMessageContaining("Invalid task definition");
    }

    @Test
    @DisplayName("Should get task definition")
    void shouldGetTaskDefinition() {
        TaskDefinition definition = TaskDefinition.ofFixedRate("test-task", 5000)
            .withDescription("My test task");

        taskManager.registerTask(definition, () -> {});

        TaskDefinition retrieved = taskManager.getTaskDefinition("test-task");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.taskId()).isEqualTo("test-task");
        assertThat(retrieved.description()).isEqualTo("My test task");
    }

    @Test
    @DisplayName("Should return null for non-existent task definition")
    void shouldReturnNullForNonExistent() {
        TaskDefinition result = taskManager.getTaskDefinition("non-existent");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should check if task exists")
    void shouldCheckTaskExists() {
        assertThat(taskManager.hasTask("test-task")).isFalse();

        taskManager.registerTask(TaskDefinition.ofFixedRate("test-task", 5000), () -> {});

        assertThat(taskManager.hasTask("test-task")).isTrue();
    }
}