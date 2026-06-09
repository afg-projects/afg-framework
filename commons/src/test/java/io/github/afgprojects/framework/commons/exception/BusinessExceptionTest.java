package io.github.afgprojects.framework.commons.exception;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException 测试
 */
@DisplayName("BusinessException 测试")
class BusinessExceptionTest {

    @Nested
    @DisplayName("BusinessException(String message) 构造器")
    class StringConstructorTests {

        @Test
        @DisplayName("应使用 FAIL 错误码和自定义消息")
        void shouldUseFailErrorCodeAndCustomMessage() {
            BusinessException ex = new BusinessException("自定义错误");

            assertThat(ex.getMessage()).isEqualTo("自定义错误");
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.FAIL);
            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.FAIL.getCode());
            assertThat(ex.isCustomMessage()).isTrue();
            assertThat(ex.getArgs()).isNull();
        }
    }

    @Nested
    @DisplayName("BusinessException(ErrorCode) 构造器")
    class ErrorCodeConstructorTests {

        @Test
        @DisplayName("应使用指定错误码和默认消息")
        void shouldUseGivenErrorCodeAndDefaultMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.NOT_FOUND);

            assertThat(ex.getMessage()).isEqualTo("资源不存在");
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(ex.isCustomMessage()).isFalse();
            assertThat(ex.getArgs()).isNull();
        }
    }

    @Nested
    @DisplayName("BusinessException(ErrorCode, String message) 构造器")
    class ErrorCodeMessageConstructorTests {

        @Test
        @DisplayName("应使用指定错误码和自定义消息")
        void shouldUseGivenErrorCodeAndCustomMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR, "用户名不能为空");

            assertThat(ex.getMessage()).isEqualTo("用户名不能为空");
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.PARAM_ERROR);
            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(ex.isCustomMessage()).isTrue();
        }
    }

    @Nested
    @DisplayName("BusinessException(ErrorCode, String message, Throwable cause) 构造器")
    class ErrorCodeMessageCauseConstructorTests {

        @Test
        @DisplayName("应保留原始异常原因")
        void shouldRetainCause() {
            RuntimeException cause = new RuntimeException("数据库连接失败");
            BusinessException ex = new BusinessException(CommonErrorCode.SYSTEM_ERROR, "系统异常", cause);

            assertThat(ex.getMessage()).isEqualTo("系统异常");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.SYSTEM_ERROR);
            assertThat(ex.isCustomMessage()).isTrue();
        }
    }

    @Nested
    @DisplayName("BusinessException(ErrorCode, Object[] args) 构造器")
    class ErrorCodeArgsConstructorTests {

        @Test
        @DisplayName("应保留消息参数")
        void shouldRetainArgs() {
            Object[] args = {"用户A", 3};
            BusinessException ex = new BusinessException(CommonErrorCode.FAIL, args);

            assertThat(ex.getArgs()).isSameAs(args);
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.FAIL);
            assertThat(ex.isCustomMessage()).isFalse();
        }
    }

    @Nested
    @DisplayName("BusinessException(ErrorCode, Object[] args, Throwable cause) 构造器")
    class ErrorCodeArgsCauseConstructorTests {

        @Test
        @DisplayName("应保留消息参数和原因")
        void shouldRetainArgsAndCause() {
            Object[] args = {"test"};
            Throwable cause = new IllegalStateException("连接超时");
            BusinessException ex = new BusinessException(CommonErrorCode.CLIENT_TIMEOUT, args, cause);

            assertThat(ex.getArgs()).isSameAs(args);
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.CLIENT_TIMEOUT);
            assertThat(ex.isCustomMessage()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCode() 方法")
    class GetCodeTests {

        @Test
        @DisplayName("应返回错误码的数字编码")
        void shouldReturnErrorCodeValue() {
            BusinessException ex = new BusinessException(CommonErrorCode.UNAUTHORIZED);

            assertThat(ex.getCode()).isEqualTo(10400);
        }
    }

    @Nested
    @DisplayName("getMessage() 方法")
    class GetMessageTests {

        @Test
        @DisplayName("自定义消息时应返回自定义消息")
        void shouldReturnCustomMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.FAIL, "自定义消息");

            assertThat(ex.getMessage()).isEqualTo("自定义消息");
        }

        @Test
        @DisplayName("非自定义消息时应返回错误码默认消息")
        void shouldReturnErrorCodeMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.FORBIDDEN);

            assertThat(ex.getMessage()).isEqualTo("无权限访问");
        }
    }

    @Nested
    @DisplayName("getMessage(Locale) 方法")
    class GetMessageLocaleTests {

        @Test
        @DisplayName("自定义消息时 locale 参数不影响返回值")
        void shouldReturnCustomMessageRegardlessOfLocale() {
            BusinessException ex = new BusinessException(CommonErrorCode.FAIL, "自定义消息");

            assertThat(ex.getMessage(Locale.CHINA)).isEqualTo("自定义消息");
            assertThat(ex.getMessage(Locale.US)).isEqualTo("自定义消息");
        }

        @Test
        @DisplayName("非自定义消息时应委托给 ErrorCode.getMessage(args, locale)")
        void shouldDelegateToErrorCodeForNonCustomMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.NOT_FOUND);

            String messageWithNullLocale = ex.getMessage(null);
            assertThat(messageWithNullLocale).isEqualTo("资源不存在");
        }

        @Test
        @DisplayName("非自定义消息时带 Locale 参数应返回消息")
        void shouldReturnMessageWithLocale() {
            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR);

            assertThat(ex.getMessage(Locale.CHINA)).isEqualTo("参数错误");
        }

        @Test
        @DisplayName("带参数的非自定义消息应返回消息")
        void shouldReturnMessageWithArgs() {
            Object[] args = {"user1"};
            BusinessException ex = new BusinessException(CommonErrorCode.FAIL, args);

            // 当前 getMessage(args, locale) 默认返回 getMessage()，无模板替换
            assertThat(ex.getMessage(Locale.CHINA)).isNotNull();
        }
    }

    @Nested
    @DisplayName("继承关系")
    class InheritanceTests {

        @Test
        @DisplayName("BusinessException 应继承 AfgException")
        void shouldExtendAfgException() {
            BusinessException ex = new BusinessException("test");

            assertThat(ex).isInstanceOf(AfgException.class);
            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("formatCode() 应返回 E 前缀的错误码")
        void shouldReturnFormattedCode() {
            BusinessException ex = new BusinessException(CommonErrorCode.UNAUTHORIZED);

            assertThat(ex.formatCode()).isEqualTo("E10400");
        }
    }
}
