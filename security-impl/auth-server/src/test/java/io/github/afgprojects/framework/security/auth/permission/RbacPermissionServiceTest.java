package io.github.afgprojects.framework.security.auth.permission;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.permission.PermissionService;

/**
 * RBAC 权限服务测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class RbacPermissionServiceTest {

    private InMemoryRolePermissionStorage storage;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        storage = InMemoryRolePermissionStorage.createTestInstance();
        permissionService = new RbacPermissionService(storage);
    }

    @Nested
    @DisplayName("角色检查测试")
    class RoleCheckTests {

        @Test
        @DisplayName("应检查用户是否具有角色")
        void shouldCheckUserRole() {
            assertThat(permissionService.hasRole("admin-user", "ADMIN")).isTrue();
            assertThat(permissionService.hasRole("admin-user", "USER")).isFalse();
            assertThat(permissionService.hasRole("normal-user", "USER")).isTrue();
        }

        @Test
        @DisplayName("应检查用户是否具有任意角色")
        void shouldCheckAnyRole() {
            assertThat(permissionService.hasAnyRole("admin-user", Set.of("ADMIN", "USER"))).isTrue();
            assertThat(permissionService.hasAnyRole("normal-user", Set.of("ADMIN", "MANAGER"))).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有所有角色")
        void shouldCheckAllRoles() {
            // 给 admin-user 也添加 USER 角色
            storage.grantRole("admin-user", "USER");

            assertThat(permissionService.hasAllRoles("admin-user", Set.of("ADMIN", "USER"))).isTrue();
            assertThat(permissionService.hasAllRoles("normal-user", Set.of("USER", "ADMIN"))).isFalse();
        }

        @Test
        @DisplayName("应获取用户所有角色")
        void shouldGetUserRoles() {
            Set<String> roles = permissionService.getRoles("admin-user");

            assertThat(roles).contains("ADMIN");
            assertThat(roles).doesNotContain("USER");
        }
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionCheckTests {

        @Test
        @DisplayName("应检查用户是否具有权限")
        void shouldCheckUserPermission() {
            assertThat(permissionService.hasPermission("admin-user", "user:read")).isTrue();
            assertThat(permissionService.hasPermission("admin-user", "user:delete")).isTrue();
            assertThat(permissionService.hasPermission("normal-user", "user:read")).isTrue();
            assertThat(permissionService.hasPermission("normal-user", "user:delete")).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有任意权限")
        void shouldCheckAnyPermission() {
            assertThat(permissionService.hasAnyPermission("admin-user", Set.of("user:read", "user:delete"))).isTrue();
            assertThat(permissionService.hasAnyPermission("normal-user", Set.of("user:delete", "role:read"))).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有所有权限")
        void shouldCheckAllPermissions() {
            assertThat(permissionService.hasAllPermissions("admin-user", Set.of("user:read", "user:write"))).isTrue();
            assertThat(permissionService.hasAllPermissions("normal-user", Set.of("user:read", "user:write"))).isFalse();
        }

        @Test
        @DisplayName("应获取用户所有权限")
        void shouldGetUserPermissions() {
            Set<String> permissions = permissionService.getPermissions("admin-user");

            assertThat(permissions).contains("user:read", "user:write", "user:delete", "role:read", "role:write");
        }
    }

    @Nested
    @DisplayName("角色授权测试")
    class RoleGrantTests {

        @Test
        @DisplayName("应授予用户角色")
        void shouldGrantRole() {
            permissionService.grantRole("new-user", "USER");

            assertThat(permissionService.hasRole("new-user", "USER")).isTrue();
        }

        @Test
        @DisplayName("应撤销用户角色")
        void shouldRevokeRole() {
            permissionService.revokeRole("admin-user", "ADMIN");

            assertThat(permissionService.hasRole("admin-user", "ADMIN")).isFalse();
        }
    }

    @Nested
    @DisplayName("权限授权测试")
    class PermissionGrantTests {

        @Test
        @DisplayName("应授予角色权限")
        void shouldGrantPermission() {
            permissionService.grantPermission("USER", "profile:edit");

            assertThat(permissionService.hasPermission("normal-user", "profile:edit")).isTrue();
        }

        @Test
        @DisplayName("应撤销角色权限")
        void shouldRevokePermission() {
            permissionService.revokePermission("USER", "user:read");

            assertThat(permissionService.hasPermission("normal-user", "user:read")).isFalse();
        }
    }

    @Nested
    @DisplayName("ABAC 兼容性测试")
    class AbacCompatibilityTests {

        @Test
        @DisplayName("RBAC 模式下 check 应等同于权限检查")
        void checkShouldBeEquivalentToPermissionCheck() {
            assertThat(permissionService.check("admin-user", "user:read", "user-001")).isTrue();
            assertThat(permissionService.check("normal-user", "user:delete", "user-001")).isFalse();
        }
    }
}