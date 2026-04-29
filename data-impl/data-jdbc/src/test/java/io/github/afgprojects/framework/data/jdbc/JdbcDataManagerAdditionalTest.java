package io.github.afgprojects.framework.data.jdbc;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcDataManager 额外方法测试
 * <p>
 * 测试 batchUpdate、queryForObject、queryForOptional、executeUpdate(Map) 等方法
 */
@DisplayName("JdbcDataManager 额外方法测试")
class JdbcDataManagerAdditionalTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    @Nested
    @DisplayName("batchUpdate 测试")
    class BatchUpdateTests {

        @BeforeEach
        void setUp() {
            createTestTable();
        }

        @Test
        @DisplayName("应正确执行批量更新")
        void shouldExecuteBatchUpdate() {
            // Given - 先插入一些数据
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('test1', 1)");
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('test2', 2)");
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('test3', 3)");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // When - 批量更新
            List<List<Object>> batchArgs = List.of(
                    List.of("updated1", 1L),
                    List.of("updated2", 2L),
                    List.of("updated3", 3L)
            );
            int[] results = dataManager.batchUpdate(
                    "UPDATE test_entity SET name = ? WHERE id = ?", batchArgs);

            // Then
            assertThat(results).hasSize(3);
            for (int r : results) {
                assertThat(r).isEqualTo(1);
            }

            // 验证更新结果
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM test_entity ORDER BY id")) {
                rs.next();
                assertThat(rs.getString("name")).isEqualTo("updated1");
                rs.next();
                assertThat(rs.getString("name")).isEqualTo("updated2");
                rs.next();
                assertThat(rs.getString("name")).isEqualTo("updated3");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("应正确执行批量插入")
        void shouldExecuteBatchInsert() {
            // When
            List<List<Object>> batchArgs = List.of(
                    List.of("batch1", 10),
                    List.of("batch2", 20),
                    List.of("batch3", 30)
            );
            int[] results = dataManager.batchUpdate(
                    "INSERT INTO test_entity (name, counter_value) VALUES (?, ?)", batchArgs);

            // Then
            assertThat(results).hasSize(3);

            // 验证插入结果
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_entity")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(3);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("queryForObject 测试")
    class QueryForObjectTests {

        @BeforeEach
        void setUp() {
            createTestTable();
        }

        @Test
        @DisplayName("应正确查询单个对象")
        void shouldQueryForObject() {
            // Given
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('test', 42)");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            RowMapper<TestEntity> rowMapper = (rs, rowNum) -> {
                TestEntity entity = new TestEntity();
                entity.setId(rs.getLong("id"));
                entity.setName(rs.getString("name"));
                entity.setCounterValue(rs.getInt("counter_value"));
                return entity;
            };

            // When
            TestEntity result = dataManager.queryForObject(
                    "SELECT * FROM test_entity WHERE name = ?",
                    List.of("test"),
                    rowMapper);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getCounterValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("查询无结果时应抛出异常")
        void shouldThrowWhenNoResult() {
            // Given
            RowMapper<TestEntity> rowMapper = (rs, rowNum) -> {
                TestEntity entity = new TestEntity();
                entity.setId(rs.getLong("id"));
                return entity;
            };

            // When/Then
            assertThatThrownBy(() -> dataManager.queryForObject(
                    "SELECT * FROM test_entity WHERE name = ?",
                    List.of("nonexistent"),
                    rowMapper))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("queryForOptional 测试")
    class QueryForOptionalTests {

        @BeforeEach
        void setUp() {
            createTestTable();
        }

        @Test
        @DisplayName("应正确查询存在的对象")
        void shouldQueryForPresentOptional() {
            // Given
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('optional', 100)");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            RowMapper<TestEntity> rowMapper = (rs, rowNum) -> {
                TestEntity entity = new TestEntity();
                entity.setId(rs.getLong("id"));
                entity.setName(rs.getString("name"));
                entity.setCounterValue(rs.getInt("counter_value"));
                return entity;
            };

            // When
            Optional<TestEntity> result = dataManager.queryForOptional(
                    "SELECT * FROM test_entity WHERE name = ?",
                    List.of("optional"),
                    rowMapper);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("optional");
            assertThat(result.get().getCounterValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("查询无结果时应返回空 Optional")
        void shouldReturnEmptyOptional() {
            // Given
            RowMapper<TestEntity> rowMapper = (rs, rowNum) -> {
                TestEntity entity = new TestEntity();
                entity.setId(rs.getLong("id"));
                return entity;
            };

            // When
            Optional<TestEntity> result = dataManager.queryForOptional(
                    "SELECT * FROM test_entity WHERE name = ?",
                    List.of("nonexistent"),
                    rowMapper);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("executeUpdate with Map 测试")
    class ExecuteUpdateWithMapTests {

        @BeforeEach
        void setUp() {
            createTestTable();
        }

        @Test
        @DisplayName("应正确使用命名参数执行更新")
        void shouldExecuteUpdateWithNamedParameters() {
            // Given
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_entity (name, counter_value) VALUES ('named', 1)");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // When
            int result = dataManager.executeUpdate(
                    "UPDATE test_entity SET counter_value = :value WHERE name = :name",
                    Map.of("value", 99, "name", "named"));

            // Then
            assertThat(result).isEqualTo(1);

            // 验证更新结果
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT counter_value FROM test_entity WHERE name = 'named'")) {
                rs.next();
                assertThat(rs.getInt("counter_value")).isEqualTo(99);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("应正确使用命名参数执行插入")
        void shouldExecuteInsertWithNamedParameters() {
            // When
            int result = dataManager.executeUpdate(
                    "INSERT INTO test_entity (name, counter_value) VALUES (:name, :value)",
                    Map.of("name", "inserted", "value", 123));

            // Then
            assertThat(result).isEqualTo(1);

            // 验证插入结果
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT counter_value FROM test_entity WHERE name = 'inserted'")) {
                rs.next();
                assertThat(rs.getInt("counter_value")).isEqualTo(123);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("getCacheManager 测试")
    class GetCacheManagerTests {

        @Test
        @DisplayName("未设置缓存管理器时应返回 null")
        void shouldReturnNullWhenNotSet() {
            // When
            var result = dataManager.getCacheManager();

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("数据库类型检测测试")
    class DatabaseTypeTests {

        @Test
        @DisplayName("应正确获取数据库类型")
        void shouldGetDatabaseType() {
            // Given - H2 数据库
            JdbcDataManager manager = new JdbcDataManager(dataSource);

            // When
            var dbType = manager.getDatabaseType();

            // Then
            assertThat(dbType).isNotNull();
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:additionaltest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_entity (id SERIAL PRIMARY KEY, name VARCHAR(100), counter_value INTEGER)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create table", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_entity");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体 ====================

    @Data
    @NoArgsConstructor
    static class TestEntity {
        private Long id;
        private String name;
        private Integer counterValue;
    }
}
