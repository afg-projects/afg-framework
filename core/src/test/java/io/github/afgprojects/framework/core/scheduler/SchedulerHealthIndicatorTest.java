package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

/**
 * SchedulerHealthIndicator 测试
 */
@DisplayName("SchedulerHealthIndicator 测试")
@ExtendWith(MockitoExtension.class)
class SchedulerHealthIndicatorTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private TaskExecutionLogStorage logStorage;

    private SchedulerHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new SchedulerHealthIndicator(taskScheduler, logStorage);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用默认参数初始化")
        void shouldInitializeWithDefaults() {
            assertThat(indicator).isNotNull();
        }

        @Test
        @DisplayName("应该使用自定义参数初始化")
        void shouldInitializeWithCustomParams() {
            SchedulerHealthIndicator custom = new SchedulerHealthIndicator(
                    taskScheduler, logStorage, 0.3, 1800000);
            assertThat(custom).isNotNull();
        }
    }

    @Nested
    @DisplayName("health 测试")
    class HealthTests {

        @Test
        @DisplayName("应该返回健康状态")
        void shouldReturnHealthStatus() {
            Health health = indicator.health();

            assertThat(health).isNotNull();
            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("应该包含调度器类型")
        void shouldContainSchedulerType() {
            Health health = indicator.health();

            assertThat(health.getDetails()).containsKey("schedulerType");
        }

        @Test
        @DisplayName("应该包含执行统计")
        void shouldContainExecutionStats() {
            Health health = indicator.health();

            assertThat(health.getDetails()).containsKey("totalExecutions");
            assertThat(health.getDetails()).containsKey("failedExecutions");
            assertThat(health.getDetails()).containsKey("failureRate");
        }
    }

    @Nested
    @DisplayName("updateTaskHealth 测试")
    class UpdateTaskHealthTests {

        @Test
        @DisplayName("应该更新任务健康信息 - 成功")
        void shouldUpdateTaskHealthSuccess() {
            indicator.updateTaskHealth("task-1", true, null);

            SchedulerHealthIndicator.TaskHealthInfo info = indicator.getTaskHealth("task-1");
            assertThat(info).isNotNull();
            assertThat(info.getTotalCount()).isEqualTo(1);
            assertThat(info.getFailedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该更新任务健康信息 - 失败")
        void shouldUpdateTaskHealthFailure() {
            indicator.updateTaskHealth("task-1", false, "Error message");

            SchedulerHealthIndicator.TaskHealthInfo info = indicator.getTaskHealth("task-1");
            assertThat(info).isNotNull();
            assertThat(info.getTotalCount()).isEqualTo(1);
            assertThat(info.getFailedCount()).isEqualTo(1);
            assertThat(info.getLastError()).isEqualTo("Error message");
        }

        @Test
        @DisplayName("应该累积多次更新")
        void shouldAccumulateUpdates() {
            indicator.updateTaskHealth("task-1", true, null);
            indicator.updateTaskHealth("task-1", true, null);
            indicator.updateTaskHealth("task-1", false, "Error");

            SchedulerHealthIndicator.TaskHealthInfo info = indicator.getTaskHealth("task-1");
            assertThat(info.getTotalCount()).isEqualTo(3);
            assertThat(info.getFailedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getTaskHealth 测试")
    class GetTaskHealthTests {

        @Test
        @DisplayName("应该返回 null 当任务不存在")
        void shouldReturnNullWhenTaskNotFound() {
            SchedulerHealthIndicator.TaskHealthInfo info = indicator.getTaskHealth("non-existent");
            assertThat(info).isNull();
        }
    }

    @Nested
    @DisplayName("clearTaskHealth 测试")
    class ClearTaskHealthTests {

        @Test
        @DisplayName("应该清除任务健康信息")
        void shouldClearTaskHealth() {
            indicator.updateTaskHealth("task-1", true, null);
            indicator.clearTaskHealth("task-1");

            SchedulerHealthIndicator.TaskHealthInfo info = indicator.getTaskHealth("task-1");
            assertThat(info).isNull();
        }
    }

    @Nested
    @DisplayName("TaskHealthInfo 测试")
    class TaskHealthInfoTests {

        @Test
        @DisplayName("应该正确计算失败率")
        void shouldCalculateFailureRate() {
            SchedulerHealthIndicator.TaskHealthInfo info = new SchedulerHealthIndicator.TaskHealthInfo();

            info.update(true, null);
            info.update(true, null);
            info.update(false, "Error");

            assertThat(info.getFailureRate()).isEqualTo(1.0 / 3.0);
        }

        @Test
        @DisplayName("应该返回 0 失败率当无执行")
        void shouldReturnZeroFailureRateWhenNoExecutions() {
            SchedulerHealthIndicator.TaskHealthInfo info = new SchedulerHealthIndicator.TaskHealthInfo();
            assertThat(info.getFailureRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("应该记录最后成功时间")
        void shouldRecordLastSuccessTime() {
            SchedulerHealthIndicator.TaskHealthInfo info = new SchedulerHealthIndicator.TaskHealthInfo();

            long before = System.currentTimeMillis();
            info.update(true, null);
            long after = System.currentTimeMillis();

            assertThat(info.getLastSuccessTime()).isBetween(before, after);
        }

        @Test
        @DisplayName("应该记录最后失败时间")
        void shouldRecordLastFailureTime() {
            SchedulerHealthIndicator.TaskHealthInfo info = new SchedulerHealthIndicator.TaskHealthInfo();

            long before = System.currentTimeMillis();
            info.update(false, "Error");
            long after = System.currentTimeMillis();

            assertThat(info.getLastFailureTime()).isBetween(before, after);
        }
    }
}
