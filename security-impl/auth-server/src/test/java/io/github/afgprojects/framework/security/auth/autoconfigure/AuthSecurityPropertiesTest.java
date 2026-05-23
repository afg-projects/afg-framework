package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AuthSecurityProperties 测试
 */
@DisplayName("AuthSecurityProperties 测试")
class AuthSecurityPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValuesTests {

        @Test
        @DisplayName("应该使用正确的默认值")
        void shouldUseCorrectDefaultValues() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();

            // then
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Token 配置应该有正确的默认值")
        void shouldHaveCorrectTokenConfigDefaults() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.TokenConfig tokenConfig = properties.getToken();

            // then
            assertThat(tokenConfig).isNotNull();
            assertThat(tokenConfig.getIssuer()).isEqualTo("afg-framework");
            assertThat(tokenConfig.getAccessTokenTtl()).isEqualTo(Duration.ofHours(2));
            assertThat(tokenConfig.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));
        }

        @Test
        @DisplayName("OAuth2 配置应该有正确的默认值")
        void shouldHaveCorrectOAuth2ConfigDefaults() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            // then
            assertThat(oauth2Config).isNotNull();
            assertThat(oauth2Config.isEnabled()).isTrue();
            assertThat(oauth2Config.getAuthorizationCodeTtl()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("Login 配置应该有正确的默认值")
        void shouldHaveCorrectLoginConfigDefaults() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.LoginConfig loginConfig = properties.getLogin();

            // then
            assertThat(loginConfig).isNotNull();
            assertThat(loginConfig.isEnabled()).isTrue();
            assertThat(loginConfig.getCaptchaLength()).isEqualTo(4);
            assertThat(loginConfig.getCaptchaTtl()).isEqualTo(Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("属性设置测试")
    class PropertySettingTests {

        @Test
        @DisplayName("应该正确设置 Token 配置")
        void shouldSetTokenConfig() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.TokenConfig tokenConfig = properties.getToken();

            // when
            tokenConfig.setIssuer("https://auth.example.com");
            tokenConfig.setKeyStorePath("file:/var/afg/keys");
            tokenConfig.setAccessTokenTtl(Duration.ofHours(1));
            tokenConfig.setRefreshTokenTtl(Duration.ofDays(14));

            // then
            assertThat(tokenConfig.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(tokenConfig.getKeyStorePath()).isEqualTo("file:/var/afg/keys");
            assertThat(tokenConfig.getAccessTokenTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(tokenConfig.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(14));
        }

        @Test
        @DisplayName("应该正确设置 OAuth2 客户端配置")
        void shouldSetOAuth2ClientConfig() {
            // given
            AuthSecurityProperties properties = new AuthSecurityProperties();
            AuthSecurityProperties.OAuth2Config oauth2Config = properties.getOauth2();

            // when
            Set<AuthSecurityProperties.OAuth2Config.ClientConfig> clients = Set.of(
                    new AuthSecurityProperties.OAuth2Config.ClientConfig(
                            "client-001",
                            "secret-123",
                            "Test Client",
                            Set.of("https://example.com/callback"),
                            Set.of("read", "write"),
                            Set.of("authorization_code", "refresh_token"),
                            true
                    )
            );
            oauth2Config.setClients(clients);

            // then
            assertThat(oauth2Config.getClients()).hasSize(1);
            assertThat(oauth2Config.getClients().iterator().next().clientId()).isEqualTo("client-001");
        }
    }

    @Nested
    @DisplayName("配置前缀测试")
    class ConfigurationPrefixTests {

        @Test
        @DisplayName("应该使用正确的配置前缀")
        void shouldUseCorrectConfigurationPrefix() {
            // when
            ConfigurationProperties annotation = AuthSecurityProperties.class
                    .getAnnotation(ConfigurationProperties.class);

            // then
            assertThat(annotation).isNotNull();
            assertThat(annotation.prefix()).isEqualTo("afg.security.auth-server");
        }
    }
}
