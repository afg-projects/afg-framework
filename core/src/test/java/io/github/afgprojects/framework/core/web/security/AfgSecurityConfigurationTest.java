package io.github.afgprojects.framework.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AfgSecurityConfigurationTest {

    @Test
    void should_collectPermitRules_when_permitCalled() {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        config.permit("/api/auth/login", "/api/auth/register");
        assertThat(config.getPermitPatterns()).containsExactly("/api/auth/login", "/api/auth/register");
    }

    @Test
    void should_collectDenyRules_when_denyCalled() {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        config.deny("/api/admin/delete");
        assertThat(config.getDenyPatterns()).containsExactly("/api/admin/delete");
    }

    @Test
    void should_collectRoleRules_when_requireRoleCalled() {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        config.requireRole("ADMIN", "/api/admin/**");
        assertThat(config.getRoleRules()).hasSize(1);
        assertThat(config.getRoleRules().get(0).role()).isEqualTo("ADMIN");
        assertThat(config.getRoleRules().get(0).patterns()).containsExactly("/api/admin/**");
    }

    @Test
    void should_collectPermissionRules_when_requirePermissionCalled() {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        config.requirePermission("user:write", "/api/user/**");
        assertThat(config.getPermissionRules()).hasSize(1);
        assertThat(config.getPermissionRules().get(0).permission()).isEqualTo("user:write");
        assertThat(config.getPermissionRules().get(0).patterns()).containsExactly("/api/user/**");
    }
}
