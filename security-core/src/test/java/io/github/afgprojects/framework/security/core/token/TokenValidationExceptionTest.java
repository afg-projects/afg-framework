package io.github.afgprojects.framework.security.core.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TokenValidationException 测试
 */
@DisplayName("TokenValidationException 测试")
class TokenValidationExceptionTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("带消息应正确创建")
        void shouldCreateWithMessage() {
            TokenValidationException exception = new TokenValidationException("Token expired");

            assertThat(exception.getMessage()).isEqualTo("Token expired");
        }

        @Test
        @DisplayName("带消息和原因应正确创建")
        void shouldCreateWithMessageAndCause() {
            Throwable cause = new RuntimeException("Signature error");
            TokenValidationException exception = new TokenValidationException("Invalid token", cause);

            assertThat(exception.getMessage()).isEqualTo("Invalid token");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("expired 应创建过期异常")
        void shouldCreateExpiredException() {
            TokenValidationException exception = TokenValidationException.expired();

            assertThat(exception.getMessage()).isEqualTo("Token has expired");
        }

        @Test
        @DisplayName("invalid 应创建无效异常")
        void shouldCreateInvalidException() {
            TokenValidationException exception = TokenValidationException.invalid();

            assertThat(exception.getMessage()).isEqualTo("Token is invalid");
        }

        @Test
        @DisplayName("invalidSignature 应创建签名无效异常")
        void shouldCreateInvalidSignatureException() {
            TokenValidationException exception = TokenValidationException.invalidSignature();

            assertThat(exception.getMessage()).isEqualTo("Token signature is invalid");
        }

        @Test
        @DisplayName("revoked 应创建已撤销异常")
        void shouldCreateRevokedException() {
            TokenValidationException exception = TokenValidationException.revoked();

            assertThat(exception.getMessage()).isEqualTo("Token has been revoked");
        }
    }

    @Nested
    @DisplayName("异常行为")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("应可被抛出和捕获")
        void shouldBeThrowableAndCatchable() {
            assertThatThrownBy(() -> {
                throw TokenValidationException.expired();
            })
                    .isInstanceOf(TokenValidationException.class)
                    .hasMessage("Token has expired");
        }

        @Test
        @DisplayName("应可被 RuntimeException 捕获")
        void shouldBeCatchableAsRuntimeException() {
            assertThatThrownBy(() -> {
                throw TokenValidationException.invalid();
            })
                    .isInstanceOf(RuntimeException.class);
        }
    }
}