package io.github.afgprojects.framework.security.auth.oauth2;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.oauth2.AccessTokenInfo;
import io.github.afgprojects.framework.security.core.oauth2.AuthorizationCodeStorage;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2Exception;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationCode;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenRequest;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;

/**
 * DefaultOAuth2AuthorizationService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultOAuth2AuthorizationServiceTest {

    @Mock
    private OAuth2ClientService clientService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthorizationCodeStorage codeStorage;

    private DefaultOAuth2AuthorizationService authService;

    @BeforeEach
    void setUp() {
        authService = new DefaultOAuth2AuthorizationService(clientService, tokenService, null);
    }

    @Nested
    @DisplayName("授权请求测试")
    class AuthorizeTests {

        @Test
        @DisplayName("应成功处理授权请求")
        void shouldProcessAuthorizationRequest() {
            // Given
            ClientDetails client = createConfidentialClient();
            when(clientService.loadClientByClientId("client-001")).thenReturn(client);

            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "client-001", "https://example.com/callback",
                    Set.of("read"), "state-123", null, null);

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.code()).isNotBlank();
            assertThat(response.state()).isEqualTo("state-123");
            assertThat(response.redirectUri()).isEqualTo("https://example.com/callback");
        }

        @Test
        @DisplayName("客户端不存在时应返回错误")
        void shouldReturnErrorWhenClientNotFound() {
            // Given
            when(clientService.loadClientByClientId("unknown")).thenReturn(null);

            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "unknown", "https://example.com/callback",
                    null, null, null, null);

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.error()).isEqualTo("invalid_client");
        }

        @Test
        @DisplayName("不支持的响应类型应返回错误")
        void shouldReturnErrorForUnsupportedResponseType() {
            // Given
            ClientDetails client = createConfidentialClient();
            when(clientService.loadClientByClientId("client-001")).thenReturn(client);

            AuthorizationRequest request = new AuthorizationRequest(
                    "token", "client-001", "https://example.com/callback",
                    null, null, null, null);

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.error()).isEqualTo("unsupported_response_type");
        }

        @Test
        @DisplayName("无效的重定向 URI 应返回错误")
        void shouldReturnErrorForInvalidRedirectUri() {
            // Given
            ClientDetails client = createConfidentialClient();
            when(clientService.loadClientByClientId("client-001")).thenReturn(client);

            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "client-001", "https://evil.com/callback",
                    null, null, null, null);

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.error()).isEqualTo("invalid_request");
        }

        @Test
        @DisplayName("需要 PKCE 的客户端未提供 PKCE 时应返回错误")
        void shouldReturnErrorWhenPkceRequiredButNotProvided() {
            // Given
            ClientDetails client = createPublicClient(true);
            when(clientService.loadClientByClientId("client-002")).thenReturn(client);

            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "client-002", "https://example.com/callback",
                    null, null, null, null);

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.error()).isEqualTo("invalid_request");
        }

        @Test
        @DisplayName("应支持 PKCE 授权请求")
        void shouldSupportPkceAuthorizationRequest() {
            // Given
            ClientDetails client = createPublicClient(true);
            when(clientService.loadClientByClientId("client-002")).thenReturn(client);

            AuthorizationRequest request = new AuthorizationRequest(
                    "code", "client-002", "https://example.com/callback",
                    Set.of("read"), "state-123", "challenge-abc", "S256");

            // When
            AuthorizationResponse response = authService.authorize(request, "user-001");

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.code()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("令牌颁发测试")
    class IssueTokenTests {

        @Test
        @DisplayName("客户端不存在时应抛出异常")
        void shouldThrowExceptionWhenClientNotFound() {
            // Given
            when(clientService.loadClientByClientId("unknown")).thenReturn(null);

            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code", null, null,
                    "unknown", null, null, null);

            // When/Then
            assertThatThrownBy(() -> authService.issueToken(request))
                    .isInstanceOf(OAuth2Exception.class)
                    .hasFieldOrPropertyWithValue("errorCode", "invalid_client");
        }

        @Test
        @DisplayName("机密客户端凭证无效时应抛出异常")
        void shouldThrowExceptionWhenClientCredentialsInvalid() {
            // Given
            ClientDetails client = createConfidentialClient();
            when(clientService.loadClientByClientId("client-001")).thenReturn(client);
            when(clientService.validateClientCredentials("client-001", "wrong-secret")).thenReturn(false);

            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_AUTHORIZATION_CODE, "code", null, null,
                    "client-001", "wrong-secret", null, null);

            // When/Then
            assertThatThrownBy(() -> authService.issueToken(request))
                    .isInstanceOf(OAuth2Exception.class)
                    .hasFieldOrPropertyWithValue("errorCode", "invalid_client");
        }

        @Test
        @DisplayName("不支持的授权类型应抛出异常")
        void shouldThrowExceptionForUnsupportedGrantType() {
            // Given
            ClientDetails client = new ClientDetails(
                    "client-001", "secret", "Test Client",
                    Set.of("https://example.com/callback"), Set.of("read"),
                    Set.of("authorization_code"), // 只支持 authorization_code
                    false, Duration.ofHours(1), Duration.ofDays(7));
            when(clientService.loadClientByClientId("client-001")).thenReturn(client);
            when(clientService.validateClientCredentials(eq("client-001"), any())).thenReturn(true);

            TokenRequest request = new TokenRequest(
                    TokenRequest.GRANT_CLIENT_CREDENTIALS, null, null, null,
                    "client-001", "secret", null, Set.of("read"));

            // When/Then
            assertThatThrownBy(() -> authService.issueToken(request))
                    .isInstanceOf(OAuth2Exception.class)
                    .hasFieldOrPropertyWithValue("errorCode", "unsupported_grant_type");
        }
    }

    @Nested
    @DisplayName("令牌验证测试")
    class ValidateTokenTests {

        @Test
        @DisplayName("应验证有效令牌")
        void shouldValidateValidToken() {
            // Given
            when(tokenService.validateAccessToken("valid-token")).thenReturn(true);
            when(tokenService.extractUserId("valid-token")).thenReturn("user-001");
            when(tokenService.extractUsername("valid-token")).thenReturn("admin");
            when(tokenService.extractRoles("valid-token")).thenReturn(Set.of("ADMIN"));
            when(tokenService.extractPermissions("valid-token")).thenReturn(Set.of("read", "write"));
            when(tokenService.extractTenantId("valid-token")).thenReturn("tenant-001");
            when(tokenService.getAccessTokenTtl()).thenReturn(3600L);

            // When
            AccessTokenInfo info = authService.validateToken("valid-token");

            // Then
            assertThat(info).isNotNull();
            assertThat(info.userId()).isEqualTo("user-001");
            assertThat(info.username()).isEqualTo("admin");
            assertThat(info.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("无效令牌应返回 null")
        void shouldReturnNullForInvalidToken() {
            // Given
            when(tokenService.validateAccessToken("invalid-token")).thenReturn(false);

            // When
            AccessTokenInfo info = authService.validateToken("invalid-token");

            // Then
            assertThat(info).isNull();
        }
    }

    @Nested
    @DisplayName("令牌撤销测试")
    class RevokeTokenTests {

        @Test
        @DisplayName("应撤销令牌")
        void shouldRevokeToken() {
            // Given
            doNothing().when(tokenService).invalidateToken("token-to-revoke");

            // When
            authService.revokeToken("token-to-revoke", "access_token");

            // Then
            verify(tokenService).invalidateToken("token-to-revoke");
        }
    }

    // ==================== 辅助方法 ====================

    private ClientDetails createConfidentialClient() {
        return new ClientDetails(
                "client-001",
                "secret-123",
                "Test Client",
                Set.of("https://example.com/callback"),
                Set.of("read", "write"),
                Set.of("authorization_code", "refresh_token", "client_credentials"),
                false,
                Duration.ofHours(1),
                Duration.ofDays(7));
    }

    private ClientDetails createPublicClient(boolean requirePkce) {
        return new ClientDetails(
                "client-002",
                null, // 公共客户端无密钥
                "Public Client",
                Set.of("https://example.com/callback"),
                Set.of("read"),
                Set.of("authorization_code", "refresh_token"),
                requirePkce,
                Duration.ofHours(1),
                Duration.ofDays(7));
    }
}