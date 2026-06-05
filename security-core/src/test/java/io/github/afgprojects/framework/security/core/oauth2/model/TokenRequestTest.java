package io.github.afgprojects.framework.security.core.oauth2.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenRequest 测试
 */
@DisplayName("TokenRequest 测试")
class TokenRequestTest {

    @Nested
    @DisplayName("授权类型判断")
    class GrantTypeTests {

        @Test
        @DisplayName("authorization_code 模式应正确识别")
        void shouldIdentifyAuthorizationCodeGrant() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code-123", null,
                    "https://app.example.com/callback", "my-client", "my-secret", null, null
            );

            assertThat(request.isAuthorizationCodeGrant()).isTrue();
            assertThat(request.isRefreshTokenGrant()).isFalse();
            assertThat(request.isClientCredentialsGrant()).isFalse();
        }

        @Test
        @DisplayName("refresh_token 模式应正确识别")
        void shouldIdentifyRefreshTokenGrant() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_REFRESH_TOKEN, null, "refresh-token-123",
                    null, "my-client", "my-secret", null, null
            );

            assertThat(request.isRefreshTokenGrant()).isTrue();
            assertThat(request.isAuthorizationCodeGrant()).isFalse();
            assertThat(request.isClientCredentialsGrant()).isFalse();
        }

        @Test
        @DisplayName("client_credentials 模式应正确识别")
        void shouldIdentifyClientCredentialsGrant() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_CLIENT_CREDENTIALS, null, null,
                    null, "my-client", "my-secret", null, Set.of("read")
            );

            assertThat(request.isClientCredentialsGrant()).isTrue();
            assertThat(request.isAuthorizationCodeGrant()).isFalse();
            assertThat(request.isRefreshTokenGrant()).isFalse();
        }
    }

    @Nested
    @DisplayName("PKCE 判断")
    class PkceTests {

        @Test
        @DisplayName("有 codeVerifier 应识别为 PKCE")
        void shouldIdentifyPkceWhenCodeVerifierPresent() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code-123", null,
                    "https://app.example.com/callback", "my-client", "my-secret", "code-verifier-123", null
            );

            assertThat(request.isPkce()).isTrue();
        }

        @Test
        @DisplayName("无 codeVerifier 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeVerifierAbsent() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code-123", null,
                    "https://app.example.com/callback", "my-client", "my-secret", null, null
            );

            assertThat(request.isPkce()).isFalse();
        }

        @Test
        @DisplayName("空 codeVerifier 应识别为非 PKCE")
        void shouldIdentifyNonPkceWhenCodeVerifierEmpty() {
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code-123", null,
                    "https://app.example.com/callback", "my-client", "my-secret", "", null
            );

            assertThat(request.isPkce()).isFalse();
        }
    }

    @Nested
    @DisplayName("授权类型常量")
    class GrantTypeConstantTests {

        @Test
        @DisplayName("授权类型常量应正确")
        void shouldHaveCorrectGrantTypeConstants() {
            assertThat(TokenRequest.GRANT_AUTHORIZATION_CODE).isEqualTo("authorization_code");
            assertThat(TokenRequest.GRANT_REFRESH_TOKEN).isEqualTo("refresh_token");
            assertThat(TokenRequest.GRANT_CLIENT_CREDENTIALS).isEqualTo("client_credentials");
        }
    }
}
