package io.github.afgprojects.framework.security.auth.oauth2;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import io.github.afgprojects.framework.security.auth.config.AuthServerProperties;

/**
 * AfgAuthorizationServerConfig 测试
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AfgAuthorizationServerConfigTest {

    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("应创建 AuthorizationServerSettings")
        void shouldCreateAuthorizationServerSettings() {
            // Given
            AuthServerProperties properties = new AuthServerProperties();
            properties.setIssuer("https://auth.test.example.com");

            AfgAuthorizationServerConfig config = new AfgAuthorizationServerConfig();

            // When
            AuthorizationServerSettings settings = config.authorizationServerSettings(properties);

            // Then
            assertThat(settings).isNotNull();
            assertThat(settings.getIssuer()).isEqualTo("https://auth.test.example.com");
            assertThat(settings.getAuthorizationEndpoint()).isEqualTo("/oauth2/authorize");
            assertThat(settings.getTokenEndpoint()).isEqualTo("/oauth2/token");
            assertThat(settings.getTokenIntrospectionEndpoint()).isEqualTo("/oauth2/introspect");
            assertThat(settings.getTokenRevocationEndpoint()).isEqualTo("/oauth2/revoke");
            assertThat(settings.getJwkSetEndpoint()).isEqualTo("/oauth2/jwks");
        }

        @Test
        @DisplayName("应使用默认配置")
        void shouldUseDefaultConfiguration() {
            // Given
            AuthServerProperties properties = new AuthServerProperties();
            properties.setIssuer("https://default.auth.example.com"); // 必须设置 issuer

            AfgAuthorizationServerConfig config = new AfgAuthorizationServerConfig();

            // When
            AuthorizationServerSettings settings = config.authorizationServerSettings(properties);

            // Then
            assertThat(settings).isNotNull();
            assertThat(settings.getIssuer()).isEqualTo("https://default.auth.example.com");
        }
    }

    @Nested
    @DisplayName("端点配置测试")
    class EndpointConfigurationTests {

        @Test
        @DisplayName("应配置所有 OAuth2 端点")
        void shouldConfigureAllOAuth2Endpoints() {
            // Given
            AuthServerProperties properties = new AuthServerProperties();
            properties.setIssuer("https://auth.example.com");

            AfgAuthorizationServerConfig config = new AfgAuthorizationServerConfig();

            // When
            AuthorizationServerSettings settings = config.authorizationServerSettings(properties);

            // Then
            assertThat(settings.getAuthorizationEndpoint()).isEqualTo("/oauth2/authorize");
            assertThat(settings.getTokenEndpoint()).isEqualTo("/oauth2/token");
            assertThat(settings.getTokenIntrospectionEndpoint()).isEqualTo("/oauth2/introspect");
            assertThat(settings.getTokenRevocationEndpoint()).isEqualTo("/oauth2/revoke");
            assertThat(settings.getJwkSetEndpoint()).isEqualTo("/oauth2/jwks");
            assertThat(settings.getOidcUserInfoEndpoint()).isEqualTo("/userinfo");
        }
    }
}
