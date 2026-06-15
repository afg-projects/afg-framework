package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.core.api.id.IdGenerator;
import io.github.afgprojects.framework.data.core.event.EntityChangedEventPublisher;
import io.github.afgprojects.framework.data.core.event.NoOpEntityChangedEventPublisher;
import io.github.afgprojects.framework.data.core.safety.FullTableOperationChecker;
import io.github.afgprojects.framework.data.core.safety.NoOpFullTableOperationChecker;
import io.github.afgprojects.framework.data.jdbc.metrics.RawSqlSecurityGuard;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.dialect.*;
import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.entity.AuditableContext;
import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import io.github.afgprojects.framework.data.core.entity.NoOpAuditableContext;
import io.github.afgprojects.framework.data.core.entity.NoOpFieldEncryptor;
import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
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
import io.github.afgprojects.framework.data.jdbc.mapper.DtoMapper;
import io.github.afgprojects.framework.data.jdbc.mapper.ResultMapperAdapter;
import io.github.afgprojects.framework.data.sql.builder.SqlDeleteBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlInsertBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import io.github.afgprojects.framework.data.sql.builder.SqlUpdateBuilderImpl;
import io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider;
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
import java.util.Optional;
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
    /**
     * 租户上下文持有者（可注入，支持与 TenantContextAutoConfiguration 创建的 Bean 共享）
     */
    private TenantContextHolder tenantContextHolder;

    /**
     * 事务管理器（可选，用于 executeInTransaction 方法）
     */
    private volatile @Nullable PlatformTransactionManager transactionManager;

    /**
     * 缓存的 TransactionTemplate 实例，避免每次调用 executeInTransaction 时重复创建。
     * 当 transactionManager 变更时，通过 setTransactionManager() 清除缓存。
     */
    private volatile @Nullable TransactionTemplate cachedTransactionTemplate;

    /**
     * 缓存的只读 TransactionTemplate 实例
     */
    private volatile @Nullable TransactionTemplate cachedReadOnlyTransactionTemplate;

    /**
     * 实体缓存管理器（可选）
     */
    private volatile @Nullable EntityCacheManager cacheManager;

    /**
     * 事务适配器（可选）
     */
    private volatile @Nullable TransactionAdapter transactionAdapter;

    /**
     * 实体元数据缓存
     */
    private final RawSqlSecurityGuard rawSqlSecurityGuard = new RawSqlSecurityGuard();

    /**
     * 实体元数据缓存
     */
    private final EntityMetadataCache metadataCache = new EntityMetadataCache();

    /**
     * 数据权限上下文提供者（可选）
     */
    private volatile @Nullable DataScopeContextProvider dataScopeContextProvider;

    /**
     * 审计上下文（可注入，支持与 AuditableContext Bean 共享）
     */
    private AuditableContext auditableContext;

    /**
     * 字段加密器（可注入，支持与 FieldEncryptor Bean 共享）
     */
    private FieldEncryptor fieldEncryptor;

    /**
     * 盲索引提供者（可注入，用于加密字段的盲索引值计算）
     */
    private BlindIndexProvider blindIndexProvider;

    /**
     * ID 生成器（可选，来自 core 模块的 SPI）
     * <p>
     * 当 IdGenerator 存在时，插入实体前预生成 ID（如 Snowflake ID），
     * 否则使用数据库自增 ID（向后兼容）。
     */
    private volatile @Nullable IdGenerator idGenerator;

    /**
     * 实体变更事件发布器（可选）
     * <p>
     * 用于在 DataManager 执行保存、更新、删除、恢复操作后发布实体变更事件。
     * 默认使用 NoOp 实现（不发布任何事件）。
     */
    private EntityChangedEventPublisher entityChangedEventPublisher;

    /**
     * 全表操作检查器（可选）
     * <p>
     * 用于在条件更新/删除操作前检查条件是否为空（全表操作），
     * 根据策略阻止、限制或警告。默认使用 NoOp 实现（不检查）。
     */
    private FullTableOperationChecker fullTableOperationChecker;

    /**
     * 类型处理器注册表
     */
    private volatile TypeHandlerRegistry typeHandlerRegistry = TypeHandlerRegistry.defaultRegistry();

    public JdbcDataManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.databaseType = detectDatabaseType(dataSource);
        this.dialect = createDialect(databaseType);
        this.tenantContextHolder = new TenantContextHolder();
        this.auditableContext = new NoOpAuditableContext();
        this.fieldEncryptor = new NoOpFieldEncryptor();
        this.entityChangedEventPublisher = new NoOpEntityChangedEventPublisher();
        this.fullTableOperationChecker = new NoOpFullTableOperationChecker();
    }

    public JdbcDataManager(DataSource dataSource, DatabaseType databaseType) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.databaseType = databaseType;
        this.dialect = createDialect(databaseType);
        this.tenantContextHolder = new TenantContextHolder();
        this.auditableContext = new NoOpAuditableContext();
        this.fieldEncryptor = new NoOpFieldEncryptor();
        this.entityChangedEventPublisher = new NoOpEntityChangedEventPublisher();
        this.fullTableOperationChecker = new NoOpFullTableOperationChecker();
    }

    /**
     * 设置事务管理器
     *
     * @param transactionManager Spring 事务管理器
     */
    public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        // 清除缓存的 TransactionTemplate，下次使用时重新创建
        this.cachedTransactionTemplate = null;
        this.cachedReadOnlyTransactionTemplate = null;
    }

    /**
     * 设置租户上下文持有者
     * <p>
     * 注入与 {@code TenantContextAutoConfiguration} 创建的同一实例，
     * 确保多租户上下文在整个应用中一致。
     * 如果不设置，使用构造函数中创建的默认实例。
     *
     * @param tenantContextHolder 租户上下文持有者
     */
    public void setTenantContextHolder(@NonNull TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * 设置审计上下文
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保审计用户信息在整个应用中一致。
     * 如果不设置，使用构造函数中创建的 NoOp 默认实例。
     *
     * @param auditableContext 审计上下文
     */
    public void setAuditableContext(@NonNull AuditableContext auditableContext) {
        this.auditableContext = auditableContext;
    }

    /**
     * 获取审计上下文
     *
     * @return 审计上下文
     */
    public AuditableContext getAuditableContext() {
        return auditableContext;
    }

    /**
     * 设置字段加密器
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保加密/解密行为在整个应用中一致。
     * 如果不设置，使用构造函数中创建的 NoOp 默认实例（不加密）。
     *
     * @param fieldEncryptor 字段加密器
     */
    public void setFieldEncryptor(@NonNull FieldEncryptor fieldEncryptor) {
        this.fieldEncryptor = fieldEncryptor;
    }

    /**
     * 获取字段加密器
     *
     * @return 字段加密器
     */
    public FieldEncryptor getFieldEncryptor() {
        return fieldEncryptor;
    }

    /**
     * 设置盲索引提供者
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保盲索引计算在 INSERT/UPDATE 中使用相同的密钥。
     * 如果不设置，不计算盲索引值（向后兼容）。
     *
     * @param blindIndexProvider 盲索引提供者
     */
    public void setBlindIndexProvider(@Nullable BlindIndexProvider blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    /**
     * 获取盲索引提供者
     *
     * @return 盲索引提供者，可能为 null
     */
    public BlindIndexProvider getBlindIndexProvider() {
        return blindIndexProvider;
    }

    /**
     * 设置 ID 生成器
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保分布式 ID 在整个应用中一致。
     * 如果不设置，使用数据库自增 ID（向后兼容）。
     *
     * @param idGenerator ID 生成器
     */
    public void setIdGenerator(@Nullable IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 获取 ID 生成器
     *
     * @return ID 生成器，可能为 null（未配置时使用数据库自增）
     */
    public @Nullable IdGenerator getIdGenerator() {
        return idGenerator;
    }

    /**
     * 设置实体变更事件发布器
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保实体变更事件在整个应用中一致。
     * 如果不设置，使用 NoOp 默认实例（不发布事件）。
     *
     * @param publisher 实体变更事件发布器
     */
    public void setEntityChangedEventPublisher(@NonNull EntityChangedEventPublisher publisher) {
        this.entityChangedEventPublisher = publisher;
    }

    /**
     * 获取实体变更事件发布器
     *
     * @return 实体变更事件发布器
     */
    public EntityChangedEventPublisher getEntityChangedEventPublisher() {
        return entityChangedEventPublisher;
    }

    /**
     * 设置全表操作检查器
     * <p>
     * 注入与 AutoConfiguration 创建的同一实例，
     * 确保全表操作保护在整个应用中一致。
     * 如果不设置，使用 NoOp 默认实例（不检查）。
     *
     * @param checker 全表操作检查器
     */
    public void setFullTableOperationChecker(@NonNull FullTableOperationChecker checker) {
        this.fullTableOperationChecker = checker;
    }

    /**
     * 获取全表操作检查器
     *
     * @return 全表操作检查器
     */
    public FullTableOperationChecker getFullTableOperationChecker() {
        return fullTableOperationChecker;
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
        TransactionTemplate template = getOrCreateTransactionTemplate();
        template.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        TransactionTemplate template = getOrCreateTransactionTemplate();
        return template.execute(status -> action.get());
    }

    @Override
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        TransactionTemplate template = getOrCreateReadOnlyTransactionTemplate();
        return template.execute(status -> action.get());
    }

    /**
     * 获取或创建 TransactionTemplate 实例（双重检查锁定）
     */
    private TransactionTemplate getOrCreateTransactionTemplate() {
        if (transactionManager == null) {
            throw new TransactionException("TransactionManager not configured. " +
                "Please call setTransactionManager() first.");
        }
        TransactionTemplate template = cachedTransactionTemplate;
        if (template == null) {
            synchronized (this) {
                template = cachedTransactionTemplate;
                if (template == null) {
                    template = new TransactionTemplate(transactionManager);
                    cachedTransactionTemplate = template;
                }
            }
        }
        return template;
    }

    /**
     * 获取或创建只读 TransactionTemplate 实例（双重检查锁定）
     */
    private TransactionTemplate getOrCreateReadOnlyTransactionTemplate() {
        if (transactionManager == null) {
            throw new TransactionException("TransactionManager not configured. " +
                "Please call setTransactionManager() first.");
        }
        TransactionTemplate template = cachedReadOnlyTransactionTemplate;
        if (template == null) {
            synchronized (this) {
                template = cachedReadOnlyTransactionTemplate;
                if (template == null) {
                    template = new TransactionTemplate(transactionManager);
                    template.setReadOnly(true);
                    cachedReadOnlyTransactionTemplate = template;
                }
            }
        }
        return template;
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
    public @Nullable Object getTransactionManager() {
        return transactionManager;
    }

    @Override
    public @Nullable TransactionAdapter getTransactionAdapter() {
        return transactionAdapter;
    }

    @Override
    public void setTransactionAdapter(@NonNull TransactionAdapter adapter) {
        this.transactionAdapter = adapter;
    }

    // ==================== DataManager 原始 SQL 操作接口实现 ====================

    @Override
    public <T> List<T> queryForList(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.queryForList");
        return jdbcClient.sql(sql)
            .params(params)
            .query(ResultMapperAdapter.adapt(rowMapper))
            .list();
    }

    @Override
    public <T> @Nullable T queryForObject(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.queryForObject");
        return jdbcClient.sql(sql)
            .params(params)
            .query(ResultMapperAdapter.adapt(rowMapper))
            .single();
    }

    @Override
    public <T> Optional<T> queryForOptional(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.queryForOptional");
        return jdbcClient.sql(sql)
            .params(params)
            .query(ResultMapperAdapter.adapt(rowMapper))
            .optional();
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
     * 执行更新操作
     */
    @Override
    public int executeUpdate(String sql, List<Object> params) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.executeUpdate");
        return jdbcClient.sql(sql)
            .params(params)
            .update();
    }

    /**
     * 执行更新操作（命名参数）
     */
    @Override
    public int executeUpdate(String sql, Map<String, Object> params) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.executeUpdate(Map)");
        return jdbcClient.sql(sql)
            .params(params)
            .update();
    }

    /**
     * 执行插入并返回生成的主键
     */
    public long executeInsertAndReturnKey(String sql, List<Object> params) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.executeInsertAndReturnKey");
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
    @Override
    public long queryForCount(String sql, List<Object> params) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.queryForCount");
        Long count = jdbcClient.sql(sql)
            .params(params)
            .query(Long.class)
            .single();
        return count != null ? count : 0L;
    }

    /**
     * 使用 ResultMapper 执行查询，返回列表
     */
    public <T> List<T> queryWithMapper(String sql, List<Object> params, ResultMapper<T> mapper) {
        rawSqlSecurityGuard.check(sql, "JdbcDataManager.queryWithMapper");
        return jdbcClient.sql(sql)
                .params(params)
                .query(ResultMapperAdapter.adapt(mapper))
                .list();
    }

    /**
     * 获取类型处理器注册表
     */
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }

    /**
     * 设置类型处理器注册表
     */
    public void setTypeHandlerRegistry(TypeHandlerRegistry registry) {
        this.typeHandlerRegistry = registry;
    }

    /**
     * 设置数据权限上下文提供者
     *
     * @param provider 数据权限上下文提供者
     */
    public void setDataScopeContextProvider(@Nullable DataScopeContextProvider provider) {
        this.dataScopeContextProvider = provider;
    }

    /**
     * 获取数据权限上下文提供者
     *
     * @return 数据权限上下文提供者，可能为 null
     */
    public @Nullable DataScopeContextProvider getDataScopeContextProvider() {
        return dataScopeContextProvider;
    }

    /**
     * 判断给定类型是否为已注册的实体类型
     */
    public boolean isEntityType(Class<?> type) {
        try {
            getEntityMetadata(type);
            return true;
        } catch (Exception e) {
            return false;
        }
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
