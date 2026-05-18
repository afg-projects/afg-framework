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

import io.github.afgprojects.framework.security.permission.entity.SecResource;
import io.github.afgprojects.framework.security.permission.service.JdbcResourceService;

/**
 * ResourceController 集成测试
 */
@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock
    private JdbcResourceService resourceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceController controller = new ResourceController(resourceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("资源 CRUD 测试")
    class ResourceCrudTests {

        @Test
        @DisplayName("创建资源")
        void shouldCreateResource() throws Exception {
            SecResource resource = new SecResource();
            resource.setId(1L);
            resource.setResourceName("用户管理");
            resource.setResourceCode("user:manage");
            resource.setResourceType("MENU");

            when(resourceService.create(any())).thenReturn(resource);

            mockMvc.perform(post("/resources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "resourceName": "用户管理",
                            "resourceCode": "user:manage",
                            "resourceType": "MENU"
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.resourceName").value("用户管理"));
        }

        @Test
        @DisplayName("获取资源详情")
        void shouldGetResourceById() throws Exception {
            SecResource resource = new SecResource();
            resource.setId(1L);
            resource.setResourceName("用户管理");

            when(resourceService.findById(1L)).thenReturn(Optional.of(resource));

            mockMvc.perform(get("/resources/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceName").value("用户管理"));
        }

        @Test
        @DisplayName("获取资源列表")
        void shouldListResources() throws Exception {
            SecResource r1 = new SecResource();
            r1.setId(1L);
            r1.setResourceName("用户管理");

            SecResource r2 = new SecResource();
            r2.setId(2L);
            r2.setResourceName("角色管理");

            when(resourceService.findAll("tenant-001")).thenReturn(List.of(r1, r2));

            mockMvc.perform(get("/resources")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("获取资源树")
        void shouldGetResourceTree() throws Exception {
            SecResource parent = new SecResource();
            parent.setId(1L);
            parent.setResourceName("系统管理");

            when(resourceService.getResourceTree("tenant-001")).thenReturn(List.of(parent));

            mockMvc.perform(get("/resources/tree")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("按类型获取资源")
        void shouldListByType() throws Exception {
            SecResource r1 = new SecResource();
            r1.setId(1L);
            r1.setResourceType("MENU");

            when(resourceService.findByType("MENU", "tenant-001")).thenReturn(List.of(r1));

            mockMvc.perform(get("/resources/type/MENU")
                    .param("tenantId", "tenant-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("删除资源")
        void shouldDeleteResource() throws Exception {
            doNothing().when(resourceService).delete(1L);

            mockMvc.perform(delete("/resources/1"))
                    .andExpect(status().isOk());

            verify(resourceService).delete(1L);
        }
    }
}