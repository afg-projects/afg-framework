package io.github.afgprojects.framework.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SchedulerException 测试
 */
@DisplayName("SchedulerException 测试")
class SchedulerExceptionTest {

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用消息创建异常")
        void shouldCreateWithMessage() {
            SchedulerException ex = new SchedulerException("任务执行失败");

            assertThat(ex.getMessage()).isEqualTo("任务执行失败");
            assertThat(ex.getCode()).isEqualTo(SchedulerException.SCHEDULER_ERROR);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("应该使用消息和原因创建异常")
        void shouldCreateWithMessageAndCause() {
            Throwable cause = new RuntimeException("原始异常");
            SchedulerException ex = new SchedulerException("任务执行失败", cause);

            assertThat(ex.getMessage()).isEqualTo("任务执行失败");
            assertThat(ex.getCode()).isEqualTo(SchedulerException.SCHEDULER_ERROR);
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("应该使用自定义错误码创建异常")
        void shouldCreateWithCustomCode() {
            SchedulerException ex = new SchedulerException(SchedulerException.JOB_NOT_FOUND, "任务不存在");

            assertThat(ex.getMessage()).isEqualTo("任务不存在");
            assertThat(ex.getCode()).isEqualTo(SchedulerException.JOB_NOT_FOUND);
        }

        @Test
        @DisplayName("应该使用自定义错误码和原因创建异常")
        void shouldCreateWithCustomCodeAndCause() {
            Throwable cause = new RuntimeException("原始异常");
            SchedulerException ex = new SchedulerException(SchedulerException.JOB_EXECUTION_ERROR, "执行失败", cause);

            assertThat(ex.getMessage()).isEqualTo("执行失败");
            assertThat(ex.getCode()).isEqualTo(SchedulerException.JOB_EXECUTION_ERROR);
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("错误码常量测试")
    class ErrorCodeTests {

        @Test
        @DisplayName("应该有正确的错误码常量")
        void shouldHaveCorrectErrorCodes() {
            assertThat(SchedulerException.SCHEDULER_ERROR).isEqualTo(90400);
            assertThat(SchedulerException.JOB_NOT_FOUND).isEqualTo(90401);
            assertThat(SchedulerException.JOB_EXECUTION_ERROR).isEqualTo(90402);
            assertThat(SchedulerException.JOB_ALREADY_EXISTS).isEqualTo(90403);
        }
    }

    @Nested
    @DisplayName("formatCode 测试")
    class FormatCodeTests {

        @Test
        @DisplayName("应该格式化错误码")
        void shouldFormatCode() {
            SchedulerException ex = new SchedulerException(SchedulerException.JOB_NOT_FOUND, "任务不存在");

            assertThat(ex.formatCode()).isEqualTo("E90401");
        }
    }
}
