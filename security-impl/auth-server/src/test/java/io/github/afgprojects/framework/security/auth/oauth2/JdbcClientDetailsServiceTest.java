package io.github.afgprojects.framework.security.auth.oauth2;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import io.github.afgprojects.framework.security.auth.user.AfgClientDetails;

import java.util.Set;

/**
 * JdbcClientDetailsService 测试
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JdbcClientDetailsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcClientDetailsService clientDetailsService;

    @BeforeEach
    void setUp() {
        clientDetailsService = new JdbcClientDetailsService(jdbcTemplate);
    }

    @Nested
    @DisplayName("客户端加载测试")
    class LoadClientTests {

        @Test
        @DisplayName("应成功加载机密客户端")
        void shouldLoadConfidentialClient() {
            // Given
            String clientId = "test-client";
            AfgClientDetails expectedClient = createConfidentialClient();

            when(jdbcTemplate.queryForObject(
                    anyString(),
                    any(RowMapper.class),
                    eq(clientId)))
                    .thenReturn(expectedClient);

            // When
            AfgClientDetails loadedClient = clientDetailsService.loadClientByClientId(clientId);

            // Then
            assertThat(loadedClient).isNotNull();
            assertThat(loadedClient.getClientId()).isEqualTo(clientId);
            assertThat(loadedClient.getClientName()).isEqualTo("Test Client");
            assertThat(loadedClient.getClientSecret()).isEqualTo("test-secret");
            assertThat(loadedClient.getAuthorizationGrantTypes())
                    .containsExactlyInAnyOrder(
                            AuthorizationGrantType.AUTHORIZATION_CODE,
                            AuthorizationGrantType.REFRESH_TOKEN,
                            AuthorizationGrantType.CLIENT_CREDENTIALS);
            assertThat(loadedClient.getClientAuthenticationMethods())
                    .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }

        @Test
        @DisplayName("应成功加载公共客户端")
        void shouldLoadPublicClient() {
            // Given
            String clientId = "public-client";
            AfgClientDetails expectedClient = createPublicClient();

            when(jdbcTemplate.queryForObject(
                    anyString(),
                    any(RowMapper.class),
                    eq(clientId)))
                    .thenReturn(expectedClient);

            // When
            AfgClientDetails loadedClient = clientDetailsService.loadClientByClientId(clientId);

            // Then
            assertThat(loadedClient).isNotNull();
            assertThat(loadedClient.getClientId()).isEqualTo(clientId);
            assertThat(loadedClient.getClientSecret()).isNull();
            assertThat(loadedClient.getClientAuthenticationMethods())
                    .contains(ClientAuthenticationMethod.NONE);
        }

        @Test
        @DisplayName("不存在的客户端应返回 null")
        void shouldReturnNullForNonExistentClient() {
            // Given
            String clientId = "unknown-client";

            when(jdbcTemplate.queryForObject(
                    anyString(),
                    any(RowMapper.class),
                    eq(clientId)))
                    .thenThrow(new RuntimeException("Client not found"));

            // When
            AfgClientDetails loadedClient = clientDetailsService.loadClientByClientId(clientId);

            // Then
            assertThat(loadedClient).isNull();
        }
    }

    @Nested
    @DisplayName("客户端详情转换测试")
    class ClientDetailsConversionTests {

        @Test
        @DisplayName("应正确转换为 RegisteredClient")
        void shouldConvertToRegisteredClient() {
            // Given
            AfgClientDetails clientDetails = createConfidentialClient();

            // When
            var registeredClient = clientDetails.toRegisteredClient();

            // Then
            assertThat(registeredClient).isNotNull();
            assertThat(registeredClient.getClientId()).isEqualTo(clientDetails.getClientId());
            assertThat(registeredClient.getClientName()).isEqualTo(clientDetails.getClientName());
            assertThat(registeredClient.getRedirectUris())
                    .containsExactlyInAnyOrderElementsOf(clientDetails.getRedirectUris());
            assertThat(registeredClient.getScopes())
                    .containsExactlyInAnyOrderElementsOf(clientDetails.getScopes());
        }
    }

    // ==================== 辅助方法 ====================

    private AfgClientDetails createConfidentialClient() {
        return AfgClientDetails.builder()
                .clientId("test-client")
                .clientSecret("test-secret")
                .clientName("Test Client")
                .redirectUris(Set.of("http://localhost:8080/callback"))
                .scopes(Set.of("read", "write"))
                .authorizationGrantTypes(Set.of(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN,
                        AuthorizationGrantType.CLIENT_CREDENTIALS))
                .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                .build();
    }

    private AfgClientDetails createPublicClient() {
        return AfgClientDetails.builder()
                .clientId("public-client")
                .clientSecret(null)
                .clientName("Public Client")
                .redirectUris(Set.of("http://localhost:3000/callback"))
                .scopes(Set.of("read"))
                .authorizationGrantTypes(Set.of(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN))
                .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.NONE))
                .build();
    }
}