package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.afgprojects.framework.security.permission.entity.SecRole;
import io.github.afgprojects.framework.security.permission.service.JdbcRoleService;

/**
 * RoleController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private JdbcRoleService roleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RoleController controller = new RoleController(roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("角色 CRUD 测试")
    class RoleCrudTests {

        @Test
        @DisplayName("创建角色")
        void shouldCreateRole() throws Exception {
            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");
            role.setTenantId("tenant-001");

            when(roleService.create(any())).thenReturn(role);

            mockMvc.perform(post("/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roleName": "管理员",
                            "roleCode": "ADMIN",
                            "tenantId": "tenant-001"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.roleName").value("管理员"))
                    .andExpect(jsonPath("$.roleCode").value("ADMIN"));
        }

        @Test
        @DisplayName("获取角色详情")
        void shouldGetRoleById() throws Exception {
            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleCode("ADMIN");

            when(roleService.findById(1L)).thenReturn(Optional.of(role));

            mockMvc.perform(get("/roles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.roleName").value("管理员"));
        }

        @Test
        @DisplayName("获取角色列表")
        void shouldListRoles() throws Exception {
            SecRole role1 = new SecRole();
            role1.setId(1L);
            role1.setRoleName("管理员");

            SecRole role2 = new SecRole();
            role2.setId(2L);
            role2.setRoleName("普通用户");

            when(roleService.findAll("tenant-001")).thenReturn(List.of(role1, role2));

            mockMvc.perform(get("/roles")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].roleName").value("管理员"))
                    .andExpect(jsonPath("$[1].roleName").value("普通用户"));
        }

        @Test
        @DisplayName("更新角色")
        void shouldUpdateRole() throws Exception {
            SecRole role = new SecRole();
            role.setId(1L);
            role.setRoleName("超级管理员");

            when(roleService.update(any())).thenReturn(role);

            mockMvc.perform(put("/roles/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roleName": "超级管理员"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roleName").value("超级管理员"));
        }

        @Test
        @DisplayName("删除角色")
        void shouldDeleteRole() throws Exception {
            doNothing().when(roleService).delete(1L);

            mockMvc.perform(delete("/roles/1"))
                    .andExpect(status().isOk());

            verify(roleService).delete(1L);
        }
    }

    @Nested
    @DisplayName("角色权限测试")
    class RolePermissionTests {

        @Test
        @DisplayName("设置角色权限")
        void shouldSetRolePermissions() throws Exception {
            doNothing().when(roleService).setRolePermissions(1L, Set.of(1L, 2L), "tenant-001");

            mockMvc.perform(post("/roles/1/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[1, 2]")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk());

            verify(roleService).setRolePermissions(1L, Set.of(1L, 2L), "tenant-001");
        }

        @Test
        @DisplayName("获取角色权限")
        void shouldGetRolePermissions() throws Exception {
            when(roleService.getRolePermissions(1L)).thenReturn(Set.of(1L, 2L));

            mockMvc.perform(get("/roles/1/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("角色继承测试")
    class RoleHierarchyTests {

        @Test
        @DisplayName("设置父角色")
        void shouldSetParentRole() throws Exception {
            doNothing().when(roleService).setParentRole(2L, 1L, "tenant-001");

            mockMvc.perform(post("/roles/2/parent/1")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk());

            verify(roleService).setParentRole(2L, 1L, "tenant-001");
        }
    }
}