package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限安全测试
 */
@DisplayName("数据权限安全测试")
@Tag("security")
class DataPermissionSecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("租户隔离测试")
    class TenantIsolationTests {

        @Test
        @DisplayName("应正确设置租户隔离")
        void shouldSetTenantIsolation() {
            var scopedProxy = userProxy.withTenant("tenant-a");
            assertThat(scopedProxy).isNotNull();
        }

        @Test
        @DisplayName("应能通过条件查询过滤租户数据")
        void shouldFilterTenantDataByCondition() {
            // 使用条件查询过滤租户 A 的数据
            var results = userProxy.findAll(
                Conditions.eq("tenant_id", "tenant-a")
            );

            // tenant-a 有 2 条数据
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(u -> "tenant-a".equals(u.getTenantId()));
        }

        @Test
        @DisplayName("应能通过条件查询过滤租户 B 的数据")
        void shouldFilterTenantBDataByCondition() {
            // 使用条件查询过滤租户 B 的数据
            var results = userProxy.findAll(
                Conditions.eq("tenant_id", "tenant-b")
            );

            // tenant-b 有 1 条数据
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTenantId()).isEqualTo("tenant-b");
        }
    }

    @Nested
    @DisplayName("数据作用域测试")
    class DataScopeTests {

        @Test
        @DisplayName("应正确设置数据作用域")
        void shouldSetDataScope() {
            DataScope scope = DataScope.of(
                "security_test_user",
                "dept_id",
                DataScopeType.DEPT
            );

            var scopedProxy = userProxy.withDataScope(scope);
            assertThat(scopedProxy).isNotNull();
        }

        @Test
        @DisplayName("应正确设置多个数据作用域")
        void shouldSetMultipleDataScopes() {
            DataScope scope1 = DataScope.of("security_test_user", "dept_id", DataScopeType.DEPT);
            DataScope scope2 = DataScope.of("security_test_user", "tenant_id", DataScopeType.SELF);

            var scopedProxy = userProxy.withDataScopes(scope1, scope2);
            assertThat(scopedProxy).isNotNull();
        }
    }

    @Nested
    @DisplayName("只读模式测试")
    class ReadOnlyTests {

        @Test
        @DisplayName("应正确设置只读模式")
        void shouldSetReadOnly() {
            var readOnlyProxy = userProxy.withReadOnly();
            assertThat(readOnlyProxy).isNotNull();
        }
    }

    @Nested
    @DisplayName("条件查询安全测试")
    class ConditionSecurityTests {

        @Test
        @DisplayName("IN 条件应正确处理")
        void shouldHandleInCondition() {
            var results = userProxy.findAll(
                Conditions.in("name", java.util.List.of("alice", "bob"))
            );
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("NOT IN 条件应正确处理")
        void shouldHandleNotInCondition() {
            var results = userProxy.findAll(
                Conditions.builder().notIn("name", java.util.List.of("alice")).build()
            );
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("BETWEEN 条件应正确处理")
        void shouldHandleBetweenCondition() {
            var results = userProxy.findAll(
                Conditions.builder().between("id", 1L, 2L).build()
            );
            assertThat(results).hasSize(2);
        }
    }
}
