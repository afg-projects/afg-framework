package io.github.afgprojects.framework.security.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.auth.permission.entity.SecResource;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcResourceService;

import static org.assertj.core.api.Assertions.*;

/**
 * ResourceController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock
    private JdbcResourceService resourceService;

    @Nested
    @DisplayName("资源 CRUD 测试")
    class ResourceCrudTests {

        @Test
        @DisplayName("创建资源")
        void shouldCreateResource() {
            ResourceController controller = new ResourceController(resourceService);

            SecResource resource = new SecResource();
            resource.setId(1L);
            resource.setResourceName("用户管理");
            resource.setResourceCode("user:manage");
            resource.setResourceType("MENU");

            when(resourceService.create(any())).thenReturn(resource);

            SecResource result = controller.create(resource);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getResourceName()).isEqualTo("用户管理");
        }

        @Test
        @DisplayName("获取资源详情")
        void shouldGetResourceById() {
            ResourceController controller = new ResourceController(resourceService);

            SecResource resource = new SecResource();
            resource.setId(1L);
            resource.setResourceName("用户管理");

            when(resourceService.findById(1L)).thenReturn(Optional.of(resource));

            SecResource result = controller.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getResourceName()).isEqualTo("用户管理");
        }

        @Test
        @DisplayName("获取资源列表")
        void shouldListResources() {
            ResourceController controller = new ResourceController(resourceService);

            SecResource r1 = new SecResource();
            r1.setId(1L);
            r1.setResourceName("用户管理");

            SecResource r2 = new SecResource();
            r2.setId(2L);
            r2.setResourceName("角色管理");

            when(resourceService.findAll("tenant-001")).thenReturn(List.of(r1, r2));

            List<SecResource> result = controller.list("tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("获取资源树")
        void shouldGetResourceTree() {
            ResourceController controller = new ResourceController(resourceService);

            SecResource parent = new SecResource();
            parent.setId(1L);
            parent.setResourceName("系统管理");

            when(resourceService.getResourceTree("tenant-001")).thenReturn(List.of(parent));

            List<SecResource> result = controller.tree("tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("按类型获取资源")
        void shouldListByType() {
            ResourceController controller = new ResourceController(resourceService);

            SecResource r1 = new SecResource();
            r1.setId(1L);
            r1.setResourceType("MENU");

            when(resourceService.findByType("MENU", "tenant-001")).thenReturn(List.of(r1));

            List<SecResource> result = controller.listByType("MENU", "tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("删除资源")
        void shouldDeleteResource() {
            ResourceController controller = new ResourceController(resourceService);

            controller.delete(1L);

            verify(resourceService).delete(1L);
        }
    }
}