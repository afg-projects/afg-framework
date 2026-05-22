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

import io.github.afgprojects.framework.security.auth.datascope.entity.SecDataScope;
import io.github.afgprojects.framework.security.auth.datascope.service.JdbcDataScopeService;

import static org.assertj.core.api.Assertions.*;

/**
 * DataScopeController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DataScopeControllerTest {

    @Mock
    private JdbcDataScopeService dataScopeService;

    @Nested
    @DisplayName("数据权限 CRUD 测试")
    class DataScopeCrudTests {

        @Test
        @DisplayName("创建数据权限")
        void shouldCreateDataScope() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.create(any())).thenReturn(scope);

            SecDataScope result = controller.create(scope);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getScopeName()).isEqualTo("本部门数据");
        }

        @Test
        @DisplayName("获取数据权限详情")
        void shouldGetDataScopeById() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.findById(1L)).thenReturn(java.util.Optional.of(scope));

            SecDataScope result = controller.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getScopeName()).isEqualTo("本部门数据");
        }

        @Test
        @DisplayName("获取数据权限列表")
        void shouldListDataScopes() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            SecDataScope s1 = new SecDataScope();
            s1.setId(1L);
            s1.setScopeName("全部数据");
            s1.setScopeCode("ALL_SCOPE");
            s1.setScopeType("ALL");

            SecDataScope s2 = new SecDataScope();
            s2.setId(2L);
            s2.setScopeName("本部门数据");
            s2.setScopeCode("DEPT_SCOPE");
            s2.setScopeType("DEPT");

            when(dataScopeService.findAll("tenant-001")).thenReturn(List.of(s1, s2));

            List<SecDataScope> result = controller.list("tenant-001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("删除数据权限")
        void shouldDeleteDataScope() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            controller.delete(1L);

            verify(dataScopeService).delete(1L);
        }
    }

    @Nested
    @DisplayName("用户数据权限测试")
    class UserDataScopeTests {

        @Test
        @DisplayName("获取用户数据权限")
        void shouldGetUserDataScopes() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            SecDataScope scope = new SecDataScope();
            scope.setId(1L);
            scope.setScopeName("本部门数据");
            scope.setScopeCode("DEPT_SCOPE");
            scope.setScopeType("DEPT");

            when(dataScopeService.getUserDataScopes("user-001", "tenant-001")).thenReturn(List.of(scope));

            List<SecDataScope> result = controller.userScopes("user-001", "tenant-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("设置用户数据权限")
        void shouldSetUserDataScopes() {
            DataScopeController controller = new DataScopeController(dataScopeService);

            Set<Long> scopeIds = Set.of(1L, 2L);
            controller.setUserScopes("user-001", scopeIds, "tenant-001");

            verify(dataScopeService).setUserDataScopes("user-001", scopeIds, "tenant-001");
        }
    }
}