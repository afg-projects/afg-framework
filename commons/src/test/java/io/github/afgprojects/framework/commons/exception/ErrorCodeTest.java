package io.github.afgprojects.framework.commons.exception;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCode 接口测试
 * 通过自定义实现测试接口默认方法
 */
@DisplayName("ErrorCode 接口测试")
class ErrorCodeTest {

    @Nested
    @DisplayName("getCategory() 默认方法")
    class GetCategoryDefaultTests {

        @Test
        @DisplayName("默认应返回 BUSINESS 分类")
        void shouldReturnBusinessByDefault() {
            ErrorCode errorCode = new TestErrorCode(99999, "测试");

            assertThat(errorCode.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }
    }

    @Nested
    @DisplayName("formatCode() 默认方法")
    class FormatCodeDefaultTests {

        @Test
        @DisplayName("应返回 E 前缀的错误码字符串")
        void shouldReturnFormattedCode() {
            ErrorCode errorCode = new TestErrorCode(20001, "测试");

            assertThat(errorCode.formatCode()).isEqualTo("E20001");
        }
    }

    @Nested
    @DisplayName("getMessage(Locale) 默认方法")
    class GetMessageLocaleDefaultTests {

        @Test
        @DisplayName("应返回默认消息，忽略 locale")
        void shouldReturnDefaultMessageIgnoringLocale() {
            ErrorCode errorCode = new TestErrorCode(20001, "默认消息");

            assertThat(errorCode.getMessage(Locale.CHINA)).isEqualTo("默认消息");
            assertThat(errorCode.getMessage((Locale) null)).isEqualTo("默认消息");
        }
    }

    @Nested
    @DisplayName("getMessage(Object[], Locale) 默认方法")
    class GetMessageArgsLocaleDefaultTests {

        @Test
        @DisplayName("应使用 MessageFormat 对消息模板进行参数替换")
        void shouldFormatMessageWithArgs() {
            ErrorCode errorCode = new TestErrorCode(11000, "实体 {0} 不存在");

            assertThat(errorCode.getMessage(new Object[]{"User"}, null)).isEqualTo("实体 User 不存在");
        }

        @Test
        @DisplayName("应支持多个占位符")
        void shouldFormatMessageWithMultipleArgs() {
            ErrorCode errorCode = new TestErrorCode(10002, "参数 {0} 格式错误，期望 {1}");

            assertThat(errorCode.getMessage(new Object[]{"username", "String"}, null))
                    .isEqualTo("参数 username 格式错误，期望 String");
        }

        @Test
        @DisplayName("args 为 null 时应返回原始消息")
        void shouldReturnTemplateWhenArgsIsNull() {
            ErrorCode errorCode = new TestErrorCode(11000, "实体 {0} 不存在");

            assertThat(errorCode.getMessage(null, null)).isEqualTo("实体 {0} 不存在");
        }

        @Test
        @DisplayName("args 为空数组时应返回原始消息")
        void shouldReturnTemplateWhenArgsIsEmpty() {
            ErrorCode errorCode = new TestErrorCode(11000, "实体 {0} 不存在");

            assertThat(errorCode.getMessage(new Object[]{}, null)).isEqualTo("实体 {0} 不存在");
        }

        @Test
        @DisplayName("消息中无占位符时 args 应被忽略")
        void shouldIgnoreArgsWhenNoPlaceholders() {
            ErrorCode errorCode = new TestErrorCode(20001, "默认消息");

            assertThat(errorCode.getMessage(new Object[]{"arg1"}, Locale.US)).isEqualTo("默认消息");
        }
    }

    /**
     * 测试用 ErrorCode 最小实现
     */
    private record TestErrorCode(int code, String message) implements ErrorCode {

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
