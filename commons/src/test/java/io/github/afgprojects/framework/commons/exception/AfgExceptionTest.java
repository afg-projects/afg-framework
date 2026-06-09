package io.github.afgprojects.framework.commons.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AfgException 测试
 */
@DisplayName("AfgException 测试")
class AfgExceptionTest {

    @Nested
    @DisplayName("AfgException(int code, String message) 构造器")
    class CodeMessageConstructorTests {

        @Test
        @DisplayName("应保留 code 和 message")
        void shouldRetainCodeAndMessage() {
            AfgException ex = new AfgException(10001, "测试异常") {};

            assertThat(ex.getCode()).isEqualTo(10001);
            assertThat(ex.getMessage()).isEqualTo("测试异常");
        }

        @Test
        @DisplayName("cause 应为 null")
        void shouldHaveNullCause() {
            AfgException ex = new AfgException(10001, "测试异常") {};

            assertThat(ex.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("AfgException(int code, String message, Throwable cause) 构造器")
    class CodeMessageCauseConstructorTests {

        @Test
        @DisplayName("应保留 code、message 和 cause")
        void shouldRetainCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("原始异常");
            AfgException ex = new AfgException(10002, "包装异常", cause) {};

            assertThat(ex.getCode()).isEqualTo(10002);
            assertThat(ex.getMessage()).isEqualTo("包装异常");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("formatCode() 方法")
    class FormatCodeTests {

        @Test
        @DisplayName("应返回 E 前缀的错误码")
        void shouldReturnFormattedCode() {
            AfgException ex = new AfgException(10001, "测试") {};

            assertThat(ex.formatCode()).isEqualTo("E10001");
        }

        @Test
        @DisplayName("code=0 时应返回 E0")
        void shouldReturnE0ForZeroCode() {
            AfgException ex = new AfgException(0, "成功") {};

            assertThat(ex.formatCode()).isEqualTo("E0");
        }
    }

    @Nested
    @DisplayName("继承关系")
    class InheritanceTests {

        @Test
        @DisplayName("AfgException 应继承 RuntimeException")
        void shouldExtendRuntimeException() {
            AfgException ex = new AfgException(10001, "测试") {};

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }
}
