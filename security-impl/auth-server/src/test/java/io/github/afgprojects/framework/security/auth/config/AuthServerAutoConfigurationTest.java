package io.github.afgprojects.framework.security.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import io.github.afgprojects.framework.security.auth.token.JwtTokenProvider;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetailsService;

/**
 * AuthServerAutoConfiguration 自动配置测试
 *
 * <p>验证条件装配逻辑。
 */
@DisplayName("AuthServerAutoConfiguration 测试")
class AuthServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthServerAutoConfiguration.class));

    @Nested
    @DisplayName("条件装配测试")
    class ConditionalConfigurationTests {

        @Test
        @DisplayName("应该在启用时自动配置 Bean")
        void shouldAutoConfigureWhenEnabled() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.enabled=true",
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long-for-security")
                    .run(context -> {
                        assertThat(context).hasSingleBean(AuthServerProperties.class);
                        assertThat(context).hasSingleBean(JwtTokenProvider.class);
                        assertThat(context).hasSingleBean(AuthorizationServerSettings.class);
                    });
        }

        @Test
        @DisplayName("应该在禁用时不配置 Bean")
        void shouldNotAutoConfigureWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.auth.server.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(AuthServerProperties.class);
                        assertThat(context).doesNotHaveBean(JwtTokenProvider.class);
                        assertThat(context).doesNotHaveBean(AuthorizationServerSettings.class);
                    });
        }

        @Test
        @DisplayName("应该在默认配置下自动装配（matchIfMissing=true）")
        void shouldAutoConfigureByDefault() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long-for-security")
                    .run(context -> {
                        assertThat(context).hasSingleBean(AuthServerProperties.class);
                        assertThat(context).hasSingleBean(JwtTokenProvider.class);
                        assertThat(context).hasSingleBean(AuthorizationServerSettings.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定测试")
    class PropertiesBindingTests {

        @Test
        @DisplayName("应该正确绑定配置属性")
        void shouldBindConfigurationProperties() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.enabled=true",
                            "afg.auth.server.issuer=https://custom.auth.com",
                            "afg.auth.server.signing-key=custom-signing-key-must-be-at-least-256-bits-long",
                            "afg.auth.server.access-token-ttl=1h",
                            "afg.auth.server.refresh-token-ttl=14d",
                            "afg.auth.server.require-pkce=false")
                    .run(context -> {
                        AuthServerProperties properties = context.getBean(AuthServerProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                        assertThat(properties.getIssuer()).isEqualTo("https://custom.auth.com");
                        assertThat(properties.getSigningKey()).isEqualTo("custom-signing-key-must-be-at-least-256-bits-long");
                        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(1));
                        assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(14));
                        assertThat(properties.isRequirePkce()).isFalse();
                    });
        }

        @Test
        @DisplayName("应该使用默认属性值")
        void shouldUseDefaultPropertyValues() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long")
                    .run(context -> {
                        AuthServerProperties properties = context.getBean(AuthServerProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofHours(2));
                        assertThat(properties.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(7));
                        assertThat(properties.isRequirePkce()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("JwtTokenProvider 配置测试")
    class JwtTokenProviderConfigurationTests {

        @Test
        @DisplayName("应该使用配置的属性创建 JwtTokenProvider")
        void shouldCreateJwtTokenProviderWithConfiguredProperties() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long",
                            "afg.auth.server.access-token-ttl=4h")
                    .run(context -> {
                        JwtTokenProvider provider = context.getBean(JwtTokenProvider.class);
                        assertThat(provider).isNotNull();

                        // 验证 provider 可以正常工作
                        String token = provider.generateAccessToken(
                                "test-user",
                                java.util.List.of("USER"),
                                java.util.List.of("read"),
                                java.util.Map.of());
                        assertThat(token).isNotBlank();
                    });
        }
    }

    @Nested
    @DisplayName("AuthorizationServerSettings 配置测试")
    class AuthorizationServerSettingsConfigurationTests {

        @Test
        @DisplayName("应该使用配置的 issuer 创建 AuthorizationServerSettings")
        void shouldCreateAuthorizationServerSettingsWithConfiguredIssuer() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://my-auth-server.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long")
                    .run(context -> {
                        AuthorizationServerSettings settings = context.getBean(AuthorizationServerSettings.class);
                        assertThat(settings).isNotNull();
                        assertThat(settings.getIssuer()).isEqualTo("https://my-auth-server.com");
                    });
        }
    }

    @Nested
    @DisplayName("RegisteredClientRepository 配置测试")
    class RegisteredClientRepositoryConfigurationTests {

        @Test
        @DisplayName("应该在提供 AfgClientDetailsService 时创建 RegisteredClientRepository")
        void shouldCreateRegisteredClientRepositoryWhenClientDetailsServiceProvided() {
            // 创建一个 mock 的 AfgClientDetailsService
            AfgClientDetailsService mockService = new AfgClientDetailsService() {
                @Override
                public io.github.afgprojects.framework.security.auth.user.AfgClientDetails loadClientByClientId(String clientId) {
                    return null;
                }
            };

            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long")
                    .withBean(AfgClientDetailsService.class, () -> mockService)
                    .run(context -> {
                        assertThat(context).hasSingleBean(RegisteredClientRepository.class);
                    });
        }

        @Test
        @DisplayName("应该在缺少 AfgClientDetailsService 时不创建 RegisteredClientRepository")
        void shouldNotCreateRegisteredClientRepositoryWithoutClientDetailsService() {
            contextRunner
                    .withPropertyValues(
                            "afg.auth.server.issuer=https://auth.example.com",
                            "afg.auth.server.signing-key=test-signing-key-must-be-at-least-256-bits-long")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(RegisteredClientRepository.class);
                    });
        }
    }
}