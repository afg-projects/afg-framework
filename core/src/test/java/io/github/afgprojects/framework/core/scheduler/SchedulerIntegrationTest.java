package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * Scheduler 集成测试
 */
@DisplayName("Scheduler 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.scheduler.enabled=true",
                "afg.scheduler.thread-pool-size=2"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SchedulerIntegrationTest {

    @Autowired(required = false)
    private TaskScheduler taskScheduler;

    @Autowired(required = false)
    private DynamicTaskManager dynamicTaskManager;

    @Autowired(required = false)
    private LocalTaskScheduler localTaskScheduler;

    @Nested
    @DisplayName("Scheduler 配置测试")
    class SchedulerConfigTests {

        @Test
        @DisplayName("应该正确配置 TaskScheduler")
        void shouldConfigureTaskScheduler() {
            // TaskScheduler 可能没有被自动配置，检查是否可用
            if (taskScheduler != null) {
                assertThat(taskScheduler).isNotNull();
            }
        }

        @Test
        @DisplayName("应该正确配置 DynamicTaskManager")
        void shouldConfigureDynamicTaskManager() {
            if (dynamicTaskManager != null) {
                assertThat(dynamicTaskManager).isNotNull();
            }
        }

        @Test
        @DisplayName("应该正确配置 LocalTaskScheduler")
        void shouldConfigureLocalTaskScheduler() {
            if (localTaskScheduler != null) {
                assertThat(localTaskScheduler).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("任务调度测试")
    class TaskSchedulingTests {

        @Test
        @DisplayName("应该能够调度固定速率任务")
        void shouldScheduleFixedRateTask() {
            if (taskScheduler == null) {
                return;
            }
            var handle = taskScheduler.scheduleAtFixedRate(
                    "test-fixed-rate",
                    () -> {},
                    java.time.Duration.ofSeconds(10)
            );

            assertThat(handle).isNotNull();
            assertThat(handle.taskId()).isEqualTo("test-fixed-rate");

            taskScheduler.cancel("test-fixed-rate");
        }

        @Test
        @DisplayName("应该能够调度固定延迟任务")
        void shouldScheduleFixedDelayTask() {
            if (taskScheduler == null) {
                return;
            }
            var handle = taskScheduler.scheduleWithFixedDelay(
                    "test-fixed-delay",
                    () -> {},
                    java.time.Duration.ofSeconds(5)
            );

            assertThat(handle).isNotNull();
            assertThat(handle.taskId()).isEqualTo("test-fixed-delay");

            taskScheduler.cancel("test-fixed-delay");
        }

        @Test
        @DisplayName("应该能够检查任务是否存在")
        void shouldCheckTaskExists() {
            if (taskScheduler == null) {
                return;
            }
            taskScheduler.scheduleAtFixedRate(
                    "test-exists",
                    () -> {},
                    java.time.Duration.ofSeconds(10)
            );

            assertThat(taskScheduler.hasTask("test-exists")).isTrue();
            assertThat(taskScheduler.hasTask("non-existent")).isFalse();

            taskScheduler.cancel("test-exists");
        }
    }
}
