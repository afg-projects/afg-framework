package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.auth.permission.entity.SecRole;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;

import static org.assertj.core.api.Assertions.*;

/**
 * RoleController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private JdbcRoleService roleService;

    @Nested
    @DisplayName("角色 CRUD 测试")
    class RoleCrudTests {

        @Test
        @DisplayName("创建角色")
        void shouldCreateRole() {
            RoleController controller = new RoleController(roleService);

            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");
            role.setTenantId("tenant-001");

            when(roleService.create(any())).thenReturn(role);

            SecRole result = controller.create(role);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRoleName()).isEqualTo("管理员");
            assertThat(result.getRoleCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("获取角色详情")
        void shouldGetRoleById() {
            RoleController controller = new RoleController(roleService);

            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");

            when(roleService.findById(1L)).thenReturn(Optional.of(role));

            SecRole result = controller.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getRoleName()).isEqualTo("管理员");
        }

        @Test
        @DisplayName("获取角色列表")
        void shouldListRoles() {
            RoleController controller = new RoleController(roleService);

            SecRole role1 = new SecRole();
            role1.setId(1L);
            role1.setRoleName("管理员");

            SecRole role2 = new SecRole();
            role2.setId(2L);
            role2.setRoleName("普通用户");

            when(roleService.findAll("tenant-001")).thenReturn(List.of(role1, role2));

            List<SecRole> result = controller.list("tenant-001");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRoleName()).isEqualTo("管理员");
            assertThat(result.get(1).getRoleName()).isEqualTo("普通用户");
        }

        @Test
        @DisplayName("更新角色")
        void shouldUpdateRole() {
            RoleController controller = new RoleController(roleService);

            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("超级管理员");

            when(roleService.update(any())).thenReturn(role);

            SecRole result = controller.update(1L, role);

            assertThat(result.getRoleName()).isEqualTo("超级管理员");
            verify(roleService).update(argThat(r -> r.getId().equals(1L)));
        }

        @Test
        @DisplayName("删除角色")
        void shouldDeleteRole() {
            RoleController controller = new RoleController(roleService);

            controller.delete(1L);

            verify(roleService).delete(1L);
        }
    }

    @Nested
    @DisplayName("角色权限测试")
    class RolePermissionTests {

        @Test
        @DisplayName("设置角色权限")
        void shouldSetRolePermissions() {
            RoleController controller = new RoleController(roleService);

            Set<Long> permissionIds = Set.of(1L, 2L);
            controller.setPermissions(1L, permissionIds, "tenant-001");

            verify(roleService).setRolePermissions(1L, permissionIds, "tenant-001");
        }

        @Test
        @DisplayName("获取角色权限")
        void shouldGetRolePermissions() {
            RoleController controller = new RoleController(roleService);

            when(roleService.getRolePermissions(1L)).thenReturn(Set.of(1L, 2L));

            Set<Long> result = controller.getPermissions(1L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("角色继承测试")
    class RoleHierarchyTests {

        @Test
        @DisplayName("设置父角色")
        void shouldSetParentRole() {
            RoleController controller = new RoleController(roleService);

            controller.setParent(2L, 1L, "tenant-001");

            verify(roleService).setParentRole(2L, 1L, "tenant-001");
        }
    }
}
