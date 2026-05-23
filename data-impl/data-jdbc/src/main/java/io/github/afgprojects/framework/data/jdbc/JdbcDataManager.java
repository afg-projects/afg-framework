package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.dialect.*;
import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.scope.TenantScope;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import io.github.afgprojects.framework.data.core.transaction.TransactionException;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.sql.builder.SqlDeleteBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlInsertBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlUpdateBuilderImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 基于 Spring JdbcClient 的 DataManager 实现
 *
 * <p>事务管理完全依赖 Spring，与 @Transactional 注解兼容。
 */
@Slf4j
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class JdbcDataManager implements DataManager {

    private final DataSource dataSource;
    /**
     * -- GETTER --
     *  获取底层 JdbcClient
     */
    @Getter
    private final JdbcClient jdbcClient;
    /**
     * -- GETTER --
     *  获取底层 JdbcTemplate
     */
    @Getter
    private final JdbcTemplate jdbcTemplate;
    private final Dialect dialect;
    private final DatabaseType databaseType;
    private final TenantContextHolder tenantContextHolder = new TenantContextHolder();

    /**
     * 事务管理器（可选，用于 executeInTransaction 方法）
     */
    private @Nullable PlatformTransactionManager transactionManager;

    /**
     * 实体缓存管理器（可选）
     */
    private @Nullable EntityCacheManager cacheManager;

    /**
     * 事务适配器（可选）
     */
    private @Nullable TransactionAdapter transactionAdapter;

    /**
     * 实体元数据缓存
     */
    private final EntityMetadataCache metadataCache = new EntityMetadataCache();

    public JdbcDataManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.databaseType = detectDatabaseType(dataSource);
        this.dialect = createDialect(databaseType);
    }

    public JdbcDataManager(DataSource dataSource, DatabaseType databaseType) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.databaseType = databaseType;
        this.dialect = createDialect(databaseType);
    }

    /**
     * 设置事务管理器
     *
     * @param transactionManager Spring 事务管理器
     */
    public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 设置实体缓存管理器
     *
     * @param cacheManager 实体缓存管理器
     */
    public void setCacheManager(@Nullable EntityCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 获取实体缓存管理器
     *
     * @return 实体缓存管理器，可能为 null
     */
    public @Nullable EntityCacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    public <T> @NonNull EntityProxy<T> entity(@NonNull Class<T> entityClass) {
        return new JdbcEntityProxy<>(entityClass, jdbcClient, dialect, this, cacheManager);
    }

    @Override
    public <T> @NonNull EntityMetadata<T> getEntityMetadata(@NonNull Class<T> entityClass) {
        return metadataCache.get(entityClass);
    }

    @Override
    public @NonNull SqlQueryBuilder query() {
        return new SqlQueryBuilderImpl(dialect);
    }

    @Override
    public @NonNull SqlUpdateBuilder update() {
        return new SqlUpdateBuilderImpl();
    }

    @Override
    public @NonNull SqlInsertBuilder insert() {
        return new SqlInsertBuilderImpl();
    }

    @Override
    public @NonNull SqlDeleteBuilder delete() {
        return new SqlDeleteBuilderImpl();
    }

    @Override
    public void executeInTransaction(@NonNull Runnable action) {
        if (transactionManager == null) {
            throw new TransactionException("TransactionManager not configured. " +
                "Please call setTransactionManager() first.");
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        if (transactionManager == null) {
            throw new TransactionException("TransactionManager not configured. " +
                "Please call setTransactionManager() first.");
        }
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    @Override
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        if (transactionManager == null) {
            throw new TransactionException("TransactionManager not configured. " +
                "Please call setTransactionManager() first.");
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template.execute(status -> action.get());
    }

    @Override
    public @NonNull TenantScope tenantScope(@NonNull String tenantId) {
        return new SimpleTenantScope(tenantContextHolder, tenantId);
    }

    @Override
    public @NonNull TenantContextHolder getTenantContextHolder() {
        return tenantContextHolder;
    }

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public @NonNull Object getTransactionManager() {
        return transactionManager != null ? transactionManager : dataSource;
    }

    @Override
    public @Nullable TransactionAdapter getTransactionAdapter() {
        return transactionAdapter;
    }

    @Override
    public void setTransactionAdapter(@NonNull TransactionAdapter adapter) {
        this.transactionAdapter = adapter;
    }

    // ==================== 连接管理（集成 Spring 事务） ====================

    /**
     * 获取数据库连接
     * <p>
     * 优先使用 Spring 事务中的连接，确保与 @Transactional 兼容
     */
    Connection getConnection() throws SQLException {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Spring 事务中，使用 DataSourceUtils 获取连接（自动绑定到事务）
            return DataSourceUtils.getConnection(dataSource);
        }
        // 无事务，获取新连接
        return dataSource.getConnection();
    }

    /**
     * 释放数据库连接
     */
    void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Spring 事务中，由 Spring 管理
            DataSourceUtils.releaseConnection(conn, dataSource);
        } else {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查是否在事务中
     */
    boolean isInTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    // ==================== JdbcClient 便捷方法 ====================

    /**
     * 执行查询并返回列表
     */
    public <T> List<T> queryForList(String sql, List<Object> params, RowMapper<T> rowMapper) {
        return jdbcClient.sql(sql)
            .params(params)
            .query(rowMapper)
            .list();
    }

    /**
     * 执行查询并返回单个结果
     */
    public <T> T queryForObject(String sql, List<Object> params, RowMapper<T> rowMapper) {
        return jdbcClient.sql(sql)
            .params(params)
            .query(rowMapper)
            .single();
    }

    /**
     * 执行查询并返回可选单个结果
     */
    public <T> java.util.Optional<T> queryForOptional(String sql, List<Object> params, RowMapper<T> rowMapper) {
        return jdbcClient.sql(sql)
            .params(params)
            .query(rowMapper)
            .optional();
    }

    /**
     * 执行更新操作
     */
    public int executeUpdate(String sql, List<Object> params) {
        return jdbcClient.sql(sql)
            .params(params)
            .update();
    }

    /**
     * 执行更新操作并返回受影响的行数
     */
    public int executeUpdate(String sql, Map<String, Object> params) {
        return jdbcClient.sql(sql)
            .params(params)
            .update();
    }

    /**
     * 执行插入并返回生成的主键
     */
    public long executeInsertAndReturnKey(String sql, List<Object> params) {
        return isInTransaction()
                ? executeInsertInTransaction(sql, params)
                : executeInsertWithoutTransaction(sql, params);
    }

    /**
     * 在事务中执行插入并返回生成的主键
     */
    private long executeInsertInTransaction(String sql, List<Object> params) {
        try (var pstmt = getConnection().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setParameters(pstmt, params);
            pstmt.executeUpdate();
            return extractGeneratedKey(pstmt);
        } catch (SQLException e) {
            throw new EntityMappingException("Failed to execute insert and return key", null, e);
        }
        // 连接由 Spring 事务管理，无需关闭
    }

    /**
     * 非事务模式下执行插入并返回生成的主键
     */
    private long executeInsertWithoutTransaction(String sql, List<Object> params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql).params(params).update(keyHolder);

        // 优先从 keys 列表中获取 id（兼容 H2 返回 Integer 的情况）
        var keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number num) {
                return num.longValue();
            }
        }

        // 尝试获取单个主键（兼容其他数据库）
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }

        throw new EntityMappingException("Failed to get generated key");
    }

    /**
     * 执行批量插入并返回所有生成的主键
     *
     * @param sql       多值 INSERT SQL
     * @param params    参数列表
     * @param batchSize 批次大小
     * @return 生成的主键数组
     */
    public long[] executeBatchInsertAndReturnKeys(String sql, List<Object> params, int batchSize) {
        return isInTransaction()
                ? executeBatchInsertInTransaction(sql, params, batchSize)
                : executeBatchInsertWithoutTransaction(sql, params, batchSize);
    }

    /**
     * 在事务中执行批量插入并返回主键
     */
    private long[] executeBatchInsertInTransaction(String sql, List<Object> params, int batchSize) {
        try (var pstmt = getConnection().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setParameters(pstmt, params);
            pstmt.executeUpdate();
            return extractGeneratedKeys(pstmt, batchSize);
        } catch (SQLException e) {
            throw new EntityMappingException("Failed to execute batch insert and return keys", null, e);
        }
        // 连接由 Spring 事务管理，无需关闭
    }

    /**
     * 非事务模式下执行批量插入并返回主键
     */
    private long[] executeBatchInsertWithoutTransaction(String sql, List<Object> params, int batchSize) {
        try (Connection conn = dataSource.getConnection();
             var pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setParameters(pstmt, params);
            pstmt.executeUpdate();
            return extractGeneratedKeys(pstmt, batchSize);
        } catch (SQLException e) {
            throw new EntityMappingException("Failed to execute batch insert and return keys", null, e);
        }
    }

    /**
     * 设置 PreparedStatement 参数
     */
    private void setParameters(java.sql.PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    /**
     * 提取单个生成的主键
     */
    private long extractGeneratedKey(java.sql.PreparedStatement pstmt) throws SQLException {
        try (var rs = pstmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new EntityMappingException("Failed to get generated key");
    }

    /**
     * 提取批量生成的主键
     */
    private long[] extractGeneratedKeys(java.sql.PreparedStatement pstmt, int batchSize) throws SQLException {
        long[] keys = new long[batchSize];
        try (var rs = pstmt.getGeneratedKeys()) {
            int i = 0;
            while (rs.next() && i < batchSize) {
                keys[i++] = rs.getLong(1);
            }
        }
        return keys;
    }

    /**
     * 批量更新
     */
    public int[] batchUpdate(String sql, List<List<Object>> batchParams) {
        List<Object[]> args = new ArrayList<>();
        for (List<Object> params : batchParams) {
            args.add(params.toArray());
        }
        return jdbcTemplate.batchUpdate(sql, args);
    }

    /**
     * 查询计数
     */
    public long queryForCount(String sql, List<Object> params) {
        Long count = jdbcClient.sql(sql)
            .params(params)
            .query(Long.class)
            .single();
        return count != null ? count : 0L;
    }

    // ==================== 私有方法 ====================

    private DatabaseType detectDatabaseType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            DatabaseType type = mapProductNameToType(productName);
            log.debug("Detected database type: {} from product name: {}", type, productName);
            return type;
        } catch (SQLException e) {
            log.warn("Failed to detect database type, falling back to MYSQL. " +
                     "Consider explicitly setting database type via constructor. Error: {}", e.getMessage());
            return DatabaseType.MYSQL;
        }
    }

    private DatabaseType mapProductNameToType(String productName) {
        String name = productName.toUpperCase();
        if (name.contains("MYSQL")) return DatabaseType.MYSQL;
        if (name.contains("POSTGRESQL")) return DatabaseType.POSTGRESQL;
        if (name.contains("ORACLE")) return DatabaseType.ORACLE;
        if (name.contains("SQL SERVER")) return DatabaseType.SQLSERVER;
        if (name.contains("H2")) return DatabaseType.H2;
        if (name.contains("OCEANBASE")) return DatabaseType.OCEANBASE;
        if (name.contains("GAUSSDB")) return DatabaseType.GAUSSDB;
        if (name.contains("OPENGAUSS")) return DatabaseType.OPENGAUSS;
        if (name.contains("DM") || name.contains("达梦")) return DatabaseType.DM;
        if (name.contains("KINGBASE") || name.contains("金仓")) return DatabaseType.KINGBASE;
        return DatabaseType.MYSQL;
    }

    private Dialect createDialect(DatabaseType type) {
        return switch (type) {
            case MYSQL, OCEANBASE -> new MySQLDialect();
            case POSTGRESQL, OPENGAUSS, GAUSSDB, KINGBASE -> new PostgreSQLDialect();
            case ORACLE -> new OracleDialect();
            case H2 -> new H2Dialect();
            default -> new MySQLDialect();
        };
    }

    /**
     * 简单租户作用域实现
     */
    private static final class SimpleTenantScope implements TenantScope {
        private final TenantContextHolder holder;
        private final String tenantId;
        private final String previousTenantId;

        SimpleTenantScope(TenantContextHolder holder, String tenantId) {
            this.holder = holder;
            this.tenantId = tenantId;
            this.previousTenantId = holder.getTenantId();
            holder.setTenantId(tenantId);
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public void close() {
            if (previousTenantId != null) {
                holder.setTenantId(previousTenantId);
            } else {
                holder.clear();
            }
        }
    }
}
