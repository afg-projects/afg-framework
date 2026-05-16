package io.github.afgprojects.framework.security.auth.permission;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;

/**
 * DefaultRbacService 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultRbacServiceTest {

    @Mock
    private RolePermissionStorage storage;

    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new DefaultRbacService(storage);
    }

    @Nested
    @DisplayName("角色检查测试")
    class RoleCheckTests {

        @Test
        @DisplayName("应获取用户所有角色")
        void shouldGetUserRoles() {
            Set<String> roles = Set.of("ADMIN", "USER");
            when(storage.findRolesByUserId("user-001")).thenReturn(roles);

            Set<String> result = rbacService.getRoles("user-001", null);

            assertThat(result).containsExactlyInAnyOrder("ADMIN", "USER");
            verify(storage).findRolesByUserId("user-001");
        }

        @Test
        @DisplayName("应获取用户所有角色（带租户）")
        void shouldGetUserRolesWithTenant() {
            Set<String> roles = Set.of("ADMIN", "USER");
            when(storage.findRolesByUserId("user-001")).thenReturn(roles);

            // 当前实现忽略 tenantId
            Set<String> result = rbacService.getRoles("user-001", "tenant-001");

            assertThat(result).containsExactlyInAnyOrder("ADMIN", "USER");
            verify(storage).findRolesByUserId("user-001");
        }

        @Test
        @DisplayName("应检查用户是否具有角色")
        void shouldCheckUserRole() {
            Set<String> roles = Set.of("ADMIN", "USER");
            when(storage.findRolesByUserId("user-001")).thenReturn(roles);

            assertThat(rbacService.hasRole("user-001", "ADMIN", null)).isTrue();
            assertThat(rbacService.hasRole("user-001", "MANAGER", null)).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有角色（带租户）")
        void shouldCheckUserRoleWithTenant() {
            Set<String> roles = Set.of("ADMIN", "USER");
            when(storage.findRolesByUserId("user-001")).thenReturn(roles);

            assertThat(rbacService.hasRole("user-001", "ADMIN", "tenant-001")).isTrue();
            assertThat(rbacService.hasRole("user-001", "MANAGER", "tenant-001")).isFalse();
        }

        @Test
        @DisplayName("空角色集合应返回空结果")
        void shouldReturnEmptyRoles() {
            when(storage.findRolesByUserId("user-002")).thenReturn(Set.of());

            Set<String> result = rbacService.getRoles("user-002", null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionCheckTests {

        @Test
        @DisplayName("应获取用户所有权限")
        void shouldGetUserPermissions() {
            Set<String> roles = Set.of("ADMIN", "USER");
            Set<String> adminPermissions = Set.of("user:read", "user:write", "user:delete");
            Set<String> userPermissions = Set.of("user:read", "user:write");

            when(storage.findRolesByUserId("user-001")).thenReturn(roles);
            when(storage.findPermissionsByRole("ADMIN")).thenReturn(adminPermissions);
            when(storage.findPermissionsByRole("USER")).thenReturn(userPermissions);

            Set<String> result = rbacService.getPermissions("user-001", null);

            assertThat(result).containsExactlyInAnyOrder(
                    "user:read", "user:write", "user:delete");
        }

        @Test
        @DisplayName("应检查用户是否具有权限")
        void shouldCheckUserPermission() {
            Set<String> roles = Set.of("ADMIN");
            Set<String> adminPermissions = Set.of("user:read", "user:write", "user:delete");

            when(storage.findRolesByUserId("user-001")).thenReturn(roles);
            when(storage.findPermissionsByRole("ADMIN")).thenReturn(adminPermissions);

            assertThat(rbacService.hasPermission("user-001", "user:read", null)).isTrue();
            assertThat(rbacService.hasPermission("user-001", "user:delete", null)).isTrue();
            assertThat(rbacService.hasPermission("user-001", "role:read", null)).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有权限（带租户）")
        void shouldCheckUserPermissionWithTenant() {
            Set<String> roles = Set.of("ADMIN");
            Set<String> adminPermissions = Set.of("user:read", "user:write", "user:delete");

            when(storage.findRolesByUserId("user-001")).thenReturn(roles);
            when(storage.findPermissionsByRole("ADMIN")).thenReturn(adminPermissions);

            assertThat(rbacService.hasPermission("user-001", "user:read", "tenant-001")).isTrue();
            assertThat(rbacService.hasPermission("user-001", "role:read", "tenant-001")).isFalse();
        }

        @Test
        @DisplayName("无角色用户应返回空权限")
        void shouldReturnEmptyPermissionsForUserWithoutRoles() {
            when(storage.findRolesByUserId("user-002")).thenReturn(Set.of());

            Set<String> result = rbacService.getPermissions("user-002", null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("角色授权测试")
    class RoleGrantTests {

        @Test
        @DisplayName("应授予用户角色")
        void shouldGrantRole() {
            doNothing().when(storage).grantRole(anyString(), anyString());

            rbacService.grantRole("user-001", "ADMIN", null);

            verify(storage).grantRole("user-001", "ADMIN");
        }

        @Test
        @DisplayName("应授予用户角色（带租户）")
        void shouldGrantRoleWithTenant() {
            doNothing().when(storage).grantRole(anyString(), anyString());

            // 当前实现忽略 tenantId
            rbacService.grantRole("user-001", "ADMIN", "tenant-001");

            verify(storage).grantRole("user-001", "ADMIN");
        }

        @Test
        @DisplayName("应撤销用户角色")
        void shouldRevokeRole() {
            doNothing().when(storage).revokeRole(anyString(), anyString());

            rbacService.revokeRole("user-001", "ADMIN", null);

            verify(storage).revokeRole("user-001", "ADMIN");
        }

        @Test
        @DisplayName("应撤销用户角色（带租户）")
        void shouldRevokeRoleWithTenant() {
            doNothing().when(storage).revokeRole(anyString(), anyString());

            // 当前实现忽略 tenantId
            rbacService.revokeRole("user-001", "ADMIN", "tenant-001");

            verify(storage).revokeRole("user-001", "ADMIN");
        }
    }

    @Nested
    @DisplayName("权限授权测试")
    class PermissionGrantTests {

        @Test
        @DisplayName("应授予角色权限")
        void shouldGrantPermission() {
            doNothing().when(storage).grantPermission(anyString(), anyString());

            rbacService.grantPermission("ADMIN", "user:delete", null);

            verify(storage).grantPermission("ADMIN", "user:delete");
        }

        @Test
        @DisplayName("应授予角色权限（带租户）")
        void shouldGrantPermissionWithTenant() {
            doNothing().when(storage).grantPermission(anyString(), anyString());

            // 当前实现忽略 tenantId
            rbacService.grantPermission("ADMIN", "user:delete", "tenant-001");

            verify(storage).grantPermission("ADMIN", "user:delete");
        }

        @Test
        @DisplayName("应撤销角色权限")
        void shouldRevokePermission() {
            doNothing().when(storage).revokePermission(anyString(), anyString());

            rbacService.revokePermission("ADMIN", "user:delete", null);

            verify(storage).revokePermission("ADMIN", "user:delete");
        }

        @Test
        @DisplayName("应撤销角色权限（带租户）")
        void shouldRevokePermissionWithTenant() {
            doNothing().when(storage).revokePermission(anyString(), anyString());

            // 当前实现忽略 tenantId
            rbacService.revokePermission("ADMIN", "user:delete", "tenant-001");

            verify(storage).revokePermission("ADMIN", "user:delete");
        }
    }
}