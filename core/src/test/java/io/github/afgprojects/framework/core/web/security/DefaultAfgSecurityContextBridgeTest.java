package io.github.afgprojects.framework.core.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.context.RequestContext;

class DefaultAfgSecurityContextBridgeTest {

    @Test
    void should_populateRequestContext_when_securityContextProvided() {
        AfgPrincipal principal = new AfgPrincipal() {
            @Override
            public String getId() {
                return "42";
            }

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public Set<String> getRoles() {
                return Set.of();
            }

            @Override
            public Set<String> getPermissions() {
                return Set.of();
            }
        };

        AfgSecurityContext securityContext = new AfgSecurityContext() {
            @Override
            public AfgPrincipal getPrincipal() {
                return principal;
            }

            @Override
            public String getTenantId() {
                return "42";
            }

            @Override
            public <T> T getAttribute(String key) {
                return null;
            }
        };

        RequestContext requestContext = new RequestContext();

        AfgSecurityContextBridge bridge = new DefaultAfgSecurityContextBridge();
        bridge.populate(securityContext, requestContext);

        assertThat(requestContext.getUserId()).isEqualTo(42L);
        assertThat(requestContext.getUsername()).isEqualTo("admin");
        assertThat(requestContext.getTenantId()).isEqualTo(42L);
    }

    @Test
    void should_notThrow_when_principalIsNull() {
        AfgSecurityContext securityContext = new AfgSecurityContext() {
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

        RequestContext requestContext = new RequestContext();

        AfgSecurityContextBridge bridge = new DefaultAfgSecurityContextBridge();
        bridge.populate(securityContext, requestContext);

        assertThat(requestContext.getUserId()).isNull();
        assertThat(requestContext.getUsername()).isNull();
    }
}
