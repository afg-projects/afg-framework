package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.auth.permission.entity.SecRole;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;

import static org.assertj.core.api.Assertions.*;

/**
 * UserPermissionController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserPermissionControllerTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private JdbcRoleService roleService;

    @Nested
    @DisplayName("用户权限查询测试")
    class UserPermissionQueryTests {

        @Test
        @DisplayName("获取用户权限列表")
        void shouldGetUserPermissions() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            when(rbacService.getPermissions("user-001", "tenant-001")).thenReturn(Set.of("user:create", "user:delete"));

            Set<String> result = controller.getPermissions("user-001", "tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("获取用户角色列表")
        void shouldGetUserRoles() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            when(rbacService.getRoles("user-001", "tenant-001")).thenReturn(Set.of("ADMIN", "USER"));

            Set<String> result = controller.getRoles("user-001", "tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("获取用户角色详情")
        void shouldGetUserRoleDetails() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");

            when(roleService.getUserRoles("user-001", "tenant-001")).thenReturn(List.of(role));

            List<SecRole> result = controller.getRoleDetails("user-001", "tenant-001");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("检查用户是否有权限")
        void shouldCheckUserHasPermission() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            when(rbacService.hasPermission("user-001", "user:create", "tenant-001")).thenReturn(true);

            boolean result = controller.hasPermission("user-001", "user:create", "tenant-001");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("检查用户是否有角色")
        void shouldCheckUserHasRole() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            when(rbacService.hasRole("user-001", "ADMIN", "tenant-001")).thenReturn(true);

            boolean result = controller.hasRole("user-001", "ADMIN", "tenant-001");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("用户角色分配测试")
    class UserRoleAssignmentTests {

        @Test
        @DisplayName("为用户分配角色")
        void shouldAssignRoleToUser() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            controller.assignRole("user-001", 1L, "tenant-001");

            verify(roleService).assignRoleToUser("user-001", 1L, "tenant-001");
        }

        @Test
        @DisplayName("移除用户角色")
        void shouldRemoveRoleFromUser() {
            UserPermissionController controller = new UserPermissionController(rbacService, roleService);

            controller.removeRole("user-001", 1L, "tenant-001");

            verify(roleService).removeRoleFromUser("user-001", 1L, "tenant-001");
        }
    }
}