package io.github.afgprojects.framework.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AfgEnforcerTest {

    @Test
    void should_denyAll_when_defaultDenyEnforcer() {
        AfgEnforcer enforcer = (subject, resource, action) -> false;

        AfgSecurityContext context = new AfgSecurityContext() {
            @Override
            public AfgPrincipal getPrincipal() {
                return null;
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public <T> T getAttribute(String key) {
                return null;
            }
        };

        assertThat(enforcer.enforce(context, "/api/data", "read")).isFalse();
    }

    @Test
    void should_allowAll_when_permitAllEnforcer() {
        AfgEnforcer enforcer = (subject, resource, action) -> true;

        assertThat(enforcer.enforce("user-1", "/api/data", "read")).isTrue();
    }

    @Test
    void should_delegateToSubjectOverload_when_usingSubjectEnforce() {
        AfgEnforcer enforcer = (subject, resource, action) -> "admin".equals(subject) && "read".equals(action);

        assertThat(enforcer.enforce("admin", "/api/data", "read")).isTrue();
        assertThat(enforcer.enforce("admin", "/api/data", "write")).isFalse();
        assertThat(enforcer.enforce("guest", "/api/data", "read")).isFalse();
    }
}
