package io.github.afgprojects.framework.security.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC 数据权限服务测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class JdbcDataScopeServiceTest {

    private JdbcTemplate jdbcTemplate;
    private DataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_data_scope;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建表
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_data_scope");

        jdbcTemplate.execute("""
            CREATE TABLE auth_data_scope (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id VARCHAR(64) NOT NULL,
                tenant_id VARCHAR(64),
                scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL',
                scope_value VARCHAR(256),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
            )
            """);

        dataScopeService = new JdbcDataScopeService(jdbcTemplate);
    }

    @Nested
    @DisplayName("数据范围查询测试")
    class DataScopeQueryTests {

        @Test
        @DisplayName("应获取用户的数据范围")
        void shouldGetDataScope() {
            // given
            jdbcTemplate.update(
                "INSERT INTO auth_data_scope (id, user_id, scope_type, scope_value) VALUES (1, 'user-001', 'DEPT', 'dept-001')");

            // when
            DataScope scope = dataScopeService.getDataScope("user-001", null);

            // then
            assertThat(scope.scopeType()).isEqualTo(DataScopeType.DEPT);
            assertThat(scope.customCondition()).isEqualTo("dept-001");
        }

        @Test
        @DisplayName("用户无数据范围时应返回 ALL")
        void shouldReturnAllWhenNoDataScope() {
            // when
            DataScope scope = dataScopeService.getDataScope("user-no-scope", null);

            // then
            assertThat(scope.scopeType()).isEqualTo(DataScopeType.ALL);
        }

        @Test
        @DisplayName("应支持多租户场景")
        void shouldSupportMultiTenant() {
            // given
            jdbcTemplate.update(
                "INSERT INTO auth_data_scope (id, user_id, tenant_id, scope_type, scope_value) VALUES (1, 'user-001', 'tenant-001', 'DEPT', 'dept-001')");
            jdbcTemplate.update(
                "INSERT INTO auth_data_scope (id, user_id, tenant_id, scope_type, scope_value) VALUES (2, 'user-001', 'tenant-002', 'SELF', null)");

            // when
            DataScope scope1 = dataScopeService.getDataScope("user-001", "tenant-001");
            DataScope scope2 = dataScopeService.getDataScope("user-001", "tenant-002");

            // then
            assertThat(scope1.scopeType()).isEqualTo(DataScopeType.DEPT);
            assertThat(scope1.customCondition()).isEqualTo("dept-001");
            assertThat(scope2.scopeType()).isEqualTo(DataScopeType.SELF);
        }
    }

    @Nested
    @DisplayName("数据范围设置测试")
    class DataScopeSetTests {

        @Test
        @DisplayName("应设置用户的数据范围")
        void shouldSetDataScope() {
            // given
            DataScope scope = DataScope.builder()
                .scopeType(DataScopeType.DEPT)
                .customCondition("dept-001")
                .build();

            // when
            dataScopeService.setDataScope("user-001", null, scope);

            // then
            String scopeType = jdbcTemplate.queryForObject(
                "SELECT scope_type FROM auth_data_scope WHERE user_id = ?",
                String.class, "user-001");
            assertThat(scopeType).isEqualTo("DEPT");
        }

        @Test
        @DisplayName("应更新已存在的数据范围")
        void shouldUpdateExistingDataScope() {
            // given
            jdbcTemplate.update(
                "INSERT INTO auth_data_scope (id, user_id, scope_type, scope_value) VALUES (1, 'user-001', 'ALL', null)");
            DataScope newScope = DataScope.builder()
                .scopeType(DataScopeType.DEPT_AND_CHILD)
                .customCondition("dept-002")
                .build();

            // when
            dataScopeService.setDataScope("user-001", null, newScope);

            // then
            String scopeType = jdbcTemplate.queryForObject(
                "SELECT scope_type FROM auth_data_scope WHERE user_id = ?",
                String.class, "user-001");
            assertThat(scopeType).isEqualTo("DEPT_AND_CHILD");
        }
    }

    @Nested
    @DisplayName("数据范围移除测试")
    class DataScopeRemoveTests {

        @Test
        @DisplayName("应移除用户的数据范围")
        void shouldRemoveDataScope() {
            // given
            jdbcTemplate.update(
                "INSERT INTO auth_data_scope (id, user_id, scope_type, scope_value) VALUES (1, 'user-001', 'DEPT', 'dept-001')");

            // when
            dataScopeService.removeDataScope("user-001", null);

            // then
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_data_scope WHERE user_id = ?",
                Integer.class, "user-001");
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("移除不存在数据范围时应正常处理")
        void shouldHandleNonExistentDataScope() {
            // when & then - 不应抛出异常
            dataScopeService.removeDataScope("user-no-scope", null);
        }
    }
}
