package io.github.afgprojects.framework.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BatchError 测试
 */
@DisplayName("BatchError 测试")
class BatchErrorTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建简单错误信息")
        void shouldCreateSimpleError() {
            BatchError error = BatchError.of(0, "处理失败");

            assertThat(error.index()).isEqualTo(0);
            assertThat(error.item()).isNull();
            assertThat(error.error()).isEqualTo("处理失败");
            assertThat(error.cause()).isNull();
        }

        @Test
        @DisplayName("应该创建包含元素的错误信息")
        void shouldCreateErrorWithItem() {
            BatchError error = BatchError.of(1, "item-1", "处理失败");

            assertThat(error.index()).isEqualTo(1);
            assertThat(error.item()).isEqualTo("item-1");
            assertThat(error.error()).isEqualTo("处理失败");
            assertThat(error.cause()).isNull();
        }

        @Test
        @DisplayName("应该创建包含异常的错误信息")
        void shouldCreateErrorWithException() {
            RuntimeException ex = new RuntimeException("测试异常");
            BatchError error = BatchError.of(2, "item-2", "处理失败", ex);

            assertThat(error.index()).isEqualTo(2);
            assertThat(error.item()).isEqualTo("item-2");
            assertThat(error.error()).isEqualTo("处理失败");
            assertThat(error.cause()).isEqualTo("java.lang.RuntimeException");
        }

        @Test
        @DisplayName("应该处理 null 异常")
        void shouldHandleNullException() {
            BatchError error = BatchError.of(3, "item-3", "处理失败", null);

            assertThat(error.cause()).isNull();
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            BatchError error1 = new BatchError(0, "item", "error", null);
            BatchError error2 = new BatchError(0, "item", "error", null);

            assertThat(error1).isEqualTo(error2);
        }

        @Test
        @DisplayName("应该正确实现 hashCode")
        void shouldImplementHashCode() {
            BatchError error1 = new BatchError(0, "item", "error", null);
            BatchError error2 = new BatchError(0, "item", "error", null);

            assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
        }
    }
}
