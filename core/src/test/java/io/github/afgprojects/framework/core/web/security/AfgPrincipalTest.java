package io.github.afgprojects.framework.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AfgPrincipalTest {

    @Test
    void should_returnPrincipalInfo_when_usingAnonymousImplementation() {
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
                return Set.of("user:read", "user:write");
            }
        };

        assertThat(principal.getId()).isEqualTo("user-1");
        assertThat(principal.getName()).isEqualTo("admin");
        assertThat(principal.getRoles()).containsExactly("ADMIN");
        assertThat(principal.getPermissions()).containsExactlyInAnyOrder("user:read", "user:write");
    }
}
