package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.permission.entity.SecRole;
import io.github.afgprojects.framework.security.permission.service.JdbcRoleService;

/**
 * UserPermissionController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class UserPermissionControllerTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private JdbcRoleService roleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserPermissionController controller = new UserPermissionController(rbacService, roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("用户权限查询测试")
    class UserPermissionQueryTests {

        @Test
        @DisplayName("获取用户权限列表")
        void shouldGetUserPermissions() throws Exception {
            when(rbacService.getPermissions("user-001", "tenant-001")).thenReturn(Set.of("user:create", "user:delete"));

            mockMvc.perform(get("/user-permissions/user-001/permissions")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("获取用户角色列表")
        void shouldGetUserRoles() throws Exception {
            when(rbacService.getRoles("user-001", "tenant-001")).thenReturn(Set.of("ADMIN", "USER"));

            mockMvc.perform(get("/user-permissions/user-001/roles")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("获取用户角色详情")
        void shouldGetUserRoleDetails() throws Exception {
            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");

            when(roleService.getUserRoles("user-001", "tenant-001")).thenReturn(List.of(role));

            mockMvc.perform(get("/user-permissions/user-001/roles/detail")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].roleCode").value("ADMIN"));
        }

        @Test
        @DisplayName("检查用户是否有权限")
        void shouldCheckUserHasPermission() throws Exception {
            when(rbacService.hasPermission("user-001", "user:create", "tenant-001")).thenReturn(true);

            mockMvc.perform(get("/user-permissions/user-001/has-permission")
                    .param("permission", "user:create")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("检查用户是否有角色")
        void shouldCheckUserHasRole() throws Exception {
            when(rbacService.hasRole("user-001", "ADMIN", "tenant-001")).thenReturn(true);

            mockMvc.perform(get("/user-permissions/user-001/has-role")
                    .param("role", "ADMIN")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }
    }

    @Nested
    @DisplayName("用户角色分配测试")
    class UserRoleAssignmentTests {

        @Test
        @DisplayName("为用户分配角色")
        void shouldAssignRoleToUser() throws Exception {
            doNothing().when(roleService).assignRoleToUser("user-001", 1L, "tenant-001");

            mockMvc.perform(post("/user-permissions/user-001/roles/1")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk());

            verify(roleService).assignRoleToUser("user-001", 1L, "tenant-001");
        }

        @Test
        @DisplayName("移除用户角色")
        void shouldRemoveRoleFromUser() throws Exception {
            doNothing().when(roleService).removeRoleFromUser("user-001", 1L, "tenant-001");

            mockMvc.perform(delete("/user-permissions/user-001/roles/1")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk());

            verify(roleService).removeRoleFromUser("user-001", 1L, "tenant-001");
        }
    }
}