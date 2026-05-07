package io.github.afgprojects.framework.security.resource.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JwtResourceProperties 测试类。
 *
 * @since 1.0.0
 */
class JwtResourcePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("enabled 默认值为 true")
        void shouldDefaultEnabledToTrue() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("jwsAlgorithm 默认值为 RS256")
        void shouldDefaultJwsAlgorithmToRS256() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getJwsAlgorithm()).isEqualTo("RS256");
        }

        @Test
        @DisplayName("cacheTtl 默认值为 5 分钟")
        void shouldDefaultCacheTtlTo5Minutes() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("tenantIdClaim 默认值为 tenant_id")
        void shouldDefaultTenantIdClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getTenantIdClaim()).isEqualTo("tenant_id");
        }

        @Test
        @DisplayName("userIdClaim 默认值为 sub")
        void shouldDefaultUserIdClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getUserIdClaim()).isEqualTo("sub");
        }

        @Test
        @DisplayName("usernameClaim 默认值为 preferred_username")
        void shouldDefaultUsernameClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getUsernameClaim()).isEqualTo("preferred_username");
        }

        @Test
        @DisplayName("rolesClaim 默认值为 roles")
        void shouldDefaultRolesClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getRolesClaim()).isEqualTo("roles");
        }

        @Test
        @DisplayName("permissionsClaim 默认值为 permissions")
        void shouldDefaultPermissionsClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getPermissionsClaim()).isEqualTo("permissions");
        }

        @Test
        @DisplayName("audience 默认值为空集合")
        void shouldDefaultAudienceToEmptySet() {
            JwtResourceProperties properties = new JwtResourceProperties();
            assertThat(properties.getAudience()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setter 测试")
    class SetterTests {

        @Test
        @DisplayName("设置 enabled")
        void shouldSetEnabled() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("设置 jwkSetUri")
        void shouldSetJwkSetUri() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setJwkSetUri("https://auth.example.com/.well-known/jwks.json");
            assertThat(properties.getJwkSetUri()).isEqualTo("https://auth.example.com/.well-known/jwks.json");
        }

        @Test
        @DisplayName("设置 issuerUri")
        void shouldSetIssuerUri() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setIssuerUri("https://auth.example.com");
            assertThat(properties.getIssuerUri()).isEqualTo("https://auth.example.com");
        }

        @Test
        @DisplayName("设置 cacheTtl")
        void shouldSetCacheTtl() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setCacheTtl(Duration.ofMinutes(10));
            assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("设置 audience")
        void shouldSetAudience() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setAudience(Set.of("api1", "api2"));
            assertThat(properties.getAudience()).containsExactlyInAnyOrder("api1", "api2");
        }

        @Test
        @DisplayName("设置 tenantIdClaim")
        void shouldSetTenantIdClaim() {
            JwtResourceProperties properties = new JwtResourceProperties();
            properties.setTenantIdClaim("custom_tenant");
            assertThat(properties.getTenantIdClaim()).isEqualTo("custom_tenant");
        }
    }
}