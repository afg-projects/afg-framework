package io.github.afgprojects.framework.security.core.oauth2.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OAuth2 模型测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class OAuth2ModelTest {

    @Nested
    @DisplayName("AuthorizationRequest 测试")
    class AuthorizationRequestTest {

        @Test
        @DisplayName("应正确创建授权请求")
        void shouldCreateAuthorizationRequest() {
            Set<String> scopes = Set.of("read", "write");
            AuthorizationRequest request = new AuthorizationRequest(
                    "code",
                    "client-001",
                    "https://example.com/callback",
                    scopes,
                    "state-123",
                    "challenge-abc",
                    "S256"
            );

            assertThat(request.responseType()).isEqualTo("code");
            assertThat(request.clientId()).isEqualTo("client-001");
            assertThat(request.redirectUri()).isEqualTo("https://example.com/callback");
            assertThat(request.scope()).containsExactlyInAnyOrder("read", "write");
            assertThat(request.state()).isEqualTo("state-123");
            assertThat(request.codeChallenge()).isEqualTo("challenge-abc");
            assertThat(request.codeChallengeMethod()).isEqualTo("S256");
        }

        @Test
        @DisplayName("应正确判断是否使用 PKCE")
        void shouldDetectPkce() {
            AuthorizationRequest withPkce = new AuthorizationRequest(
                    "code", "client-001", "https://example.com/callback",
                    null, null, "challenge", "S256"
            );
            AuthorizationRequest withoutPkce = new AuthorizationRequest(
                    "code", "client-001", "https://example.com/callback",
                    null, null, null, null
            );

            assertThat(withPkce.isPkce()).isTrue();
            assertThat(withoutPkce.isPkce()).isFalse();
        }
    }

    @Nested
    @DisplayName("AuthorizationResponse 测试")
    class AuthorizationResponseTest {

        @Test
        @DisplayName("应创建成功响应")
        void shouldCreateSuccessResponse() {
            AuthorizationResponse response = AuthorizationResponse.success(
                    "auth-code-123",
                    "state-123",
                    "https://example.com/callback"
            );

            assertThat(response.code()).isEqualTo("auth-code-123");
            assertThat(response.state()).isEqualTo("state-123");
            assertThat(response.redirectUri()).isEqualTo("https://example.com/callback");
            assertThat(response.error()).isNull();
            assertThat(response.errorDescription()).isNull();
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("应创建错误响应")
        void shouldCreateErrorResponse() {
            AuthorizationResponse response = AuthorizationResponse.error(
                    "invalid_request",
                    "Missing required parameter",
                    "state-123",
                    "https://example.com/callback"
            );

            assertThat(response.code()).isNull();
            assertThat(response.error()).isEqualTo("invalid_request");
            assertThat(response.errorDescription()).isEqualTo("Missing required parameter");
            assertThat(response.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("TokenRequest 测试")
    class TokenRequestTest {

        @Test
        @DisplayName("应正确创建令牌请求")
        void shouldCreateTokenRequest() {
            Set<String> scopes = Set.of("read");
            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE,
                    "auth-code-123",
                    null,
                    "https://example.com/callback",
                    "client-001",
                    "secret-123",
                    "verifier-xyz",
                    scopes
            );

            assertThat(request.grantType()).isEqualTo(TokenRequest.GRANT_AUTHORIZATION_CODE);
            assertThat(request.code()).isEqualTo("auth-code-123");
            assertThat(request.refreshToken()).isNull();
            assertThat(request.redirectUri()).isEqualTo("https://example.com/callback");
            assertThat(request.clientId()).isEqualTo("client-001");
            assertThat(request.clientSecret()).isEqualTo("secret-123");
            assertThat(request.codeVerifier()).isEqualTo("verifier-xyz");
        }

        @Test
        @DisplayName("应正确判断授权类型")
        void shouldDetectGrantType() {
            TokenRequest authCodeRequest = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code", null, null, "client", null, null, null
            );
            TokenRequest refreshTokenRequest = new TokenRequest(
                    TokenRequest.GRANT_REFRESH_TOKEN, null, "refresh-token", null, "client", null, null, null
            );
            TokenRequest clientCredentialsRequest = new TokenRequest(
                    TokenRequest.GRANT_CLIENT_CREDENTIALS, null, null, null, "client", "secret", null, null
            );

            assertThat(authCodeRequest.isAuthorizationCodeGrant()).isTrue();
            assertThat(authCodeRequest.isRefreshTokenGrant()).isFalse();
            assertThat(refreshTokenRequest.isRefreshTokenGrant()).isTrue();
            assertThat(clientCredentialsRequest.isClientCredentialsGrant()).isTrue();
        }
    }

    @Nested
    @DisplayName("TokenResponse 测试")
    class TokenResponseTest {

        @Test
        @DisplayName("应创建令牌响应")
        void shouldCreateTokenResponse() {
            TokenResponse response = TokenResponse.of(
                    "access-token-123",
                    3600L,
                    "refresh-token-456",
                    "read write"
            );

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.scope()).isEqualTo("read write");
        }

        @Test
        @DisplayName("应创建只有访问令牌的响应")
        void shouldCreateAccessTokenOnlyResponse() {
            TokenResponse response = TokenResponse.ofAccessToken("access-token-123", 3600L);

            assertThat(response.accessToken()).isEqualTo("access-token-123");
            assertThat(response.refreshToken()).isNull();
            assertThat(response.scope()).isNull();
        }
    }

    @Nested
    @DisplayName("ClientDetails 测试")
    class ClientDetailsTest {

        @Test
        @DisplayName("应正确创建客户端详情")
        void shouldCreateClientDetails() {
            Set<String> redirectUris = Set.of("https://example.com/callback");
            Set<String> scopes = Set.of("read", "write");
            Set<String> grantTypes = Set.of("authorization_code", "refresh_token");

            ClientDetails client = new ClientDetails(
                    "client-001",
                    "secret-123",
                    "Test Client",
                    redirectUris,
                    scopes,
                    grantTypes,
                    true,
                    java.time.Duration.ofHours(1),
                    java.time.Duration.ofDays(7)
            );

            assertThat(client.clientId()).isEqualTo("client-001");
            assertThat(client.clientSecret()).isEqualTo("secret-123");
            assertThat(client.clientName()).isEqualTo("Test Client");
            assertThat(client.redirectUris()).containsExactlyInAnyOrder("https://example.com/callback");
            assertThat(client.scopes()).containsExactlyInAnyOrder("read", "write");
            assertThat(client.grantTypes()).containsExactlyInAnyOrder("authorization_code", "refresh_token");
            assertThat(client.requirePkce()).isTrue();
        }

        @Test
        @DisplayName("应正确判断客户端类型")
        void shouldDetectClientType() {
            ClientDetails confidential = new ClientDetails(
                    "client-001", "secret", "Confidential Client",
                    Set.of("https://example.com/callback"), Set.of("read"), Set.of("authorization_code"),
                    false, java.time.Duration.ofHours(1), java.time.Duration.ofDays(7)
            );
            ClientDetails publik = new ClientDetails(
                    "client-002", null, "Public Client",
                    Set.of("https://example.com/callback"), Set.of("read"), Set.of("authorization_code"),
                    true, java.time.Duration.ofHours(1), java.time.Duration.ofDays(7)
            );

            assertThat(confidential.getClientType()).isEqualTo(ClientDetails.ClientType.CONFIDENTIAL);
            assertThat(publik.getClientType()).isEqualTo(ClientDetails.ClientType.PUBLIC);
        }

        @Test
        @DisplayName("应正确检查重定向 URI")
        void shouldCheckRedirectUri() {
            ClientDetails client = new ClientDetails(
                    "client-001", "secret", "Test Client",
                    Set.of("https://example.com/callback", "https://example.com/oauth/callback"),
                    Set.of("read"), Set.of("authorization_code"),
                    false, java.time.Duration.ofHours(1), java.time.Duration.ofDays(7)
            );

            assertThat(client.isRedirectUriAllowed("https://example.com/callback")).isTrue();
            assertThat(client.isRedirectUriAllowed("https://evil.com/callback")).isFalse();
        }

        @Test
        @DisplayName("应正确检查授权类型")
        void shouldCheckGrantType() {
            ClientDetails client = new ClientDetails(
                    "client-001", "secret", "Test Client",
                    Set.of("https://example.com/callback"), Set.of("read"),
                    Set.of("authorization_code", "refresh_token"),
                    false, java.time.Duration.ofHours(1), java.time.Duration.ofDays(7)
            );

            assertThat(client.isGrantTypeAllowed("authorization_code")).isTrue();
            assertThat(client.isGrantTypeAllowed("client_credentials")).isFalse();
        }

        @Test
        @DisplayName("应正确检查权限范围")
        void shouldCheckScope() {
            ClientDetails client = new ClientDetails(
                    "client-001", "secret", "Test Client",
                    Set.of("https://example.com/callback"),
                    Set.of("read", "write", "admin"),
                    Set.of("authorization_code"),
                    false, java.time.Duration.ofHours(1), java.time.Duration.ofDays(7)
            );

            assertThat(client.isScopeAllowed(Set.of("read", "write"))).isTrue();
            assertThat(client.isScopeAllowed(Set.of("read", "delete"))).isFalse();
        }
    }

    @Nested
    @DisplayName("AuthorizationCode 测试")
    class AuthorizationCodeTest {

        @Test
        @DisplayName("应正确创建授权码")
        void shouldCreateAuthorizationCode() {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(300);
            Set<String> scopes = Set.of("read", "write");

            AuthorizationCode authCode = new AuthorizationCode(
                    "code-123",
                    "client-001",
                    "user-001",
                    "https://example.com/callback",
                    scopes,
                    "challenge-abc",
                    "S256",
                    expiresAt,
                    now
            );

            assertThat(authCode.code()).isEqualTo("code-123");
            assertThat(authCode.clientId()).isEqualTo("client-001");
            assertThat(authCode.userId()).isEqualTo("user-001");
            assertThat(authCode.redirectUri()).isEqualTo("https://example.com/callback");
            assertThat(authCode.scopes()).containsExactlyInAnyOrder("read", "write");
            assertThat(authCode.codeChallenge()).isEqualTo("challenge-abc");
            assertThat(authCode.codeChallengeMethod()).isEqualTo("S256");
            assertThat(authCode.expiresAt()).isEqualTo(expiresAt);
            assertThat(authCode.createdAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("应正确判断是否使用 PKCE")
        void shouldDetectPkce() {
            Instant now = Instant.now();
            AuthorizationCode withPkce = new AuthorizationCode(
                    "code-123", "client-001", "user-001", "https://example.com/callback",
                    null, "challenge", "S256", now.plusSeconds(300), now
            );
            AuthorizationCode withoutPkce = new AuthorizationCode(
                    "code-456", "client-001", "user-001", "https://example.com/callback",
                    null, null, null, now.plusSeconds(300), now
            );

            assertThat(withPkce.isPkce()).isTrue();
            assertThat(withoutPkce.isPkce()).isFalse();
        }

        @Test
        @DisplayName("应正确判断是否过期")
        void shouldDetectExpiration() {
            Instant now = Instant.now();
            AuthorizationCode valid = new AuthorizationCode(
                    "code-123", "client-001", "user-001", "https://example.com/callback",
                    null, null, null, now.plusSeconds(300), now
            );
            AuthorizationCode expired = new AuthorizationCode(
                    "code-456", "client-001", "user-001", "https://example.com/callback",
                    null, null, null, now.minusSeconds(1), now
            );

            assertThat(valid.isExpired()).isFalse();
            assertThat(valid.isValid()).isTrue();
            assertThat(expired.isExpired()).isTrue();
            assertThat(expired.isValid()).isFalse();
        }
    }
}
