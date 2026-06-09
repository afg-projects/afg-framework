package io.github.afgprojects.framework.security.core.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtClaimsConfig} 单元测试。
 *
 * @since 1.0.0
 */
class JwtClaimsConfigTest {

    @Test
    void shouldReturnDefaultValues_whenCreatedWithDefaultConstructor() {
        JwtClaimsConfig config = new JwtClaimsConfig();

        assertThat(config.getUserIdClaim()).isEqualTo("sub");
        assertThat(config.getUsernameClaim()).isEqualTo("preferred_username");
        assertThat(config.getRolesClaim()).isEqualTo("roles");
        assertThat(config.getPermissionsClaim()).isEqualTo("permissions");
        assertThat(config.getTenantIdClaim()).isEqualTo("tenant_id");
        assertThat(config.getTokenTypeClaim()).isEqualTo("token_type");
        assertThat(config.getIssuerClaim()).isEqualTo("iss");
    }

    @Test
    void shouldReturnCustomValues_whenSettersCalled() {
        JwtClaimsConfig config = new JwtClaimsConfig();
        config.setUserIdClaim("user_id");
        config.setUsernameClaim("name");
        config.setRolesClaim("custom_roles");
        config.setPermissionsClaim("custom_perms");
        config.setTenantIdClaim("custom_tenant");
        config.setTokenTypeClaim("type");
        config.setIssuerClaim("custom_iss");

        assertThat(config.getUserIdClaim()).isEqualTo("user_id");
        assertThat(config.getUsernameClaim()).isEqualTo("name");
        assertThat(config.getRolesClaim()).isEqualTo("custom_roles");
        assertThat(config.getPermissionsClaim()).isEqualTo("custom_perms");
        assertThat(config.getTenantIdClaim()).isEqualTo("custom_tenant");
        assertThat(config.getTokenTypeClaim()).isEqualTo("type");
        assertThat(config.getIssuerClaim()).isEqualTo("custom_iss");
    }

    @Test
    void shouldMatchDefaultConstants() {
        assertThat(JwtClaimsConfig.DEFAULT_USER_ID_CLAIM).isEqualTo("sub");
        assertThat(JwtClaimsConfig.DEFAULT_USERNAME_CLAIM).isEqualTo("preferred_username");
        assertThat(JwtClaimsConfig.DEFAULT_ROLES_CLAIM).isEqualTo("roles");
        assertThat(JwtClaimsConfig.DEFAULT_PERMISSIONS_CLAIM).isEqualTo("permissions");
        assertThat(JwtClaimsConfig.DEFAULT_TENANT_ID_CLAIM).isEqualTo("tenant_id");
        assertThat(JwtClaimsConfig.DEFAULT_TOKEN_TYPE_CLAIM).isEqualTo("token_type");
        assertThat(JwtClaimsConfig.DEFAULT_ISSUER_CLAIM).isEqualTo("iss");
    }
}
