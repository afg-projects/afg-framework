package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityInvocationInterceptorTest {

    // --- Helper stubs ---

    static class StubOperationMetadata implements OperationMetadata {
        private final String name;
        private final List<String> requiredRoles;
        private final String permission;
        private final boolean tenantScope;
        private final boolean dataScope;

        StubOperationMetadata(String name, List<String> requiredRoles, String permission,
                              boolean tenantScope, boolean dataScope) {
            this.name = name;
            this.requiredRoles = requiredRoles != null ? requiredRoles : List.of();
            this.permission = permission != null ? permission : "";
            this.tenantScope = tenantScope;
            this.dataScope = dataScope;
        }

        @Override public String name() { return name; }
        @Override public String description() { return ""; }
        @Override public MethodKey method() { return null; }
        @Override public List<ParameterMetadata> parameters() { return List.of(); }
        @Override public String returnType() { return ""; }
        @Override public String returnDescription() { return ""; }
        @Override public String permission() { return permission; }
        @Override public List<String> requiredRoles() { return requiredRoles; }
        @Override public boolean audit() { return false; }
        @Override public boolean tenantScope() { return tenantScope; }
        @Override public boolean dataScope() { return dataScope; }
        @Override public boolean async() { return false; }
        @Override public boolean deprecated() { return false; }
        @Override public String inputSchema() { return ""; }
        @Override public boolean paged() { return false; }
    }

    private InvocationContext makeContext(OperationMetadata op) {
        return new DefaultInvocationContext(
            null, op, new Object(), new Object[0], Map.of(), new HashMap<>()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- SecurityInvocationInterceptor tests ---

    @Nested
    class SecurityInterceptorTests {

        @Test
        void orderIs100() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            assertEquals(100, interceptor.order());
        }

        @Test
        void passesWhenNoPermissionRequired() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", false, false);
            InvocationContext ctx = makeContext(op);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void passesWhenNoRolesRequired() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", false, false);
            InvocationContext ctx = makeContext(op);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void rejectsWhenRolesRequiredButNotAuthenticated() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of("ADMIN"), "", false, false);
            InvocationContext ctx = makeContext(op);

            // No authentication set in SecurityContextHolder
            ServiceAccessDeniedException ex = assertThrows(ServiceAccessDeniedException.class,
                () -> interceptor.before(ctx));
            assertEquals("Authentication required", ex.permission());
        }

        @Test
        void passesWhenRolesRequiredAndUserHasMatchingRole() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of("ADMIN"), "", false, false);
            InvocationContext ctx = makeContext(op);

            // Set up authentication with ROLE_ADMIN authority
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            Authentication auth = new TestingAuthenticationToken("user1", "pass", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void rejectsWhenRolesRequiredButUserLacksRole() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of("ADMIN"), "", false, false);
            InvocationContext ctx = makeContext(op);

            // Set up authentication without ADMIN role
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            Authentication auth = new TestingAuthenticationToken("user1", "pass", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            ServiceAccessDeniedException ex = assertThrows(ServiceAccessDeniedException.class,
                () -> interceptor.before(ctx));
            assertTrue(ex.permission().contains("Missing required role"));
        }

        @Test
        void passesWhenPermissionRequiredAndUserHasPermission() {
            PermissionService permissionService = new PermissionService() {
                @Override public boolean hasRole(String userId, String role) { return false; }
                @Override public boolean hasAnyRole(String userId, Set<String> roles) { return false; }
                @Override public boolean hasAllRoles(String userId, Set<String> roles) { return false; }
                @Override public boolean hasPermission(String userId, String perm) { return true; }
                @Override public boolean hasAnyPermission(String userId, Set<String> permissions) { return false; }
                @Override public boolean hasAllPermissions(String userId, Set<String> permissions) { return false; }
                @Override public boolean check(String userId, String action, String resourceId) { return false; }
                @Override public boolean check(String userId, String action, String resourceId, String tenantId) { return false; }
                @Override public Set<String> getRoles(String userId) { return Set.of(); }
                @Override public Set<String> getPermissions(String userId) { return Set.of(); }
                @Override public void grantRole(String userId, String role) {}
                @Override public void revokeRole(String userId, String role) {}
                @Override public void grantPermission(String role, String permission) {}
                @Override public void revokePermission(String role, String permission) {}
            };
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor(permissionService);
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "user:read", false, false);
            InvocationContext ctx = makeContext(op);

            Authentication auth = new TestingAuthenticationToken("user1", "pass", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(interceptor.before(ctx));
        }

        @Test
        void rejectsWhenPermissionRequiredButUserLacksPermission() {
            PermissionService permissionService = new PermissionService() {
                @Override public boolean hasRole(String userId, String role) { return false; }
                @Override public boolean hasAnyRole(String userId, Set<String> roles) { return false; }
                @Override public boolean hasAllRoles(String userId, Set<String> roles) { return false; }
                @Override public boolean hasPermission(String userId, String perm) { return false; }
                @Override public boolean hasAnyPermission(String userId, Set<String> permissions) { return false; }
                @Override public boolean hasAllPermissions(String userId, Set<String> permissions) { return false; }
                @Override public boolean check(String userId, String action, String resourceId) { return false; }
                @Override public boolean check(String userId, String action, String resourceId, String tenantId) { return false; }
                @Override public Set<String> getRoles(String userId) { return Set.of(); }
                @Override public Set<String> getPermissions(String userId) { return Set.of(); }
                @Override public void grantRole(String userId, String role) {}
                @Override public void revokeRole(String userId, String role) {}
                @Override public void grantPermission(String role, String permission) {}
                @Override public void revokePermission(String role, String permission) {}
            };
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor(permissionService);
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "user:delete", false, false);
            InvocationContext ctx = makeContext(op);

            Authentication auth = new TestingAuthenticationToken("user1", "pass", List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            ServiceAccessDeniedException ex = assertThrows(ServiceAccessDeniedException.class,
                () -> interceptor.before(ctx));
            assertEquals("user:delete", ex.permission());
        }

        @Test
        void passesWhenPermissionRequiredButNoPermissionService() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "user:read", false, false);
            InvocationContext ctx = makeContext(op);

            // No PermissionService set, so permission check is skipped
            assertTrue(interceptor.before(ctx));
        }

        @Test
        void afterReturnsResultUnchanged() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertEquals("result", interceptor.after(ctx, "result"));
        }

        @Test
        void onErrorDoesNothing() {
            SecurityInvocationInterceptor interceptor = new SecurityInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertDoesNotThrow(() -> interceptor.onError(ctx, new RuntimeException()));
        }
    }

    // --- TenantInvocationInterceptor tests ---

    @Nested
    class TenantInterceptorTests {

        @Test
        void orderIs200() {
            TenantInvocationInterceptor interceptor = new TenantInvocationInterceptor();
            assertEquals(200, interceptor.order());
        }

        @Test
        void passesAndSetsTenantScopeAttributeWhenTenantScopeTrue() {
            TenantInvocationInterceptor interceptor = new TenantInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", true, false);
            Map<String, Object> attrs = new HashMap<>();
            InvocationContext ctx = new DefaultInvocationContext(null, op, new Object(), new Object[0], Map.of(), attrs);

            assertTrue(interceptor.before(ctx));
            assertEquals(true, attrs.get("tenantScope"));
        }

        @Test
        void passesWithoutAttributeWhenTenantScopeFalse() {
            TenantInvocationInterceptor interceptor = new TenantInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", false, false);
            Map<String, Object> attrs = new HashMap<>();
            InvocationContext ctx = new DefaultInvocationContext(null, op, new Object(), new Object[0], Map.of(), attrs);

            assertTrue(interceptor.before(ctx));
            assertFalse(attrs.containsKey("tenantScope"));
        }

        @Test
        void afterReturnsResultUnchanged() {
            TenantInvocationInterceptor interceptor = new TenantInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertEquals("result", interceptor.after(ctx, "result"));
        }

        @Test
        void onErrorDoesNothing() {
            TenantInvocationInterceptor interceptor = new TenantInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertDoesNotThrow(() -> interceptor.onError(ctx, new RuntimeException()));
        }
    }

    // --- DataScopeInvocationInterceptor tests ---

    @Nested
    class DataScopeInterceptorTests {

        @Test
        void orderIs300() {
            DataScopeInvocationInterceptor interceptor = new DataScopeInvocationInterceptor();
            assertEquals(300, interceptor.order());
        }

        @Test
        void passesAndSetsDataScopeEnabledAttributeWhenDataScopeTrue() {
            DataScopeInvocationInterceptor interceptor = new DataScopeInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", false, true);
            Map<String, Object> attrs = new HashMap<>();
            InvocationContext ctx = new DefaultInvocationContext(null, op, new Object(), new Object[0], Map.of(), attrs);

            assertTrue(interceptor.before(ctx));
            assertEquals(true, attrs.get("dataScopeEnabled"));
        }

        @Test
        void passesWithoutAttributeWhenDataScopeFalse() {
            DataScopeInvocationInterceptor interceptor = new DataScopeInvocationInterceptor();
            OperationMetadata op = new StubOperationMetadata("op", List.of(), "", false, false);
            Map<String, Object> attrs = new HashMap<>();
            InvocationContext ctx = new DefaultInvocationContext(null, op, new Object(), new Object[0], Map.of(), attrs);

            assertTrue(interceptor.before(ctx));
            assertFalse(attrs.containsKey("dataScopeEnabled"));
        }

        @Test
        void afterReturnsResultUnchanged() {
            DataScopeInvocationInterceptor interceptor = new DataScopeInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertEquals("result", interceptor.after(ctx, "result"));
        }

        @Test
        void onErrorDoesNothing() {
            DataScopeInvocationInterceptor interceptor = new DataScopeInvocationInterceptor();
            InvocationContext ctx = makeContext(
                new StubOperationMetadata("op", List.of(), "", false, false));
            assertDoesNotThrow(() -> interceptor.onError(ctx, new RuntimeException()));
        }
    }
}
