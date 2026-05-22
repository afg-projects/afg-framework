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

import io.github.afgprojects.framework.security.auth.datascope.entity.SecDept;
import io.github.afgprojects.framework.security.auth.datascope.service.JdbcDeptService;

import static org.assertj.core.api.Assertions.*;

/**
 * DeptController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DeptControllerTest {

    @Mock
    private JdbcDeptService deptService;

    @Nested
    @DisplayName("部门 CRUD 测试")
    class DeptCrudTests {

        @Test
        @DisplayName("创建部门")
        void shouldCreateDept() {
            DeptController controller = new DeptController(deptService);

            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");
            dept.setTenantId("tenant-001");

            when(deptService.create(any())).thenReturn(dept);

            SecDept result = controller.create(dept);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDeptName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("获取部门详情")
        void shouldGetDeptById() {
            DeptController controller = new DeptController(deptService);

            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.findById(1L)).thenReturn(Optional.of(dept));

            SecDept result = controller.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getDeptName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("获取部门列表")
        void shouldListDepts() {
            DeptController controller = new DeptController(deptService);

            SecDept d1 = new SecDept();
            d1.setId(1L);
            d1.setDeptName("研发部");

            SecDept d2 = new SecDept();
            d2.setId(2L);
            d2.setDeptName("产品部");

            when(deptService.findAll("tenant-001")).thenReturn(List.of(d1, d2));

            List<SecDept> result = controller.list("tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("获取部门树")
        void shouldGetDeptTree() {
            DeptController controller = new DeptController(deptService);

            SecDept root = new SecDept();
            root.setId(1L);
            root.setDeptName("总公司");

            when(deptService.getDeptTree("tenant-001")).thenReturn(List.of(root));

            List<SecDept> result = controller.tree("tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("获取子部门 ID")
        void shouldGetChildIds() {
            DeptController controller = new DeptController(deptService);

            when(deptService.getChildDeptIds(1L, "tenant-001")).thenReturn(Set.of(2L, 3L));

            Set<Long> result = controller.childIds(1L, "tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("删除部门")
        void shouldDeleteDept() {
            DeptController controller = new DeptController(deptService);

            controller.delete(1L);

            verify(deptService).delete(1L);
        }
    }

    @Nested
    @DisplayName("用户部门关联测试")
    class UserDeptTests {

        @Test
        @DisplayName("获取用户部门列表")
        void shouldGetUserDepts() {
            DeptController controller = new DeptController(deptService);

            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.getUserDepts("user-001", "tenant-001")).thenReturn(List.of(dept));

            List<SecDept> result = controller.userDepts("user-001", "tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("获取用户主部门")
        void shouldGetPrimaryDept() {
            DeptController controller = new DeptController(deptService);

            SecDept dept = new SecDept();
            dept.setId(1L);
            dept.setDeptName("研发部");

            when(deptService.getPrimaryDept("user-001", "tenant-001")).thenReturn(Optional.of(dept));

            SecDept result = controller.primaryDept("user-001", "tenant-001");

            assertThat(result).isNotNull();
            assertThat(result.getDeptName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("设置用户部门")
        void shouldSetUserDept() {
            DeptController controller = new DeptController(deptService);

            controller.setUserDept("user-001", 1L, "tenant-001", true);

            verify(deptService).setUserDept("user-001", 1L, "tenant-001", true);
        }
    }
}