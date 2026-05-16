package io.github.afgprojects.framework.security.core.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RbacService 接口测试。
 *
 * @since 1.0.0
 */
class RbacServiceTest {

    @Test
    @DisplayName("测试 RbacService 接口定义 - getRoles")
    void testGetRoles() {
        RbacService service = new TestRbacService();
        Set<String> roles = service.getRoles("user-001", null);
        assertNotNull(roles);
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - getPermissions")
    void testGetPermissions() {
        RbacService service = new TestRbacService();
        Set<String> permissions = service.getPermissions("user-001", null);
        assertNotNull(permissions);
        assertTrue(permissions.contains("user:read"));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - hasRole")
    void testHasRole() {
        RbacService service = new TestRbacService();
        assertTrue(service.hasRole("user-001", "ADMIN", null));
        assertFalse(service.hasRole("user-001", "GUEST", null));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - hasPermission")
    void testHasPermission() {
        RbacService service = new TestRbacService();
        assertTrue(service.hasPermission("user-001", "user:read", null));
        assertFalse(service.hasPermission("user-001", "user:delete", null));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - grantRole")
    void testGrantRole() {
        TestRbacService service = new TestRbacService();
        service.grantRole("user-001", "MANAGER", null);
        assertTrue(service.hasRole("user-001", "MANAGER", null));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - revokeRole")
    void testRevokeRole() {
        TestRbacService service = new TestRbacService();
        service.revokeRole("user-001", "ADMIN", null);
        assertFalse(service.hasRole("user-001", "ADMIN", null));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - grantPermission")
    void testGrantPermission() {
        TestRbacService service = new TestRbacService();
        service.grantPermission("ADMIN", "user:delete", null);
        assertTrue(service.hasPermission("user-001", "user:delete", null));
    }

    @Test
    @DisplayName("测试 RbacService 接口定义 - revokePermission")
    void testRevokePermission() {
        TestRbacService service = new TestRbacService();
        service.revokePermission("ADMIN", "user:read", null);
        assertFalse(service.hasPermission("user-001", "user:read", null));
    }

    /**
     * 测试用 RbacService 实现。
     */
    private static class TestRbacService implements RbacService {
        private final Set<String> roles = new HashSet<>(Set.of("ADMIN"));
        private final Set<String> permissions = new HashSet<>(Set.of("user:read"));

        @Override
        public Set<String> getRoles(String userId, String tenantId) {
            return new HashSet<>(roles);
        }

        @Override
        public Set<String> getPermissions(String userId, String tenantId) {
            return new HashSet<>(permissions);
        }

        @Override
        public boolean hasRole(String userId, String role, String tenantId) {
            return roles.contains(role);
        }

        @Override
        public boolean hasPermission(String userId, String permission, String tenantId) {
            return permissions.contains(permission);
        }

        @Override
        public void grantRole(String userId, String role, String tenantId) {
            roles.add(role);
        }

        @Override
        public void revokeRole(String userId, String role, String tenantId) {
            roles.remove(role);
        }

        @Override
        public void grantPermission(String role, String permission, String tenantId) {
            permissions.add(permission);
        }

        @Override
        public void revokePermission(String role, String permission, String tenantId) {
            permissions.remove(permission);
        }
    }
}
