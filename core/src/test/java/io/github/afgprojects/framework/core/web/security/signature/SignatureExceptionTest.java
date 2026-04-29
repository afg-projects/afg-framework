package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SignatureException 测试
 */
@DisplayName("SignatureException 测试")
class SignatureExceptionTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用错误类型创建异常")
        void shouldCreateWithErrorType() {
            // when
            SignatureException exception = new SignatureException(SignatureException.SignatureErrorType.MISSING_SIGNATURE);

            // then
            assertThat(exception.getErrorType()).isEqualTo(SignatureException.SignatureErrorType.MISSING_SIGNATURE);
            assertThat(exception.getMessage()).contains("缺少签名头");
        }

        @Test
        @DisplayName("应该使用错误类型和消息创建异常")
        void shouldCreateWithErrorTypeAndMessage() {
            // when
            SignatureException exception = new SignatureException(
                    SignatureException.SignatureErrorType.INVALID_SIGNATURE,
                    "自定义错误消息");

            // then
            assertThat(exception.getErrorType()).isEqualTo(SignatureException.SignatureErrorType.INVALID_SIGNATURE);
            assertThat(exception.getMessage()).isEqualTo("自定义错误消息");
        }
    }

    @Nested
    @DisplayName("错误类型测试")
    class ErrorTypeTests {

        @Test
        @DisplayName("所有错误类型应该有消息")
        void allErrorTypesShouldHaveMessage() {
            for (SignatureException.SignatureErrorType errorType : SignatureException.SignatureErrorType.values()) {
                assertThat(errorType.getMessage()).isNotEmpty();
            }
        }
    }
}
