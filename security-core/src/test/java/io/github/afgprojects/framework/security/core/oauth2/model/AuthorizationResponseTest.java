package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthorizationResponse 测试
 */
@DisplayName("AuthorizationResponse 测试")
class AuthorizationResponseTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("success 应创建成功响应")
        void shouldCreateSuccessResponse() {
            AuthorizationResponse response = AuthorizationResponse.success(
                    "code-123", "state-abc", "https://app.example.com/callback"
            );

            assertThat(response.code()).isEqualTo("code-123");
            assertThat(response.state()).isEqualTo("state-abc");
            assertThat(response.redirectUri()).isEqualTo("https://app.example.com/callback");
            assertThat(response.error()).isNull();
            assertThat(response.errorDescription()).isNull();
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("error 应创建错误响应")
        void shouldCreateErrorResponse() {
            AuthorizationResponse response = AuthorizationResponse.error(
                    "access_denied", "User denied access", "state-abc", "https://app.example.com/callback"
            );

            assertThat(response.code()).isNull();
            assertThat(response.state()).isEqualTo("state-abc");
            assertThat(response.redirectUri()).isEqualTo("https://app.example.com/callback");
            assertThat(response.error()).isEqualTo("access_denied");
            assertThat(response.errorDescription()).isEqualTo("User denied access");
            assertThat(response.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("isSuccess 判断")
    class IsSuccessTests {

        @Test
        @DisplayName("有 code 无 error 应为成功")
        void shouldBeSuccessWhenCodePresentAndNoError() {
            AuthorizationResponse response = AuthorizationResponse.success(
                    "code-123", null, "https://app.example.com/callback"
            );

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("无 code 有 error 应为失败")
        void shouldBeFailureWhenNoCodeAndErrorPresent() {
            AuthorizationResponse response = AuthorizationResponse.error(
                    "access_denied", null, null, "https://app.example.com/callback"
            );

            assertThat(response.isSuccess()).isFalse();
        }
    }
}
