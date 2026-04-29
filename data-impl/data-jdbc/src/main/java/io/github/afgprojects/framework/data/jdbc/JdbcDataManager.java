package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.dialect.*;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.scope.TenantScope;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.data.sql.builder.SqlDeleteBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlInsertBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlUpdateBuilderImpl;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 基于 Spring JdbcClient 的 DataManager 实现
 */
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
     * 实体缓存管理器（可选）
     */
    private @Nullable EntityCacheManager cacheManager;

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
        return new SimpleEntityMetadata<>(entityClass);
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
        doInTransaction(() -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        return doInTransaction(action);
    }

    /**
     * 执行事务的公共逻辑
     */
    private <T> T doInTransaction(Supplier<T> action) {
        return jdbcTemplate.execute((Connection con) -> {
            boolean originalAutoCommit = con.getAutoCommit();
            try {
                con.setAutoCommit(false);
                TransactionContextHolderImpl.setConnection(con);
                T result = action.get();
                con.commit();
                return result;
            } catch (Exception e) {
                con.rollback();
                throw new RuntimeException("Transaction failed", e);
            } finally {
                TransactionContextHolderImpl.clear();
                con.setAutoCommit(originalAutoCommit);
            }
        });
    }

    @Override
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        return jdbcTemplate.execute((Connection con) -> {
            boolean originalReadOnly = con.isReadOnly();
            try {
                con.setReadOnly(true);
                TransactionContextHolderImpl.setConnection(con);
                return action.get();
            } finally {
                TransactionContextHolderImpl.clear();
                con.setReadOnly(originalReadOnly);
            }
        });
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
        return dataSource;
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
        // 检查是否在事务中
        Connection txConn = TransactionContextHolderImpl.getConnection();
        if (txConn != null) {
            // 使用事务连接
            try (var pstmt = txConn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                pstmt.executeUpdate();
                try (var rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
                throw new RuntimeException("Failed to get generated key");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute insert and return key", e);
            }
        }

        // 非事务模式
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql)
            .params(params)
            .update(keyHolder);

        // 从 keys 列表中获取 id（兼容 H2 返回 Integer 的情况）
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

        throw new RuntimeException("Failed to get generated key");
    }

    /**
     * 执行批量插入并返回所有生成的主键
     * <p>
     * 使用多值 INSERT 语法（INSERT INTO ... VALUES (...), (...), ...）一次性插入多行数据，
     * 并获取所有生成的主键。此方法适用于支持多值 INSERT 和 RETURNING 的数据库（如 H2、PostgreSQL）。
     *
     * @param sql       多值 INSERT SQL（格式：INSERT INTO table (cols) VALUES (?,?), (?,?), ...）
     * @param params    参数列表（按顺序排列，每条记录的参数连续存放）
     * @param batchSize 批次大小（插入的记录数）
     * @return 生成的主键数组
     */
    public long[] executeBatchInsertAndReturnKeys(String sql, List<Object> params, int batchSize) {
        try (Connection conn = dataSource.getConnection();
             var pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            // 多值 INSERT：一次性设置所有参数，然后执行 executeUpdate
            // SQL 格式：INSERT INTO table (col1, col2) VALUES (?, ?), (?, ?), (?, ?)
            // 参数按顺序设置：p1, p2, p3, p4, p5, p6
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            // 执行多值 INSERT（不是 executeBatch，因为 SQL 已经包含多行 VALUES）
            pstmt.executeUpdate();

            // 获取生成的主键
            long[] keys = new long[batchSize];
            try (var rs = pstmt.getGeneratedKeys()) {
                int i = 0;
                while (rs.next() && i < batchSize) {
                    keys[i++] = rs.getLong(1);
                }
            }

            return keys;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute batch insert and return keys", e);
        }
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
            return mapProductNameToType(productName);
        } catch (SQLException e) {
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
     * 事务上下文持有者实现
     */
    static final class TransactionContextHolderImpl {
        private static final ThreadLocal<Connection> CONNECTION_HOLDER = new ThreadLocal<>();

        static void setConnection(Connection connection) {
            CONNECTION_HOLDER.set(connection);
        }

        static Connection getConnection() {
            return CONNECTION_HOLDER.get();
        }

        static void clear() {
            CONNECTION_HOLDER.remove();
        }

        static boolean isInTransaction() {
            return CONNECTION_HOLDER.get() != null;
        }
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
