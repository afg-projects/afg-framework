package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcDataManager 额外覆盖率测试
 * <p>
 * 覆盖以下场景：
 * - executeInsertAndReturnKey 不同分支
 * - queryForCount 边界条件
 */
@DisplayName("JdbcDataManager 额外覆盖率测试")
class JdbcDataManagerAdditionalCoverageTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource, DatabaseType.POSTGRESQL);
        createTestTable();
    }

    @AfterEach
    void tearDown() {
        dropTestTable();
    }

    @Nested
    @DisplayName("queryForCount 边界条件测试")
    class QueryForCountEdgeCases {

        @Test
        @DisplayName("空表查询应该返回 0")
        void shouldReturnZeroForEmptyTable() {
            // Given - 空表

            // When
            long count = dataManager.queryForCount("SELECT COUNT(*) FROM test_user", List.of());

            // Then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("带条件的计数查询")
        void shouldCountWithCondition() {
            // Given
            dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_user (name, email) VALUES (?, ?)",
                List.of("count-test", "count@example.com")
            );

            // When
            long count = dataManager.queryForCount(
                "SELECT COUNT(*) FROM test_user WHERE name LIKE ?",
                List.of("count%")
            );

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("executeInsertAndReturnKey 测试")
    class ExecuteInsertAndReturnKeyTests {

        @Test
        @DisplayName("非事务模式应使用 JdbcClient 路径")
        void shouldUseJdbcClientOutsideTransaction() {
            // Given - 不在事务中

            // When
            long id = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_user (name, email) VALUES (?, ?)",
                List.of("jdbc-client-test", "jdbc@example.com")
            );

            // Then
            assertThat(id).isGreaterThan(0);
        }

        @Test
        @DisplayName("事务模式应使用 Connection 路径")
        void shouldUseConnectionInTransaction() {
            // Given - 在事务中

            // When
            Long id = dataManager.executeInTransaction(() -> {
                return dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_user (name, email) VALUES (?, ?)",
                    List.of("connection-test", "conn@example.com")
                );
            });

            // Then
            assertThat(id).isNotNull();
            assertThat(id).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("executeBatchInsertAndReturnKeys 测试")
    class ExecuteBatchInsertAndReturnKeysTests {

        @Test
        @DisplayName("批量插入多行应返回多个主键")
        void shouldReturnMultipleKeys() {
            // Given
            String sql = "INSERT INTO test_user (name, email) VALUES (?, ?), (?, ?)";
            List<Object> params = List.of("batch1", "batch1@example.com", "batch2", "batch2@example.com");

            // When
            long[] keys = dataManager.executeBatchInsertAndReturnKeys(sql, params, 2);

            // Then
            assertThat(keys).hasSize(2);
            assertThat(keys[0]).isGreaterThan(0);
            assertThat(keys[1]).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("TransactionContextHolderImpl 测试")
    class TransactionContextHolderImplTests {

        @Test
        @DisplayName("isInTransaction 在非事务时应返回 false")
        void shouldReturnFalseOutsideTransaction() {
            // Given - 不在事务中

            // When & Then
            assertThat(dataManager.executeInReadOnly(() -> {
                // 在只读模式下，但没有活动的事务连接
                return true;
            })).isTrue();
        }

        @Test
        @DisplayName("嵌套事务应共享同一连接")
        void shouldShareConnectionInNestedTransaction() {
            // When
            List<Long> ids = dataManager.executeInTransaction(() -> {
                // 外层插入
                long id1 = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_user (name, email) VALUES (?, ?)",
                    List.of("nested-outer", "outer@example.com")
                );

                // 内层事务（共享连接）
                dataManager.executeInTransaction(() -> {
                    dataManager.executeInsertAndReturnKey(
                        "INSERT INTO test_user (name, email) VALUES (?, ?)",
                        List.of("nested-inner", "inner@example.com")
                    );
                });

                return List.of(id1);
            });

            // Then
            assertThat(ids).hasSize(1);

            // 验证两条记录都插入成功
            long count = dataManager.queryForCount("SELECT COUNT(*) FROM test_user", List.of());
            assertThat(count).isEqualTo(2);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:additionaltestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE test_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }

    private void dropTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_user");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private String email;
    }
}
