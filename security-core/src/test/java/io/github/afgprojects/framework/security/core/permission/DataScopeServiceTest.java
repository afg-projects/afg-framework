package io.github.afgprojects.framework.security.core.permission;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataScopeService 接口测试。
 *
 * @since 1.0.0
 */
class DataScopeServiceTest {

    @Test
    @DisplayName("测试 DataScopeService 接口定义 - getDataScope")
    void testGetDataScope() {
        DataScopeService service = new TestDataScopeService();
        DataScope scope = service.getDataScope("user-001", null);
        assertNotNull(scope);
        assertEquals("sys_user", scope.table());
        assertEquals("dept_id", scope.column());
        assertEquals(DataScopeType.DEPT, scope.scopeType());
    }

    @Test
    @DisplayName("测试 DataScopeService 接口定义 - setDataScope")
    void testSetDataScope() {
        TestDataScopeService service = new TestDataScopeService();
        DataScope newScope = DataScope.of("sys_order", "org_id", DataScopeType.DEPT_AND_CHILD);
        service.setDataScope("user-002", null, newScope);
        DataScope scope = service.getDataScope("user-002", null);
        assertNotNull(scope);
        assertEquals("sys_order", scope.table());
        assertEquals(DataScopeType.DEPT_AND_CHILD, scope.scopeType());
    }

    @Test
    @DisplayName("测试 DataScopeService 接口定义 - removeDataScope")
    void testRemoveDataScope() {
        TestDataScopeService service = new TestDataScopeService();
        service.removeDataScope("user-001", null);
        assertThrows(IllegalStateException.class, () -> service.getDataScope("user-001", null));
    }

    @Test
    @DisplayName("测试 DataScopeService 接口定义 - 带租户ID")
    void testWithTenantId() {
        DataScopeService service = new TestDataScopeService();
        DataScope scope = service.getDataScope("user-001", "tenant-001");
        assertNotNull(scope);
        assertEquals("sys_user", scope.table());
    }

    /**
     * 测试用 DataScopeService 实现。
     */
    private static class TestDataScopeService implements DataScopeService {
        private final Map<String, DataScope> scopes = new HashMap<>();

        TestDataScopeService() {
            DataScope defaultScope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);
            scopes.put("user-001:null", defaultScope);
            scopes.put("user-001:tenant-001", defaultScope);
        }

        @Override
        public DataScope getDataScope(String userId, String tenantId) {
            String key = userId + ":" + (tenantId == null ? "null" : tenantId);
            DataScope scope = scopes.get(key);
            if (scope == null) {
                throw new IllegalStateException("DataScope not found for user: " + userId);
            }
            return scope;
        }

        @Override
        public Set<Long> getAccessibleDeptIds(String userId, String tenantId) {
            return Set.of(1L, 2L, 3L);
        }

        @Override
        public void setDataScope(String userId, String tenantId, DataScope scope) {
            String key = userId + ":" + (tenantId == null ? "null" : tenantId);
            scopes.put(key, scope);
        }

        @Override
        public void removeDataScope(String userId, String tenantId) {
            String key = userId + ":" + (tenantId == null ? "null" : tenantId);
            scopes.remove(key);
        }
    }
}
