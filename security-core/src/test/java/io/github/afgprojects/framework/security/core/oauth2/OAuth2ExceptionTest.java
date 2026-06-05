package io.github.afgprojects.framework.security.core.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OAuth2Exception 测试
 */
@DisplayName("OAuth2Exception 测试")
class OAuth2ExceptionTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("应正确创建异常")
        void shouldCreateException() {
            OAuth2Exception exception = new OAuth2Exception("invalid_request", "Missing parameter");

            assertThat(exception.getErrorCode()).isEqualTo("invalid_request");
            assertThat(exception.getErrorDescription()).isEqualTo("Missing parameter");
            assertThat(exception.getMessage()).isEqualTo("invalid_request: Missing parameter");
        }

        @Test
        @DisplayName("带原因的异常应正确创建")
        void shouldCreateExceptionWithCause() {
            Throwable cause = new RuntimeException("Original error");
            OAuth2Exception exception = new OAuth2Exception("server_error", "Internal error", cause);

            assertThat(exception.getErrorCode()).isEqualTo("server_error");
            assertThat(exception.getErrorDescription()).isEqualTo("Internal error");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("invalidClient 应创建 invalid_client 错误")
        void shouldCreateInvalidClientError() {
            OAuth2Exception exception = OAuth2Exception.invalidClient("Client not found");

            assertThat(exception.getErrorCode()).isEqualTo("invalid_client");
            assertThat(exception.getErrorDescription()).isEqualTo("Client not found");
        }

        @Test
        @DisplayName("invalidGrant 应创建 invalid_grant 错误")
        void shouldCreateInvalidGrantError() {
            OAuth2Exception exception = OAuth2Exception.invalidGrant("Invalid authorization code");

            assertThat(exception.getErrorCode()).isEqualTo("invalid_grant");
            assertThat(exception.getErrorDescription()).isEqualTo("Invalid authorization code");
        }

        @Test
        @DisplayName("invalidRequest 应创建 invalid_request 错误")
        void shouldCreateInvalidRequestError() {
            OAuth2Exception exception = OAuth2Exception.invalidRequest("Missing redirect_uri");

            assertThat(exception.getErrorCode()).isEqualTo("invalid_request");
            assertThat(exception.getErrorDescription()).isEqualTo("Missing redirect_uri");
        }

        @Test
        @DisplayName("unsupportedGrantType 应创建 unsupported_grant_type 错误")
        void shouldCreateUnsupportedGrantTypeError() {
            OAuth2Exception exception = OAuth2Exception.unsupportedGrantType("Grant type not supported");

            assertThat(exception.getErrorCode()).isEqualTo("unsupported_grant_type");
            assertThat(exception.getErrorDescription()).isEqualTo("Grant type not supported");
        }

        @Test
        @DisplayName("unsupportedResponseType 应创建 unsupported_response_type 错误")
        void shouldCreateUnsupportedResponseTypeError() {
            OAuth2Exception exception = OAuth2Exception.unsupportedResponseType("Response type not supported");

            assertThat(exception.getErrorCode()).isEqualTo("unsupported_response_type");
            assertThat(exception.getErrorDescription()).isEqualTo("Response type not supported");
        }

        @Test
        @DisplayName("unauthorizedClient 应创建 unauthorized_client 错误")
        void shouldCreateUnauthorizedClientError() {
            OAuth2Exception exception = OAuth2Exception.unauthorizedClient("Client not authorized");

            assertThat(exception.getErrorCode()).isEqualTo("unauthorized_client");
            assertThat(exception.getErrorDescription()).isEqualTo("Client not authorized");
        }

        @Test
        @DisplayName("accessDenied 应创建 access_denied 错误")
        void shouldCreateAccessDeniedError() {
            OAuth2Exception exception = OAuth2Exception.accessDenied("User denied access");

            assertThat(exception.getErrorCode()).isEqualTo("access_denied");
            assertThat(exception.getErrorDescription()).isEqualTo("User denied access");
        }

        @Test
        @DisplayName("serverError 应创建 server_error 错误")
        void shouldCreateServerError() {
            OAuth2Exception exception = OAuth2Exception.serverError("Internal server error");

            assertThat(exception.getErrorCode()).isEqualTo("server_error");
            assertThat(exception.getErrorDescription()).isEqualTo("Internal server error");
        }
    }

    @Nested
    @DisplayName("异常行为")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("异常应可被抛出和捕获")
        void shouldBeThrowable() {
            assertThatThrownBy(() -> {
                throw OAuth2Exception.invalidClient("Test error");
            })
                    .isInstanceOf(OAuth2Exception.class)
                    .hasMessageContaining("invalid_client");
        }
    }
}
