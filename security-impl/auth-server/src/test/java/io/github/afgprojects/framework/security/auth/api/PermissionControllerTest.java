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

import io.github.afgprojects.framework.security.auth.permission.entity.SecPermission;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcResourceService;

import static org.assertj.core.api.Assertions.*;

/**
 * PermissionController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    @Mock
    private JdbcResourceService resourceService;

    @Nested
    @DisplayName("权限 CRUD 测试")
    class PermissionCrudTests {

        @Test
        @DisplayName("创建权限")
        void shouldCreatePermission() {
            PermissionController controller = new PermissionController(resourceService);

            SecPermission permission = new SecPermission();
            permission.setId(1L);
            permission.setPermissionName("创建用户");
            permission.setPermissionCode("user:create");

            when(resourceService.createPermission(any())).thenReturn(permission);

            SecPermission result = controller.create(permission);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPermissionName()).isEqualTo("创建用户");
        }

        @Test
        @DisplayName("按编码获取权限")
        void shouldGetPermissionByCode() {
            PermissionController controller = new PermissionController(resourceService);

            SecPermission permission = new SecPermission();
            permission.setId(1L);
            permission.setPermissionCode("user:create");
            permission.setPermissionName("创建用户");

            when(resourceService.findPermissionByCode("user:create", "tenant-001"))
                    .thenReturn(Optional.of(permission));

            SecPermission result = controller.getByCode("user:create", "tenant-001");

            assertThat(result).isNotNull();
            assertThat(result.getPermissionCode()).isEqualTo("user:create");
        }

        @Test
        @DisplayName("获取权限列表")
        void shouldListPermissions() {
            PermissionController controller = new PermissionController(resourceService);

            SecPermission p1 = new SecPermission();
            p1.setId(1L);
            p1.setPermissionCode("user:create");

            SecPermission p2 = new SecPermission();
            p2.setId(2L);
            p2.setPermissionCode("user:delete");

            when(resourceService.findAllPermissions("tenant-001"))
                    .thenReturn(List.of(p1, p2));

            List<SecPermission> result = controller.list("tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("按资源获取权限")
        void shouldListByResource() {
            PermissionController controller = new PermissionController(resourceService);

            SecPermission p1 = new SecPermission();
            p1.setId(1L);
            p1.setPermissionCode("user:create");

            when(resourceService.findPermissionsByResource(1L, "tenant-001"))
                    .thenReturn(List.of(p1));

            List<SecPermission> result = controller.listByResource(1L, "tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("删除权限")
        void shouldDeletePermission() {
            PermissionController controller = new PermissionController(resourceService);

            controller.delete(1L);

            verify(resourceService).delete(1L);
        }
    }
}