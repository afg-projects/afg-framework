package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link BatchOperationException} 单元测试。
 * <p>
 * 测试批量操作异常的构造方法和异常链传递。
 *
 * @see BatchOperationException
 */
@DisplayName("BatchOperationException 测试")
class BatchOperationExceptionTest {

    /**
     * 构造方法测试。
     * <p>
     * 验证各种构造方法创建异常实例的正确性。
     */
    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        /**
         * 测试仅使用消息创建异常。
         */
        @Test
        @DisplayName("应该使用消息创建异常")
        void shouldCreateWithMessage() {
            BatchOperationException ex = new BatchOperationException("批量操作失败");

            assertThat(ex.getMessage()).isEqualTo("批量操作失败");
            assertThat(ex.getCause()).isNull();
        }

        /**
         * 测试使用消息和原因异常创建异常，验证异常链传递。
         */
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
