package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    @DisplayName("AuthSecurityProperties.OAuth2Config 测试")
    class PropertiesTests {

        @Test
        @DisplayName("应使用默认配置")
        void shouldUseDefaultConfiguration() {
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            assertThat(oauth2Config.isEnabled()).isTrue();
            assertThat(oauth2Config.getAuthorizationCodeTtl()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("应支持自定义配置")
        void shouldSupportCustomConfiguration() {
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            oauth2Config.setEnabled(false);
            oauth2Config.setAuthorizationCodeTtl(Duration.ofMinutes(10));

            assertThat(oauth2Config.isEnabled()).isFalse();
            assertThat(oauth2Config.getAuthorizationCodeTtl()).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("应支持预配置客户端")
        void shouldSupportPreconfiguredClients() {
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            Set<AuthSecurityProperties.OAuth2Config.ClientConfig> clients = Set.of(
                    new AuthSecurityProperties.OAuth2Config.ClientConfig(
                            "client-001",
                            "secret-123",
                            "Test Client",
                            Set.of("https://example.com/callback"),
                            Set.of("read", "write"),
                            Set.of("authorization_code", "refresh_token"),
                            false
                    )
            );

            oauth2Config.setClients(clients);

            assertThat(oauth2Config.getClients()).hasSize(1);
            assertThat(oauth2Config.getClients().iterator().next().clientId()).isEqualTo("client-001");
        }
    }

    @Nested
    @DisplayName("OAuth2AutoConfiguration 测试")
    class AutoConfigurationTests {

        @Test
        @DisplayName("应创建默认客户端服务")
        void shouldCreateDefaultClientService() {
            AuthSecurityProperties properties = new AuthSecurityProperties();
            OAuth2AutoConfiguration config = new OAuth2AutoConfiguration();

            OAuth2ClientService clientService = config.oAuth2ClientService(properties);

            assertThat(clientService).isNotNull();
        }

        @Test
        @DisplayName("应从配置加载客户端")
        void shouldLoadClientsFromConfiguration() {
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            oauth2Config.setClients(Set.of(
                    new AuthSecurityProperties.OAuth2Config.ClientConfig(
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
