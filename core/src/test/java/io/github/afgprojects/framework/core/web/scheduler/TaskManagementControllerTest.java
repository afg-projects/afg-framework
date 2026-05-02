package io.github.afgprojects.framework.core.web.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.model.result.Result;

/**
 * TaskManagementController 测试
 */
@DisplayName("TaskManagementController 测试")
@ExtendWith(MockitoExtension.class)
class TaskManagementControllerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private TaskExecutionLogStorage logStorage;

    @Mock
    private DynamicTaskConfigSource configSource;

    private TaskManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new TaskManagementController(taskScheduler, logStorage, configSource);
    }

    @Nested
    @DisplayName("exists 测试")
    class ExistsTests {

        @Test
        @DisplayName("应该返回任务存在")
        void shouldReturnTaskExists() {
            when(taskScheduler.hasTask("task-1")).thenReturn(true);

            Result<Boolean> result = controller.exists("task-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isTrue();
        }

        @Test
        @DisplayName("应该返回任务不存在")
        void shouldReturnTaskNotExists() {
            when(taskScheduler.hasTask("task-1")).thenReturn(false);

            Result<Boolean> result = controller.exists("task-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isFalse();
        }
    }

    @Nested
    @DisplayName("getStatus 测试")
    class GetStatusTests {

        @Test
        @DisplayName("应该返回任务状态")
        void shouldReturnTaskStatus() {
            when(taskScheduler.getTaskStatus("task-1")).thenReturn(TaskStatus.RUNNING);
            when(taskScheduler.hasTask("task-1")).thenReturn(true);

            Result<TaskManagementController.TaskStatusInfo> result = controller.getStatus("task-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().taskId()).isEqualTo("task-1");
            assertThat(result.data().status()).isEqualTo(TaskStatus.RUNNING);
            assertThat(result.data().exists()).isTrue();
        }

        @Test
        @DisplayName("应该返回任务未找到")
        void shouldReturnTaskNotFound() {
            when(taskScheduler.getTaskStatus("task-1")).thenReturn(null);

            Result<TaskManagementController.TaskStatusInfo> result = controller.getStatus("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("pause 测试")
    class PauseTests {

        @Test
        @DisplayName("应该成功暂停任务")
        void shouldPauseTask() {
            doNothing().when(taskScheduler).pause("task-1");

            Result<Void> result = controller.pause("task-1");

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该处理暂停失败")
        void shouldHandlePauseFailure() {
            doThrow(new RuntimeException("Pause failed")).when(taskScheduler).pause("task-1");

            Result<Void> result = controller.pause("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("resume 测试")
    class ResumeTests {

        @Test
        @DisplayName("应该成功恢复任务")
        void shouldResumeTask() {
            doNothing().when(taskScheduler).resume("task-1");

            Result<Void> result = controller.resume("task-1");

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该处理恢复失败")
        void shouldHandleResumeFailure() {
            doThrow(new RuntimeException("Resume failed")).when(taskScheduler).resume("task-1");

            Result<Void> result = controller.resume("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("cancel 测试")
    class CancelTests {

        @Test
        @DisplayName("应该成功取消任务")
        void shouldCancelTask() {
            when(taskScheduler.cancel("task-1")).thenReturn(true);

            Result<Boolean> result = controller.cancel("task-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isTrue();
        }

        @Test
        @DisplayName("应该返回任务未找到")
        void shouldReturnTaskNotFound() {
            when(taskScheduler.cancel("task-1")).thenReturn(false);

            Result<Boolean> result = controller.cancel("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("getHistory 测试")
    class GetHistoryTests {

        @Test
        @DisplayName("应该返回执行历史")
        void shouldReturnHistory() {
            List<TaskExecutionLog> logs = Collections.emptyList();
            when(logStorage.findByTaskId("task-1", 20)).thenReturn(logs);

            Result<List<TaskExecutionLog>> result = controller.getHistory("task-1", 20);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFailures 测试")
    class GetFailuresTests {

        @Test
        @DisplayName("应该返回失败执行记录")
        void shouldReturnFailures() {
            List<TaskExecutionLog> logs = Collections.emptyList();
            when(logStorage.findFailedExecutions("task-1", 20)).thenReturn(logs);

            Result<List<TaskExecutionLog>> result = controller.getFailures("task-1", 20);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStatistics 测试")
    class GetStatisticsTests {

        @Test
        @DisplayName("应该返回执行统计")
        void shouldReturnStatistics() {
            when(logStorage.countByTaskId("task-1")).thenReturn(100L);
            when(logStorage.countSuccessByTaskId("task-1")).thenReturn(90L);
            when(logStorage.countFailedByTaskId("task-1")).thenReturn(10L);
            when(logStorage.getAverageExecutionTime("task-1")).thenReturn(150.0);

            Result<TaskManagementController.TaskStatistics> result = controller.getStatistics("task-1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().totalExecutions()).isEqualTo(100L);
            assertThat(result.data().successCount()).isEqualTo(90L);
            assertThat(result.data().failureCount()).isEqualTo(10L);
            assertThat(result.data().averageExecutionTimeMs()).isEqualTo(150.0);
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @Test
        @DisplayName("应该返回任务配置")
        void shouldReturnConfig() {
            TaskDefinition definition = mock(TaskDefinition.class);
            when(configSource.load("task-1")).thenReturn(Optional.of(definition));

            Result<TaskDefinition> result = controller.getConfig("task-1");

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该返回配置未找到")
        void shouldReturnConfigNotFound() {
            when(configSource.load("task-1")).thenReturn(Optional.empty());

            Result<TaskDefinition> result = controller.getConfig("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("无配置源测试")
    class NoConfigSourceTests {

        @Test
        @DisplayName("应该返回服务不可用")
        void shouldReturnServiceUnavailable() {
            TaskManagementController controllerNoSource = new TaskManagementController(
                    taskScheduler, logStorage, null);

            Result<TaskDefinition> result = controllerNoSource.getConfig("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(503);
        }

        @Test
        @DisplayName("更新配置应该返回服务不可用")
        void updateConfigShouldReturnServiceUnavailable() {
            TaskManagementController controllerNoSource = new TaskManagementController(
                    taskScheduler, logStorage, null);
            TaskDefinition definition = mock(TaskDefinition.class);

            Result<Void> result = controllerNoSource.updateConfig("task-1", definition);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(503);
        }

        @Test
        @DisplayName("删除配置应该返回服务不可用")
        void deleteConfigShouldReturnServiceUnavailable() {
            TaskManagementController controllerNoSource = new TaskManagementController(
                    taskScheduler, logStorage, null);

            Result<Void> result = controllerNoSource.deleteConfig("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(503);
        }

        @Test
        @DisplayName("刷新应该返回服务不可用")
        void refreshShouldReturnServiceUnavailable() {
            TaskManagementController controllerNoSource = new TaskManagementController(
                    taskScheduler, logStorage, null);

            Result<Void> result = controllerNoSource.refresh();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(503);
        }
    }

    @Nested
    @DisplayName("updateConfig 测试")
    class UpdateConfigTests {

        @Test
        @DisplayName("应该成功更新配置")
        void shouldUpdateConfig() {
            TaskDefinition definition = mock(TaskDefinition.class);
            doNothing().when(configSource).save(definition);

            Result<Void> result = controller.updateConfig("task-1", definition);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该处理更新失败")
        void shouldHandleUpdateFailure() {
            TaskDefinition definition = mock(TaskDefinition.class);
            doThrow(new RuntimeException("Update failed")).when(configSource).save(definition);

            Result<Void> result = controller.updateConfig("task-1", definition);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("deleteConfig 测试")
    class DeleteConfigTests {

        @Test
        @DisplayName("应该成功删除配置")
        void shouldDeleteConfig() {
            doNothing().when(configSource).delete("task-1");

            Result<Void> result = controller.deleteConfig("task-1");

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该处理删除失败")
        void shouldHandleDeleteFailure() {
            doThrow(new RuntimeException("Delete failed")).when(configSource).delete("task-1");

            Result<Void> result = controller.deleteConfig("task-1");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("refresh 测试")
    class RefreshTests {

        @Test
        @DisplayName("应该成功刷新配置")
        void shouldRefresh() {
            doNothing().when(configSource).refresh();

            Result<Void> result = controller.refresh();

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应该处理刷新失败")
        void shouldHandleRefreshFailure() {
            doThrow(new RuntimeException("Refresh failed")).when(configSource).refresh();

            Result<Void> result = controller.refresh();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.code()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Record 测试")
    class RecordTests {

        @Test
        @DisplayName("TaskStatusInfo 应该正确创建")
        void shouldCreateTaskStatusInfo() {
            Instant now = Instant.now();
            TaskManagementController.TaskStatusInfo info = new TaskManagementController.TaskStatusInfo(
                    "task-1", TaskStatus.RUNNING, true, now);

            assertThat(info.taskId()).isEqualTo("task-1");
            assertThat(info.status()).isEqualTo(TaskStatus.RUNNING);
            assertThat(info.exists()).isTrue();
            assertThat(info.checkedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("TaskStatistics 应该正确创建")
        void shouldCreateTaskStatistics() {
            TaskManagementController.TaskStatistics stats = new TaskManagementController.TaskStatistics(
                    "task-1", 100, 90, 10, 150.0);

            assertThat(stats.taskId()).isEqualTo("task-1");
            assertThat(stats.totalExecutions()).isEqualTo(100);
            assertThat(stats.successCount()).isEqualTo(90);
            assertThat(stats.failureCount()).isEqualTo(10);
            assertThat(stats.averageExecutionTimeMs()).isEqualTo(150.0);
        }
    }
}
