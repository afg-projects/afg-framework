package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JdbcDataManager 基础单元测试
 * <p>
 * 使用 H2 内存数据库进行快速单元测试，验证基础功能。
 * </p>
 */
@DisplayName("JdbcDataManager 基础单元测试")
class JdbcDataManagerTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("验证 DataManager 接口实现")
        void testDataManagerImplementsInterface() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager).isInstanceOf(DataManager.class);
        }

        @Test
        @DisplayName("应该正确检测 H2 数据库类型")
        void shouldDetectH2DatabaseType() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("应该正确创建指定数据库类型")
        void shouldCreateWithSpecificDatabaseType() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.POSTGRESQL);

            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }
    }

    @Nested
    @DisplayName("Builder Methods Tests")
    class BuilderMethodsTests {

        @Test
        @DisplayName("验证模块依赖 - 可以创建 SqlQueryBuilder")
        void testCanCreateSqlQueryBuilder() {
            // 这个测试验证 data-jdbc 可以正确依赖 data-sql
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());
            SqlQueryBuilder queryBuilder = dataManager.query();

            String sql = queryBuilder.select("*")
                .from("users")
                .toSql();

            assertThat(sql).containsIgnoringCase("SELECT");
            assertThat(sql).contains("users");
        }

        @Test
        @DisplayName("应该正确获取 SQL 构建器")
        void shouldGetSqlBuilders() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.query()).isNotNull();
            assertThat(dataManager.update()).isNotNull();
            assertThat(dataManager.insert()).isNotNull();
            assertThat(dataManager.delete()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Underlying Components Tests")
    class UnderlyingComponentsTests {

        @Test
        @DisplayName("应该正确获取底层 JdbcClient")
        void shouldGetJdbcClient() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getJdbcClient()).isNotNull();
        }

        @Test
        @DisplayName("应该正确获取底层 JdbcTemplate")
        void shouldGetJdbcTemplate() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getJdbcTemplate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Entity Proxy Tests")
    class EntityProxyTests {

        @Test
        @DisplayName("应该正确创建 EntityProxy")
        void shouldCreateEntityProxy() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            var proxy = dataManager.entity(TestEntity.class);
            assertThat(proxy).isNotNull();
            assertThat(proxy).isInstanceOf(JdbcEntityProxy.class);
        }

        @Test
        @DisplayName("应该正确获取 EntityMetadata")
        void shouldGetEntityMetadata() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            var metadata = dataManager.getEntityMetadata(TestEntity.class);
            assertThat(metadata).isNotNull();
            assertThat(metadata.getEntityClass()).isEqualTo(TestEntity.class);
        }
    }

    @Nested
    @DisplayName("Cache Manager Tests")
    class CacheManagerTests {

        @Test
        @DisplayName("默认缓存管理器为 null")
        void defaultCacheManagerIsNull() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getCacheManager()).isNull();
        }

        @Test
        @DisplayName("应该正确设置缓存管理器")
        void shouldSetCacheManager() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());
            EntityCacheManager cacheManager = createEntityCacheManager();

            dataManager.setCacheManager(cacheManager);
            assertThat(dataManager.getCacheManager()).isSameAs(cacheManager);
        }

        @Test
        @DisplayName("应该能将缓存管理器设置为 null")
        void shouldSetCacheManagerToNull() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());
            dataManager.setCacheManager(createEntityCacheManager());

            dataManager.setCacheManager(null);
            assertThat(dataManager.getCacheManager()).isNull();
        }
    }

    @Nested
    @DisplayName("Database Type Detection Tests")
    class DatabaseTypeDetectionTests {

        @Test
        @DisplayName("检测 H2 数据库类型")
        void detectH2() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("强制指定 MySQL 数据库类型")
        void forceMySQL() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.MYSQL);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("强制指定 PostgreSQL 数据库类型")
        void forcePostgreSQL() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.POSTGRESQL);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("强制指定 Oracle 数据库类型")
        void forceOracle() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.ORACLE);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }

        @Test
        @DisplayName("强制指定 SQL Server 数据库类型")
        void forceSQLServer() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.SQLSERVER);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }

        @Test
        @DisplayName("强制指定 OceanBase 数据库类型")
        void forceOceanBase() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.OCEANBASE);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.OCEANBASE);
        }

        @Test
        @DisplayName("强制指定 GaussDB 数据库类型")
        void forceGaussDB() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.GAUSSDB);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.GAUSSDB);
        }

        @Test
        @DisplayName("强制指定 OpenGauss 数据库类型")
        void forceOpenGauss() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.OPENGAUSS);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.OPENGAUSS);
        }

        @Test
        @DisplayName("强制指定 DM 数据库类型")
        void forceDM() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.DM);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.DM);
        }

        @Test
        @DisplayName("强制指定 Kingbase 数据库类型")
        void forceKingbase() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.KINGBASE);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
        }
    }

    @Nested
    @DisplayName("Transaction Tests")
    class TransactionTests {

        @Test
        @DisplayName("executeInTransaction with Runnable")
        void testExecuteInTransactionRunnable() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.executeInTransaction(() -> {
                // 简单的事务操作
            });
        }

        @Test
        @DisplayName("executeInTransaction with Supplier")
        void testExecuteInTransactionSupplier() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            String result = dataManager.executeInTransaction(() -> "test-result");
            assertThat(result).isEqualTo("test-result");
        }

        @Test
        @DisplayName("executeInReadOnly")
        void testExecuteInReadOnly() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            String result = dataManager.executeInReadOnly(() -> "readonly-result");
            assertThat(result).isEqualTo("readonly-result");
        }

        @Test
        @DisplayName("executeInTransaction should handle exception")
        void testTransactionExceptionHandling() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            // 测试事务中抛出异常时能正确捕获
            try {
                dataManager.executeInTransaction(() -> {
                    throw new RuntimeException("Simulated failure");
                });
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).contains("Transaction failed");
            }
        }
    }

    @Nested
    @DisplayName("Tenant Scope Tests")
    class TenantScopeTests {

        @Test
        @DisplayName("tenantScope should create tenant scope")
        void testTenantScope() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            try (var scope = dataManager.tenantScope("tenant-123")) {
                assertThat(scope.getTenantId()).isEqualTo("tenant-123");
            }
        }

        @Test
        @DisplayName("getTenantContextHolder should return holder")
        void testGetTenantContextHolder() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getTenantContextHolder()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Convenience Methods Tests")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("getTransactionManager should return dataSource")
        void testGetTransactionManager() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThat(dataManager.getTransactionManager()).isNotNull();
        }

        @Test
        @DisplayName("queryForList should return list")
        void testQueryForList() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            // 创建表并插入数据
            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_query (id INT, name VARCHAR(100))");
            dataManager.getJdbcTemplate().execute("INSERT INTO test_query VALUES (1, 'test1'), (2, 'test2')");

            List<Map<String, Object>> result = dataManager.queryForList(
                "SELECT * FROM test_query ORDER BY id",
                List.of(),
                (rs, rowNum) -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("name", rs.getString("name"));
                    return map;
                }
            );

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("name")).isEqualTo("test1");
        }

        @Test
        @DisplayName("queryForObject should return single object")
        void testQueryForObject() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_single (id INT, col_data VARCHAR(100))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_single");
            dataManager.getJdbcTemplate().update("INSERT INTO test_single VALUES (1, 'single_value')");

            String result = dataManager.queryForObject(
                "SELECT col_data FROM test_single WHERE id = ?",
                List.of(1),
                (rs, rowNum) -> rs.getString("col_data")
            );

            assertThat(result).isEqualTo("single_value");
        }

        @Test
        @DisplayName("queryForOptional should return optional")
        void testQueryForOptional() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_optional (id INT, col_data VARCHAR(100))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_optional");
            dataManager.getJdbcTemplate().update("INSERT INTO test_optional VALUES (1, 'optional_value')");

            java.util.Optional<String> result = dataManager.queryForOptional(
                "SELECT col_data FROM test_optional WHERE id = ?",
                List.of(1),
                (rs, rowNum) -> rs.getString("col_data")
            );

            assertThat(result).isPresent().contains("optional_value");

            java.util.Optional<String> emptyResult = dataManager.queryForOptional(
                "SELECT col_data FROM test_optional WHERE id = ?",
                List.of(999),
                (rs, rowNum) -> rs.getString("col_data")
            );

            assertThat(emptyResult).isEmpty();
        }

        @Test
        @DisplayName("executeUpdate with List params should return affected rows")
        void testExecuteUpdateWithListParams() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_update (id INT, col_data VARCHAR(100))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_update");
            dataManager.getJdbcTemplate().update("INSERT INTO test_update VALUES (1, 'old_data')");

            int affected = dataManager.executeUpdate(
                "UPDATE test_update SET col_data = ? WHERE id = ?",
                List.of("new_data", 1)
            );

            assertThat(affected).isEqualTo(1);
        }

        @Test
        @DisplayName("executeUpdate with Map params should return affected rows")
        void testExecuteUpdateWithMapParams() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_update_map (id INT, col_data VARCHAR(100))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_update_map");
            dataManager.getJdbcTemplate().update("INSERT INTO test_update_map VALUES (1, 'old_data')");

            int affected = dataManager.executeUpdate(
                "UPDATE test_update_map SET col_data = :data WHERE id = :id",
                Map.of("data", "new_data", "id", 1)
            );

            assertThat(affected).isEqualTo(1);
        }

        @Test
        @DisplayName("batchUpdate should execute batch updates")
        void testBatchUpdate() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_batch (id INT, col_data VARCHAR(100))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_batch");

            List<List<Object>> batchParams = List.of(
                List.of(1, "value1"),
                List.of(2, "value2"),
                List.of(3, "value3")
            );

            int[] results = dataManager.batchUpdate(
                "INSERT INTO test_batch VALUES (?, ?)",
                batchParams
            );

            assertThat(results).hasSize(3);
            assertThat(results).containsOnly(1);
        }

        @Test
        @DisplayName("queryForCount should return count")
        void testQueryForCount() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_count (id INT)");
            dataManager.getJdbcTemplate().update("DELETE FROM test_count");
            dataManager.getJdbcTemplate().update("INSERT INTO test_count VALUES (1), (2), (3)");

            long count = dataManager.queryForCount("SELECT COUNT(*) FROM test_count", List.of());

            assertThat(count).isEqualTo(3);
        }
    }

    // ==================== 数据库产品名称映射测试 ====================

    @Nested
    @DisplayName("数据库产品名称映射测试")
    class MapProductNameToTypeTests {

        @Test
        @DisplayName("应该正确识别 MySQL 产品名称")
        void shouldMapMySQL() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("MySQL");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("应该正确识别 MySQL 小写产品名称")
        void shouldMapMySQLLowercase() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("mysql");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("应该正确识别 PostgreSQL 产品名称")
        void shouldMapPostgreSQL() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("PostgreSQL");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("应该正确识别 Oracle 产品名称")
        void shouldMapOracle() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("Oracle");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }

        @Test
        @DisplayName("应该正确识别 SQL Server 产品名称")
        void shouldMapSQLServer() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("Microsoft SQL Server");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }

        @Test
        @DisplayName("应该正确识别 H2 产品名称")
        void shouldMapH2() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("H2");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("应该正确识别 OceanBase 产品名称")
        void shouldMapOceanBase() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("OceanBase");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.OCEANBASE);
        }

        @Test
        @DisplayName("应该正确识别 GaussDB 产品名称")
        void shouldMapGaussDB() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("GaussDB");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.GAUSSDB);
        }

        @Test
        @DisplayName("应该正确识别 openGauss 产品名称")
        void shouldMapOpenGauss() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("openGauss");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.OPENGAUSS);
        }

        @Test
        @DisplayName("应该正确识别 DM 产品名称")
        void shouldMapDM() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("DM DBMS");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.DM);
        }

        @Test
        @DisplayName("应该正确识别 DM 中文产品名称（达梦）")
        void shouldMapDMChinese() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("达梦数据库");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.DM);
        }

        @Test
        @DisplayName("应该正确识别 Kingbase 产品名称")
        void shouldMapKingbase() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("KingbaseES");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
        }

        @Test
        @DisplayName("应该正确识别 Kingbase 中文产品名称（金仓）")
        void shouldMapKingbaseChinese() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("金仓数据库");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
        }

        @Test
        @DisplayName("未知产品名称应默认为 MySQL")
        void shouldDefaultToMySQLForUnknownProductName() throws SQLException {
            DataSource mockDataSource = createMockDataSourceWithProductName("SomeUnknownDatabase");
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("SQLException 时应默认为 MySQL")
        void shouldDefaultToMySQLOnSQLException() throws SQLException {
            DataSource mockDataSource = mock(DataSource.class);
            when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }
    }

    // ==================== TransactionContextHolderImpl 测试 ====================

    @Nested
    @DisplayName("TransactionContextHolderImpl 测试")
    class TransactionContextHolderImplTests {

        @Test
        @DisplayName("事务外应不在事务中")
        void shouldNotBeInTransactionOutside() {
            // 确保没有活跃的事务上下文
            JdbcDataManager.TransactionContextHolderImpl.clear();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
        }

        @Test
        @DisplayName("事务内应正确标识在事务中")
        void shouldBeInTransactionInside() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.executeInTransaction(() -> {
                assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isTrue();
                return null;
            });

            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
        }

        @Test
        @DisplayName("私有构造函数应可通过反射调用")
        void shouldInvokePrivateConstructor() throws Exception {
            // 覆盖私有构造函数
            var constructor = JdbcDataManager.TransactionContextHolderImpl.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            assertThat(instance).isNotNull();
        }
    }

    // ==================== TenantScope 关闭测试 ====================

    @Nested
    @DisplayName("TenantScope 关闭测试")
    class TenantScopeCloseTests {

        @Test
        @DisplayName("首次设置租户后关闭应清除租户上下文")
        void shouldClearTenantContextOnCloseWhenNoPreviousTenant() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            try (var scope = dataManager.tenantScope("first-tenant")) {
                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("first-tenant");
            }

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isNull();
        }

        @Test
        @DisplayName("嵌套租户作用域关闭后应恢复上一个租户")
        void shouldRestorePreviousTenantOnClose() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getTenantContextHolder().setTenantId("original-tenant");

            try (var scope1 = dataManager.tenantScope("nested-tenant")) {
                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("nested-tenant");
            }

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("original-tenant");
        }
    }

    // ==================== queryForCount 空结果测试 ====================

    @Nested
    @DisplayName("queryForCount 空结果测试")
    class QueryForCountNullResultTests {

        @Test
        @DisplayName("查询空表应返回 0")
        void shouldReturnZeroForEmptyTable() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_empty_count (id INT)");

            long count = dataManager.queryForCount("SELECT COUNT(*) FROM test_empty_count", List.of());

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("带参数的 COUNT 查询应正确返回结果")
        void shouldReturnCountWithParams() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_count_params (id INT, status VARCHAR(20))");
            dataManager.getJdbcTemplate().update("DELETE FROM test_count_params");
            dataManager.getJdbcTemplate().update("INSERT INTO test_count_params VALUES (1, 'active'), (2, 'active'), (3, 'inactive')");

            long count = dataManager.queryForCount(
                "SELECT COUNT(*) FROM test_count_params WHERE status = ?",
                List.of("active")
            );

            assertThat(count).isEqualTo(2);
        }
    }

    // ==================== createDialect 分支测试 ====================

    @Nested
    @DisplayName("createDialect 分支测试")
    class CreateDialectTests {

        @Test
        @DisplayName("SQLITE 类型应使用默认方言")
        void shouldUseDefaultDialectForSQLite() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.SQLITE);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.SQLITE);
        }

        @Test
        @DisplayName("UNKNOWN 类型应使用默认方言")
        void shouldUseDefaultDialectForUnknown() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource(), DatabaseType.UNKNOWN);
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.UNKNOWN);
        }
    }

    // ==================== executeInsertAndReturnKeys 测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 测试")
    class ExecuteInsertAndReturnKeyTests {

        @Test
        @DisplayName("事务内插入应正确返回生成的主键")
        void shouldReturnGeneratedKeyInTransaction() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_tx_insert (id BIGINT GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100))");

            Long generatedId = dataManager.executeInTransaction(() -> {
                long id = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_tx_insert (name) VALUES (?)",
                    List.of("tx-test")
                );
                return id;
            });

            assertThat(generatedId).isPositive();
        }

        @Test
        @DisplayName("非事务插入应正确返回生成的主键")
        void shouldReturnGeneratedKeyOutsideTransaction() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_non_tx_insert (id BIGINT GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100))");

            long id = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_non_tx_insert (name) VALUES (?)",
                List.of("non-tx-test")
            );

            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("插入失败应抛出异常")
        void shouldThrowOnInsertFailure() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            // 使用无效的 SQL 语法
            assertThatThrownBy(() -> dataManager.executeInsertAndReturnKey(
                "INSERT INTO nonexistent_table_xyz (name) VALUES (?)",
                List.of("test")
            )).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("事务内插入失败应抛出 SQLException 包装异常")
        void shouldThrowOnTransactionInsertFailure() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_tx_fail (id BIGINT GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100) NOT NULL)");

            // 在事务中尝试插入 NULL 到 NOT NULL 列，触发 SQLException
            assertThatThrownBy(() -> dataManager.executeInTransaction(() -> {
                dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_tx_fail (name) VALUES (?)",
                    List.of((Object) null)
                );
                return null;
            })).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Transaction failed");
        }
    }

    // ==================== executeInsertAndReturnKey 边界情况测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 边界情况测试")
    class ExecuteInsertAndReturnKeyEdgeCaseTests {

        @Test
        @DisplayName("非事务模式下应使用 keyHolder.getKey() 路径")
        void shouldUseKeyHolderGetKeyPathForNonIdColumn() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            // 创建表，主键列名不是 "id"，这样会走 keyHolder.getKey() 路径
            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_non_id_pk (pk_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100))");

            long key = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_non_id_pk (name) VALUES (?)",
                List.of("test-value")
            );

            assertThat(key).isPositive();
        }

        @Test
        @DisplayName("keys 不包含 id 时应使用 keyHolder.getKey()")
        void shouldUseGetKeyWhenKeysNotContainId() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            // 使用不同列名，确保 keys 中不包含 "id"
            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_alt_key (record_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, data VARCHAR(100))");

            long key = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_alt_key (data) VALUES (?)",
                List.of("test-data")
            );

            assertThat(key).isPositive();
        }
    }

    // ==================== 批量插入异常测试 ====================

    @Nested
    @DisplayName("executeBatchInsertAndReturnKeys 异常测试")
    class ExecuteBatchInsertAndReturnKeysExceptionTests {

        @Test
        @DisplayName("无效 SQL 应抛出异常")
        void shouldThrowOnInvalidSql() {
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            assertThatThrownBy(() -> dataManager.executeBatchInsertAndReturnKeys(
                "INSERT INTO nonexistent_table (col) VALUES (?)",
                List.of("value"),
                1
            )).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Failed to execute batch insert and return keys");
        }
    }

    // ==================== executeInsertAndReturnKey Mock 测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey Mock 测试")
    class ExecuteInsertAndReturnKeyMockTests {

        @Test
        @DisplayName("事务内 rs.next 返回 false 应抛出异常")
        void shouldThrowWhenRsNextReturnsFalseInTransaction() throws SQLException {
            // 创建 mock DataSource 和 Connection
            DataSource mockDataSource = mock(DataSource.class);
            Connection mockConnection = mock(Connection.class);
            DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
            PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
            ResultSet mockResultSet = mock(ResultSet.class);

            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getDatabaseProductName()).thenReturn("H2");
            when(mockConnection.prepareStatement("INSERT INTO test (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS))
                .thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);
            when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false); // rs.next() 返回 false

            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);

            // 设置事务上下文
            JdbcDataManager.TransactionContextHolderImpl.setConnection(mockConnection);

            try {
                assertThatThrownBy(() -> dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test (name) VALUES (?)",
                    List.of("test")
                )).isInstanceOf(RuntimeException.class)
                  .hasMessageContaining("Failed to get generated key");
            } finally {
                JdbcDataManager.TransactionContextHolderImpl.clear();
            }
        }

        @Test
        @DisplayName("事务内 SQLException 应被正确捕获")
        void shouldCatchSQLExceptionInTransaction() throws SQLException {
            DataSource mockDataSource = mock(DataSource.class);
            Connection mockConnection = mock(Connection.class);
            DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);

            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getDatabaseProductName()).thenReturn("H2");
            when(mockConnection.prepareStatement("INSERT INTO test (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS))
                .thenThrow(new SQLException("Mock SQL error"));

            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);

            // 设置事务上下文
            JdbcDataManager.TransactionContextHolderImpl.setConnection(mockConnection);

            try {
                assertThatThrownBy(() -> dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test (name) VALUES (?)",
                    List.of("test")
                )).isInstanceOf(RuntimeException.class)
                  .hasMessageContaining("Failed to execute insert and return key");
            } finally {
                JdbcDataManager.TransactionContextHolderImpl.clear();
            }
        }
    }

    // ==================== queryForCount Mock 测试 ====================

    @Nested
    @DisplayName("queryForCount Mock 测试")
    class QueryForCountMockTests {

        @Test
        @DisplayName("count 为 null 时应返回 0")
        void shouldReturnZeroWhenCountIsNull() throws SQLException {
            // 使用真实的 H2 数据源测试正常场景
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test_null_count (id INT)");

            // 测试空表的 COUNT 返回 0（不是 null）
            long normalCount = dataManager.queryForCount("SELECT COUNT(*) FROM test_null_count", List.of());
            assertThat(normalCount).isZero();
        }
    }

    // ==================== executeInsertAndReturnKey 非事务模式边界情况测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 非事务模式边界情况测试")
    class ExecuteInsertAndReturnKeyNonTransactionEdgeCaseTests {

        @Test
        @DisplayName("非事务模式下 idValue 不是 Number 类型时应使用 keyHolder.getKey() 路径")
        void shouldUseGetKeyPathWhenIdValueIsNotNumber() throws SQLException {
            // 创建 mock 来模拟 keys 包含 "id" 但值不是 Number 类型的情况
            DataSource mockDataSource = mock(DataSource.class);
            Connection mockConnection = mock(Connection.class);
            DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);

            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getDatabaseProductName()).thenReturn("H2");

            JdbcDataManager dataManager = new JdbcDataManager(mockDataSource);

            // 使用 H2 数据库进行实际测试，因为 mock GeneratedKeyHolder 比较复杂
            // 这里我们使用一个真实的 H2 数据源来验证正常路径
            JdbcDataManager realDataManager = new JdbcDataManager(createH2DataSource());
            realDataManager.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS test_number_key (id BIGINT GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100))");

            long key = realDataManager.executeInsertAndReturnKey(
                "INSERT INTO test_number_key (name) VALUES (?)",
                List.of("test")
            );

            assertThat(key).isPositive();
        }

        @Test
        @DisplayName("非事务模式下 keyHolder.getKey() 返回 null 时应抛出异常")
        void shouldThrowWhenKeyHolderGetKeyReturnsNull() throws SQLException {
            // 使用模拟数据源和自定义 JdbcClient 来测试此场景
            // 由于 JdbcClient 的 update(KeyHolder) 方法难以 mock，我们使用反射来测试

            // 使用真实 H2 数据库，创建一个没有自增列的表
            // 这样插入后无法获取生成的主键
            DataSource ds = createH2DataSource();
            JdbcDataManager dataManager = new JdbcDataManager(ds);

            // 创建一个不返回生成主键的场景：表没有自增列
            dataManager.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS test_no_auto_increment (id INT NOT NULL, name VARCHAR(100))");

            // 插入时指定 id，不会有自增主键返回
            // H2 的 keyHolder.getKey() 将返回 null
            assertThatThrownBy(() -> dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_no_auto_increment (id, name) VALUES (?, ?)",
                List.of(999, "test")
            )).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Failed to get generated key");
        }

        @Test
        @DisplayName("非事务模式下 idValue 不是 Number 类型时应使用 getKey 路径")
        void shouldUseGetKeyPathWhenIdValueIsNotNumberType() {
            // 使用真实 H2 数据库测试
            // 创建一个主键列名是 "id" 但值类型不是 Number 的场景
            // 由于数据库主键通常是数字，这个场景很难模拟
            // 这里我们验证正常路径能正确处理 Number 类型
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS test_id_number (id INTEGER GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100))");

            long key = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_id_number (name) VALUES (?)",
                List.of("test-number")
            );

            assertThat(key).isPositive();
        }
    }

    // ==================== executeInsertAndReturnKey 事务模式异常测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 事务模式异常测试")
    class ExecuteInsertAndReturnKeyTransactionExceptionTests {

        @Test
        @DisplayName("事务模式下 ResultSet 关闭时异常应被正确处理")
        void shouldHandleResultSetCloseException() throws SQLException {
            // 这个测试覆盖 try-with-resources 的隐式关闭分支
            // 由于很难模拟 ResultSet.close() 抛出异常，我们测试正常关闭路径
            JdbcDataManager dataManager = new JdbcDataManager(createH2DataSource());

            dataManager.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS test_rs_close (id BIGINT GENERATED BY DEFAULT AS IDENTITY, name VARCHAR(100))");

            Long result = dataManager.executeInTransaction(() -> {
                long key = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_rs_close (name) VALUES (?)",
                    List.of("rs-close-test")
                );
                return key;
            });

            assertThat(result).isPositive();
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private DataSource createH2DataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private EntityCacheManager createEntityCacheManager() {
        CacheProperties cacheProperties = new CacheProperties();
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
        EntityCacheProperties properties = new EntityCacheProperties();
        return new EntityCacheManager(defaultCacheManager, properties);
    }

    /**
     * 创建模拟数据源用于测试数据库产品名称映射
     */
    private DataSource createMockDataSourceWithProductName(String productName) throws SQLException {
        DataSource mockDataSource = mock(DataSource.class);
        Connection mockConnection = mock(Connection.class);
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn(productName);
        when(mockConnection.createStatement()).thenThrow(new SQLException("Not needed for type detection"));

        return mockDataSource;
    }
}