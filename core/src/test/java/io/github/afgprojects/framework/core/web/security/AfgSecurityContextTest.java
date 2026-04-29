package io.github.afgprojects.framework.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AfgSecurityContextTest {

    @Test
    void should_returnContextInfo_when_usingAnonymousImplementation() {
        AfgPrincipal principal = new AfgPrincipal() {
            @Override
            public String getId() {
                return "user-1";
            }

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return Set.of("ADMIN");
            }

            @Override
            public Set<String> getPermissions() {
                return Set.of("user:read");
            }
        };

        AfgSecurityContext context = new AfgSecurityContext() {
            @Override
            public AfgPrincipal getPrincipal() {
                return principal;
            }

            @Override
            public String getTenantId() {
                return "tenant-1";
            }

            @Override
            public <T> T getAttribute(String key) {
                return null;
            }
        };

        assertThat(context.getPrincipal().getId()).isEqualTo("user-1");
        assertThat(context.getTenantId()).isEqualTo("tenant-1");
    }
}
