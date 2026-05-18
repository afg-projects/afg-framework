package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

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

import io.github.afgprojects.framework.security.permission.entity.SecPermission;
import io.github.afgprojects.framework.security.permission.service.JdbcResourceService;

/**
 * PermissionController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    @Mock
    private JdbcResourceService resourceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PermissionController controller = new PermissionController(resourceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("权限 CRUD 测试")
    class PermissionCrudTests {

        @Test
        @DisplayName("创建权限")
        void shouldCreatePermission() throws Exception {
            SecPermission permission = new SecPermission();
            permission.setId(1L);
            permission.setPermissionName("创建用户");
            permission.setPermissionCode("user:create");

            when(resourceService.createPermission(any())).thenReturn(permission);

            mockMvc.perform(post("/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "permissionName": "创建用户",
                            "permissionCode": "user:create"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.permissionName").value("创建用户"));
        }

        @Test
        @DisplayName("按编码获取权限")
        void shouldGetPermissionByCode() throws Exception {
            SecPermission permission = new SecPermission();
            permission.setId(1L);
            permission.setPermissionCode("user:create");
            permission.setPermissionName("创建用户");

            when(resourceService.findPermissionByCode("user:create", "tenant-001"))
                    .thenReturn(Optional.of(permission));

            mockMvc.perform(get("/permissions/user:create")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissionCode").value("user:create"));
        }

        @Test
        @DisplayName("获取权限列表")
        void shouldListPermissions() throws Exception {
            SecPermission p1 = new SecPermission();
            p1.setId(1L);
            p1.setPermissionCode("user:create");

            SecPermission p2 = new SecPermission();
            p2.setId(2L);
            p2.setPermissionCode("user:delete");

            when(resourceService.findAllPermissions("tenant-001"))
                    .thenReturn(List.of(p1, p2));

            mockMvc.perform(get("/permissions")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("按资源获取权限")
        void shouldListByResource() throws Exception {
            SecPermission p1 = new SecPermission();
            p1.setId(1L);
            p1.setPermissionCode("user:create");

            when(resourceService.findPermissionsByResource(1L, "tenant-001"))
                    .thenReturn(List.of(p1));

            mockMvc.perform(get("/permissions/resource/1")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("删除权限")
        void shouldDeletePermission() throws Exception {
            doNothing().when(resourceService).delete(1L);

            mockMvc.perform(delete("/permissions/1"))
                    .andExpect(status().isOk());

            verify(resourceService).delete(1L);
        }
    }
}