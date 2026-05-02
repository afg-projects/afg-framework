package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BatchOperationException 测试
 */
@DisplayName("BatchOperationException 测试")
class BatchOperationExceptionTest {

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用消息创建异常")
        void shouldCreateWithMessage() {
            BatchOperationException ex = new BatchOperationException("批量操作失败");

            assertThat(ex.getMessage()).isEqualTo("批量操作失败");
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("应该使用消息和原因创建异常")
        void shouldCreateWithMessageAndCause() {
            Throwable cause = new RuntimeException("原始异常");
            BatchOperationException ex = new BatchOperationException("批量操作失败", cause);

            assertThat(ex.getMessage()).isEqualTo("批量操作失败");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }
}
