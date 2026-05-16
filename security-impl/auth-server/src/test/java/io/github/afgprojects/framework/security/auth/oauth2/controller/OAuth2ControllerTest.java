package io.github.afgprojects.framework.security.auth.oauth2.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.afgprojects.framework.security.core.oauth2.AccessTokenInfo;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2Exception;
import io.github.afgprojects.framework.security.core.oauth2.model.AuthorizationResponse;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.core.oauth2.model.TokenResponse;

/**
 * OAuth2 控制器测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OAuth2ControllerTest {

    @Mock
    private OAuth2AuthorizationService authorizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OAuth2AuthorizeController authorizeController = new OAuth2AuthorizeController(authorizationService);
        OAuth2TokenController tokenController = new OAuth2TokenController(authorizationService);
        mockMvc = MockMvcBuilders.standaloneSetup(authorizeController, tokenController).build();
    }

    @Nested
    @DisplayName("授权端点测试")
    class AuthorizeEndpointTests {

        @Test
        @DisplayName("应处理授权请求并重定向")
        void shouldHandleAuthorizeRequest() throws Exception {
            // Given
            AuthorizationResponse response = AuthorizationResponse.success(
                    "auth-code-123", "state-123", "https://example.com/callback");
            when(authorizationService.authorize(any(), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/oauth2/authorize")
                    .param("responseType", "code")
                    .param("clientId", "client-001")
                    .param("redirectUri", "https://example.com/callback")
                    .param("scope", "read write")
                    .param("state", "state-123")
                    .param("userId", "user-001"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("https://example.com/callback?code=auth-code-123&state=state-123"));
        }

        @Test
        @DisplayName("错误响应应包含错误信息")
        void shouldIncludeErrorInRedirect() throws Exception {
            // Given
            AuthorizationResponse response = AuthorizationResponse.error(
                    "invalid_client", "客户端不存在", "state-123", "https://example.com/callback");
            when(authorizationService.authorize(any(), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/oauth2/authorize")
                    .param("responseType", "code")
                    .param("clientId", "unknown")
                    .param("redirectUri", "https://example.com/callback")
                    .param("userId", "user-001"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(result -> {
                        String url = result.getResponse().getRedirectedUrl();
                        assertThat(url).contains("error=invalid_client");
                    });
        }
    }

    @Nested
    @DisplayName("令牌端点测试")
    class TokenEndpointTests {

        @Test
        @DisplayName("应颁发令牌")
        void shouldIssueToken() throws Exception {
            // Given
            TokenResponse response = TokenResponse.of(
                    "access-token-123", 3600L, "refresh-token-456", "read write");
            when(authorizationService.issueToken(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/oauth2/token")
                    .contentType("application/json")
                    .content("""
                        {
                            "grantType": "authorization_code",
                            "code": "auth-code-123",
                            "redirectUri": "https://example.com/callback",
                            "clientId": "client-001",
                            "clientSecret": "secret-123"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        @DisplayName("OAuth2 异常应返回错误响应")
        void shouldReturnErrorResponseForOAuth2Exception() throws Exception {
            // Given
            when(authorizationService.issueToken(any()))
                    .thenThrow(OAuth2Exception.invalidClient("客户端不存在"));

            // When/Then
            mockMvc.perform(post("/oauth2/token")
                    .contentType("application/json")
                    .content("""
                        {
                            "grantType": "authorization_code",
                            "clientId": "unknown"
                        }
                        """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }
    }

    @Nested
    @DisplayName("令牌撤销端点测试")
    class RevokeEndpointTests {

        @Test
        @DisplayName("应撤销令牌")
        void shouldRevokeToken() throws Exception {
            // Given
            doNothing().when(authorizationService).revokeToken(any(), any());

            // When/Then
            mockMvc.perform(post("/oauth2/revoke")
                    .contentType("application/json")
                    .content("""
                        {
                            "token": "token-to-revoke",
                            "tokenTypeHint": "access_token"
                        }
                        """))
                    .andExpect(status().isOk());

            verify(authorizationService).revokeToken("token-to-revoke", "access_token");
        }
    }

    @Nested
    @DisplayName("令牌自省端点测试")
    class IntrospectEndpointTests {

        @Test
        @DisplayName("应返回有效令牌信息")
        void shouldReturnActiveTokenInfo() throws Exception {
            // Given
            AccessTokenInfo info = new AccessTokenInfo(
                    "user-001", "admin", "client-001",
                    Set.of("read", "write"), "tenant-001",
                    java.time.Instant.now().plusSeconds(3600),
                    java.time.Instant.now());
            when(authorizationService.validateToken("valid-token")).thenReturn(info);

            // When/Then
            mockMvc.perform(post("/oauth2/introspect")
                    .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.sub").value("user-001"))
                    .andExpect(jsonPath("$.username").value("admin"));
        }

        @Test
        @DisplayName("无效令牌应返回 inactive")
        void shouldReturnInactiveForInvalidToken() throws Exception {
            // Given
            when(authorizationService.validateToken("invalid-token")).thenReturn(null);

            // When/Then
            mockMvc.perform(post("/oauth2/introspect")
                    .param("token", "invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }
    }

    // 使用 AssertJ 的静态导入
}