package io.github.afgprojects.framework.security.auth.oauth2.config;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.oauth2.OAuth2AuthorizationService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;

/**
 * OAuth2 自动配置测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class OAuth2AutoConfigurationTest {

    @Nested
    @DisplayName("OAuth2Properties 测试")
    class PropertiesTests {

        @Test
        @DisplayName("应使用默认配置")
        void shouldUseDefaultConfiguration() {
            OAuth2Properties properties = new OAuth2Properties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getAuthorizationCodeTtl()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.getIssuer()).isEqualTo("afg-auth-server");
        }

        @Test
        @DisplayName("应支持自定义配置")
        void shouldSupportCustomConfiguration() {
            OAuth2Properties properties = new OAuth2Properties();
            properties.setEnabled(false);
            properties.setAuthorizationCodeTtl(Duration.ofMinutes(10));
            properties.setAccessTokenTtl(Duration.ofHours(2));
            properties.setRefreshTokenTtl(Duration.ofDays(14));
            properties.setIssuer("custom-issuer");

            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getAuthorizationCodeTtl()).isEqualTo(Duration.ofMinutes(10));
            assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(2));
            assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(14));
            assertThat(properties.getIssuer()).isEqualTo("custom-issuer");
        }

        @Test
        @DisplayName("应支持预配置客户端")
        void shouldSupportPreconfiguredClients() {
            OAuth2Properties properties = new OAuth2Properties();

            Set<OAuth2Properties.ClientConfig> clients = Set.of(
                    new OAuth2Properties.ClientConfig(
                            "client-001",
                            "secret-123",
                            "Test Client",
                            Set.of("https://example.com/callback"),
                            Set.of("read", "write"),
                            Set.of("authorization_code", "refresh_token"),
                            false
                    )
            );

            properties.setClients(clients);

            assertThat(properties.getClients()).hasSize(1);
            assertThat(properties.getClients().iterator().next().clientId()).isEqualTo("client-001");
        }
    }

    @Nested
    @DisplayName("OAuth2AutoConfiguration 测试")
    class AutoConfigurationTests {

        @Test
        @DisplayName("应创建默认客户端服务")
        void shouldCreateDefaultClientService() {
            OAuth2Properties properties = new OAuth2Properties();
            OAuth2AutoConfiguration config = new OAuth2AutoConfiguration();

            OAuth2ClientService clientService = config.oAuth2ClientService(properties);

            assertThat(clientService).isNotNull();
        }

        @Test
        @DisplayName("应从配置加载客户端")
        void shouldLoadClientsFromConfiguration() {
            OAuth2Properties properties = new OAuth2Properties();
            properties.setClients(Set.of(
                    new OAuth2Properties.ClientConfig(
                            "client-001",
                            "secret-123",
                            "Test Client",
                            Set.of("https://example.com/callback"),
                            Set.of("read", "write"),
                            Set.of("authorization_code", "refresh_token"),
                            false
                    )
            ));

            OAuth2AutoConfiguration config = new OAuth2AutoConfiguration();
            OAuth2ClientService clientService = config.oAuth2ClientService(properties);

            ClientDetails client = clientService.loadClientByClientId("client-001");

            assertThat(client).isNotNull();
            assertThat(client.clientId()).isEqualTo("client-001");
            assertThat(client.clientName()).isEqualTo("Test Client");
            assertThat(client.redirectUris()).contains("https://example.com/callback");
        }

        @Test
        @DisplayName("应创建授权码存储")
        void shouldCreateAuthorizationCodeStorage() {
            OAuth2AutoConfiguration config = new OAuth2AutoConfiguration();

            var storage = config.authorizationCodeStorage();

            assertThat(storage).isNotNull();
        }
    }
}