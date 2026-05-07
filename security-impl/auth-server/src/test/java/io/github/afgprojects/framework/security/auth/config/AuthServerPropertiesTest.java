package io.github.afgprojects.framework.security.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AuthServerProperties 测试
 */
@DisplayName("AuthServerProperties 测试")
class AuthServerPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValuesTests {

        @Test
        @DisplayName("应该使用正确的默认值")
        void shouldUseCorrectDefaultValues() {
            // given
            AuthServerProperties properties = new AuthServerProperties();

            // then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getIssuer()).isNull();
            assertThat(properties.getSigningKey()).isNull();
            assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(2));
            assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.isRequirePkce()).isTrue();
            assertThat(properties.getSupportedGrantTypes()).containsExactlyInAnyOrder(
                    "authorization_code",
                    "client_credentials",
                    "refresh_token");
        }

        @Test
        @DisplayName("Token 配置应该有正确的默认值")
        void shouldHaveCorrectTokenConfigDefaults() {
            // given
            AuthServerProperties properties = new AuthServerProperties();
            AuthServerProperties.TokenConfig tokenConfig = properties.getToken();

            // then
            assertThat(tokenConfig).isNotNull();
            assertThat(tokenConfig.getAccessTokenFormat()).isEqualTo("jwt");
            assertThat(tokenConfig.isIncludeUserRoles()).isTrue();
            assertThat(tokenConfig.isIncludeUserPermissions()).isTrue();
        }
    }

    @Nested
    @DisplayName("属性设置测试")
    class PropertySettingTests {

        @Test
        @DisplayName("应该正确设置基本属性")
        void shouldSetBasicProperties() {
            // given
            AuthServerProperties properties = new AuthServerProperties();

            // when
            properties.setEnabled(false);
            properties.setIssuer("https://auth.example.com");
            properties.setSigningKey("my-secret-signing-key");
            properties.setAccessTokenTtl(Duration.ofHours(1));
            properties.setRefreshTokenTtl(Duration.ofDays(14));
            properties.setRequirePkce(false);

            // then
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(properties.getSigningKey()).isEqualTo("my-secret-signing-key");
            assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(14));
            assertThat(properties.isRequirePkce()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置支持的授权类型")
        void shouldSetSupportedGrantTypes() {
            // given
            AuthServerProperties properties = new AuthServerProperties();
            Set<String> grantTypes = Set.of("authorization_code", "client_credentials");

            // when
            properties.setSupportedGrantTypes(grantTypes);

            // then
            assertThat(properties.getSupportedGrantTypes())
                    .containsExactlyInAnyOrder("authorization_code", "client_credentials");
        }

        @Test
        @DisplayName("应该正确设置 Token 配置")
        void shouldSetTokenConfig() {
            // given
            AuthServerProperties properties = new AuthServerProperties();
            AuthServerProperties.TokenConfig tokenConfig = new AuthServerProperties.TokenConfig();

            // when
            tokenConfig.setAccessTokenFormat("opaque");
            tokenConfig.setIncludeUserRoles(false);
            tokenConfig.setIncludeUserPermissions(false);
            properties.setToken(tokenConfig);

            // then
            assertThat(properties.getToken().getAccessTokenFormat()).isEqualTo("opaque");
            assertThat(properties.getToken().isIncludeUserRoles()).isFalse();
            assertThat(properties.getToken().isIncludeUserPermissions()).isFalse();
        }
    }

    @Nested
    @DisplayName("配置前缀测试")
    class ConfigurationPrefixTests {

        @Test
        @DisplayName("应该使用正确的配置前缀")
        void shouldUseCorrectConfigurationPrefix() {
            // when
            ConfigurationProperties annotation = AuthServerProperties.class
                    .getAnnotation(ConfigurationProperties.class);

            // then
            assertThat(annotation).isNotNull();
            assertThat(annotation.prefix()).isEqualTo("afg.auth.server");
        }
    }
}