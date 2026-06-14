package io.github.afgprojects.framework.security.resource.autoconfigure;

import io.github.afgprojects.framework.security.resource.properties.ResourceSecurityProperties;
import io.github.afgprojects.framework.security.resource.properties.jwt.ResourceSecurityJwtProperties;
import io.github.afgprojects.framework.security.resource.properties.permission.ResourceSecurityPermissionProperties;
import io.github.afgprojects.framework.security.resource.properties.tenant.ResourceSecurityTenantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ResourceSecurityConfigValidator 测试
 *
 * <p>验证资源服务器配置校验器的启动检查逻辑。
 */
@DisplayName("ResourceSecurityConfigValidator 测试")
class ResourceSecurityConfigValidatorTest {

    private ResourceSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ResourceSecurityProperties();
    }

    @Nested
    @DisplayName("配置验证")
    class ConfigValidationTests {

        @Test
        @DisplayName("JWT 启用但缺少 jwkSetUri 和 publicKey 且无 auth-server 时应抛出 IllegalStateException")
        void shouldThrowWhenJwtEnabledWithoutRequiredConfig() {
            // JWT 默认启用，jwkSetUri 和 publicKey 默认为 null
            ResourceSecurityJwtProperties jwtProps = properties.getJwt();
            jwtProps.setEnabled(true);
            jwtProps.setJwkSetUri(null);
            jwtProps.setPublicKey(null);

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            // auth-server 不在 classpath 时，应抛出异常
            assertThatThrownBy(() -> validator.run(new TestApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwk-set-uri");
        }

        @Test
        @DisplayName("配置 jwkSetUri 后应验证通过")
        void shouldPassWhenJwkSetUriIsConfigured() {
            properties.getJwt().setEnabled(true);
            properties.getJwt().setJwkSetUri("https://auth.example.com/.well-known/jwks.json");

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            // 不应抛出异常
            validator.run(new TestApplicationArguments());
        }

        @Test
        @DisplayName("配置 publicKey 后应验证通过")
        void shouldPassWhenPublicKeyIsConfigured() {
            properties.getJwt().setEnabled(true);
            properties.getJwt().setPublicKey("-----BEGIN PUBLIC KEY-----\nMIIBIjAN...\n-----END PUBLIC KEY-----");

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            // 不应抛出异常
            validator.run(new TestApplicationArguments());
        }

        @Test
        @DisplayName("JWT 禁用时应验证通过")
        void shouldPassWhenJwtDisabled() {
            properties.getJwt().setEnabled(false);

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            // 不应抛出异常
            validator.run(new TestApplicationArguments());
        }

        @Test
        @DisplayName("jwkSetUri 为空白时应与 null 同等处理")
        void shouldTreatBlankJwkSetUriAsMissing() {
            properties.getJwt().setEnabled(true);
            properties.getJwt().setJwkSetUri("   ");
            properties.getJwt().setPublicKey(null);

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            assertThatThrownBy(() -> validator.run(new TestApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("publicKey 为空白时应与 null 同等处理")
        void shouldTreatBlankPublicKeyAsMissing() {
            properties.getJwt().setEnabled(true);
            properties.getJwt().setJwkSetUri(null);
            properties.getJwt().setPublicKey("   ");

            ResourceSecurityConfigValidator validator = new ResourceSecurityConfigValidator(properties);

            assertThatThrownBy(() -> validator.run(new TestApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("默认配置值")
    class DefaultConfigTests {

        @Test
        @DisplayName("默认应启用资源服务器")
        void shouldEnableByDefault() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认应启用默认安全配置")
        void shouldEnableDefaultSecurityByDefault() {
            assertThat(properties.isDefaultSecurity()).isTrue();
        }

        @Test
        @DisplayName("默认应启用 JWT 验证")
        void shouldEnableJwtByDefault() {
            assertThat(properties.getJwt().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 JWT 算法应为 RS256")
        void shouldUseRs256ByDefault() {
            assertThat(properties.getJwt().getJwsAlgorithm()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("默认 userId claim 应为 sub")
        void shouldUseSubAsUserIdClaimByDefault() {
            assertThat(properties.getJwt().getUserIdClaim()).isEqualTo("sub");
        }

        @Test
        @DisplayName("默认 username claim 应为 preferred_username")
        void shouldUsePreferredUsernameAsUsernameClaimByDefault() {
            assertThat(properties.getJwt().getUsernameClaim()).isEqualTo("preferred_username");
        }

        @Test
        @DisplayName("默认 tenantId claim 应为 tenant_id")
        void shouldUseTenantIdAsTenantIdClaimByDefault() {
            assertThat(properties.getJwt().getTenantIdClaim()).isEqualTo("tenant_id");
        }
    }

    /**
     * 测试用 ApplicationArguments 实现。
     */
    private static class TestApplicationArguments implements ApplicationArguments {

        @Override
        public String[] getSourceArgs() {
            return new String[0];
        }

        @Override
        public java.util.Set<String> getOptionNames() {
            return java.util.Collections.emptySet();
        }

        @Override
        public boolean containsOption(String name) {
            return false;
        }

        @Override
        public java.util.List<String> getOptionValues(String name) {
            return null;
        }

        @Override
        public java.util.List<String> getNonOptionArgs() {
            return java.util.Collections.emptyList();
        }
    }
}
