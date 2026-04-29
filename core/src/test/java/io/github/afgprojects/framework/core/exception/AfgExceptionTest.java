package io.github.afgprojects.framework.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AfgExceptionTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 code 和 message 构造")
        void shouldCreateWithCodeAndMessage() {
            AfgException ex = new TestAfgException(10001, "测试异常");

            assertThat(ex.getCode()).isEqualTo(10001);
            assertThat(ex.getMessage()).isEqualTo("测试异常");
        }

        @Test
        @DisplayName("使用 code、message 和 cause 构造")
        void shouldCreateWithCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("原始异常");
            AfgException ex = new TestAfgException(10002, "包装异常", cause);

            assertThat(ex.getCode()).isEqualTo(10002);
            assertThat(ex.getMessage()).isEqualTo("包装异常");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("格式化方法测试")
    class FormatTests {

        @Test
        @DisplayName("应返回格式化的错误码")
        void shouldReturnFormattedCode() {
            AfgException ex = new TestAfgException(10001, "test");

            assertThat(ex.formatCode()).isEqualTo("E10001");
        }
    }

    @Nested
    @DisplayName("继承关系测试")
    class InheritanceTests {

        @Test
        @DisplayName("应继承 RuntimeException")
        void shouldBeRuntimeException() {
            AfgException ex = new TestAfgException(10001, "test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }

    // 测试用具体实现
    private static class TestAfgException extends AfgException {
        public TestAfgException(int code, String message) {
            super(code, message);
        }

        public TestAfgException(int code, String message, Throwable cause) {
            super(code, message, cause);
        }
    }
}
