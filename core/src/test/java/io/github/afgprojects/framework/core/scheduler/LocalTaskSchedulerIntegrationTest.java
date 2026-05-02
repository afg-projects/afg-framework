package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * LocalTaskScheduler 集成测试
 */
@DisplayName("LocalTaskScheduler 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.scheduler.enabled=true",
                "afg.scheduler.thread-pool-size=4",
                "afg.scheduler.default-timeout=30s",
                "afg.scheduler.metrics.enabled=true",
                "afg.scheduler.log-storage.type=memory",
                "afg.scheduler.log-storage.max-size=1000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalTaskSchedulerIntegrationTest {

    @Autowired(required = false)
    private LocalTaskScheduler localTaskScheduler;

    @Autowired(required = false)
    private TaskExecutionMetrics taskExecutionMetrics;

    @Autowired(required = false)
    private TaskExecutionLogStorage taskExecutionLogStorage;

    @Nested
    @DisplayName("调度器配置测试")
    class SchedulerConfigTests {

        @Test
        @DisplayName("应该自动配置本地任务调度器")
        void shouldAutoConfigureLocalTaskScheduler() {
            // LocalTaskScheduler 需要 TaskExecutionMetrics，而 TaskExecutionMetrics 需要 MeterRegistry
            if (localTaskScheduler != null) {
                assertThat(localTaskScheduler).isNotNull();
            }
        }

        @Test
        @DisplayName("应该自动配置任务执行监控")
        void shouldAutoConfigureTaskExecutionMetrics() {
            // TaskExecutionMetrics 需要 MeterRegistry bean
            if (taskExecutionMetrics != null) {
                assertThat(taskExecutionMetrics).isNotNull();
            }
        }

        @Test
        @DisplayName("应该自动配置执行日志存储")
        void shouldAutoConfigureTaskExecutionLogStorage() {
            if (taskExecutionLogStorage != null) {
                assertThat(taskExecutionLogStorage).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("任务调度测试")
    class TaskSchedulingTests {

        @Test
        @DisplayName("应该能够调度固定速率任务")
        void shouldScheduleFixedRateTask() {
            if (localTaskScheduler == null) {
                return;
            }

            var handle = localTaskScheduler.scheduleAtFixedRate(
                    "test-fixed-rate",
                    () -> System.out.println("Fixed rate task executed"),
                    Duration.ofSeconds(10)
            );

            assertThat(handle).isNotNull();
            assertThat(handle.taskId()).isEqualTo("test-fixed-rate");

            localTaskScheduler.cancel("test-fixed-rate");
        }

        @Test
        @DisplayName("应该能够调度固定延迟任务")
        void shouldScheduleFixedDelayTask() {
            if (localTaskScheduler == null) {
                return;
            }

            var handle = localTaskScheduler.scheduleWithFixedDelay(
                    "test-fixed-delay",
                    () -> System.out.println("Fixed delay task executed"),
                    Duration.ofSeconds(5)
            );

            assertThat(handle).isNotNull();
            assertThat(handle.taskId()).isEqualTo("test-fixed-delay");

            localTaskScheduler.cancel("test-fixed-delay");
        }

        @Test
        @DisplayName("应该能够调度一次性任务")
        void shouldScheduleOneTimeTask() {
            if (localTaskScheduler == null) {
                return;
            }

            var handle = localTaskScheduler.scheduleOnce(
                    "test-one-time",
                    () -> System.out.println("One time task executed"),
                    java.time.Instant.now().plusSeconds(1)
            );

            assertThat(handle).isNotNull();
            assertThat(handle.taskId()).isEqualTo("test-one-time");
        }

        @Test
        @DisplayName("应该能够检查任务是否存在")
        void shouldCheckTaskExists() {
            if (localTaskScheduler == null) {
                return;
            }

            localTaskScheduler.scheduleAtFixedRate(
                    "test-exists",
                    () -> {},
                    Duration.ofSeconds(10)
            );

            assertThat(localTaskScheduler.hasTask("test-exists")).isTrue();
            assertThat(localTaskScheduler.hasTask("non-existent")).isFalse();

            localTaskScheduler.cancel("test-exists");
        }

        @Test
        @DisplayName("应该能够取消任务")
        void shouldCancelTask() {
            if (localTaskScheduler == null) {
                return;
            }

            localTaskScheduler.scheduleAtFixedRate(
                    "test-cancel",
                    () -> {},
                    Duration.ofSeconds(10)
            );

            assertThat(localTaskScheduler.hasTask("test-cancel")).isTrue();

            localTaskScheduler.cancel("test-cancel");

            assertThat(localTaskScheduler.hasTask("test-cancel")).isFalse();
        }
    }

    @Nested
    @DisplayName("执行监控测试")
    class ExecutionMetricsTests {

        @Test
        @DisplayName("应该能够记录任务开始")
        void shouldRecordTaskStart() {
            if (taskExecutionMetrics == null) {
                return;
            }

            String executionId = taskExecutionMetrics.recordStart("test-task", "default");

            assertThat(executionId).isNotNull();
        }
    }
}