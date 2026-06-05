package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.data.core.scope.TenantScope;
import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import io.github.afgprojects.framework.data.core.transaction.TransactionAdapter;
import io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 数据操作管理器
 * <p>
 * 无泛型全局门面，作为数据操作的唯一入口。
 * <ul>
 *   <li>{@code entity(Class)} - 获取 EntityProxy 进行实体操作</li>
 *   <li>{@code query()} - 获取 SqlQueryBuilder 进行复杂查询</li>
 *   <li>{@code update()} - 获取 SqlUpdateBuilder 进行更新</li>
 *   <li>{@code insert()} - 获取 SqlInsertBuilder 进行插入</li>
 *   <li>{@code delete()} - 获取 SqlDeleteBuilder 进行删除</li>
 * </ul>
 */
public interface DataManager {

    // ==================== 实体操作 ====================

    /**
     * 获取实体操作代理
     *
     * @param entityClass 实体类型
     * @return 实体操作代理
     */
    <T> @NonNull EntityProxy<T> entity(@NonNull Class<T> entityClass);

    /**
     * 获取实体元数据
     *
     * @param entityClass 实体类型
     * @return 实体元数据
     */
    <T> @NonNull EntityMetadata<T> getEntityMetadata(@NonNull Class<T> entityClass);

    // ==================== SQL 构建 ====================

    /**
     * 创建 SQL 查询构建器
     *
     * @return SQL 查询构建器
     */
    @NonNull SqlQueryBuilder query();

    /**
     * 创建 SQL 更新构建器
     *
     * @return SQL 更新构建器
     */
    @NonNull SqlUpdateBuilder update();

    /**
     * 创建 SQL 插入构建器
     *
     * @return SQL 插入构建器
     */
    @NonNull SqlInsertBuilder insert();

    /**
     * 创建 SQL 删除构建器
     *
     * @return SQL 删除构建器
     */
    @NonNull SqlDeleteBuilder delete();

    // ==================== 事务管理 ====================

    /**
     * 在事务中执行
     *
     * @param action 要执行的操作
     */
    void executeInTransaction(@NonNull Runnable action);

    /**
     * 在事务中执行并返回结果
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    <T> T executeInTransaction(@NonNull Supplier<T> action);

    /**
     * 在只读事务中执行
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    <T> T executeInReadOnly(@NonNull Supplier<T> action);

    // ==================== 租户管理 ====================

    /**
     * 创建租户作用域（try-with-resources 自动恢复）
     *
     * @param tenantId 租户ID
     * @return 租户作用域
     */
    @NonNull TenantScope tenantScope(@NonNull String tenantId);

    /**
     * 获取租户上下文持有者
     *
     * @return 租户上下文持有者
     */
    @NonNull TenantContextHolder getTenantContextHolder();

    // ==================== 数据库信息 ====================

    /**
     * 获取当前数据库类型
     *
     * @return 数据库类型
     */
    @NonNull DatabaseType getDatabaseType();

    /**
     * 获取事务管理器
     * <p>
     * 返回底层事务管理器对象（如 Spring 的 PlatformTransactionManager）。
     * 如果未配置事务管理器，可能返回 null 或降级对象。
     *
     * @return 事务管理器，可能为 null（如果未配置）
     */
    @Nullable Object getTransactionManager();

    /**
     * 获取事务适配器
     * <p>
     * 事务适配器提供与 Spring 声明式事务的集成能力。
     *
     * @return 事务适配器，可能为 null（如果未配置）
     */
    @Nullable TransactionAdapter getTransactionAdapter();

    /**
     * 设置事务适配器
     * <p>
     * 允许运行时替换事务适配器实现。
     *
     * @param adapter 事务适配器
     */
    void setTransactionAdapter(@NonNull TransactionAdapter adapter);

    // ==================== 原始 SQL 操作 ====================

    /**
     * 执行原始 SQL 更新操作
     *
     * @param sql    SQL 语句
     * @param params 参数列表
     * @return 受影响的行数
     */
    int executeUpdate(@NonNull String sql, @NonNull List<Object> params);

    /**
     * 执行原始 SQL 更新操作（命名参数）
     *
     * @param sql    SQL 语句
     * @param params 命名参数映射
     * @return 受影响的行数
     */
    int executeUpdate(@NonNull String sql, @NonNull Map<String, Object> params);

    /**
     * 执行原始 SQL 查询并返回列表
     *
     * @param sql        SQL 语句
     * @param params     参数列表
     * @param rowMapper  行映射器
     * @param <T>        结果类型
     * @return 查询结果列表
     */
    <T> @NonNull List<T> queryForList(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper);

    /**
     * 执行原始 SQL 查询并返回单个结果
     *
     * @param sql        SQL 语句
     * @param params     参数列表
     * @param rowMapper  行映射器
     * @param <T>        结果类型
     * @return 查询结果，可能为 null
     */
    <T> @Nullable T queryForObject(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper);

    /**
     * 执行原始 SQL 查询并返回可选单个结果
     *
     * @param sql        SQL 语句
     * @param params     参数列表
     * @param rowMapper  行映射器
     * @param <T>        结果类型
     * @return 查询结果可选值
     */
    <T> @NonNull Optional<T> queryForOptional(@NonNull String sql, @NonNull List<Object> params, @NonNull ResultMapper<T> rowMapper);

    /**
     * 执行原始 SQL 查询并返回计数
     *
     * @param sql    SQL 语句
     * @param params 参数列表
     * @return 计数结果
     */
    long queryForCount(@NonNull String sql, @NonNull List<Object> params);

    // ==================== 快捷查询方法 ====================

    /**
     * 根据ID查找实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).findById(id)}
     *
     * @param entityClass 实体类型
     * @param id          主键值
     * @return 实体可选值
     */
    default <T> @NonNull Optional<T> findById(@NonNull Class<T> entityClass, @Nullable Object id) {
        return entity(entityClass).findById(id);
    }

    /**
     * 根据单个字段值查找唯一实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).findOne(Conditions.builder(entityClass).eq(getter, value).build())}
     * <p>
     * <b>类型安全说明：</b>{@code value} 应与字段 {@code getter} 的返回类型一致。
     * 例如，如果字段类型为 {@code Integer}，则 {@code value} 应为 {@code Integer} 或 {@code null}。
     * 运行时会检查值类型与字段声明类型的兼容性，不兼容时抛出 {@link IllegalArgumentException}。
     *
     * @param entityClass 实体类型
     * @param getter      字段 getter 方法引用
     * @param value       字段值，应与字段类型一致，可以为 null
     * @return 实体可选值
     * @throws IllegalArgumentException 如果 value 的运行时类型与字段声明类型不兼容
     */
    default <T, R> @NonNull Optional<T> findOneByField(@NonNull Class<T> entityClass,
                                                         @NonNull SFunction<T, R> getter,
                                                         @Nullable Object value) {
        // 运行时类型检查
        String fieldName = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getter);
        Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
        io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
        return entity(entityClass).findOne(
            io.github.afgprojects.framework.data.core.condition.Conditions.builder(entityClass)
                .eq(getter, value)
                .build()
        );
    }

    /**
     * 根据单个字段值查找所有匹配实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).query().where(Conditions.builder(entityClass).eq(getter, value).build()).list()}
     * <p>
     * <b>类型安全说明：</b>{@code value} 应与字段 {@code getter} 的返回类型一致。
     * 例如，如果字段类型为 {@code Integer}，则 {@code value} 应为 {@code Integer} 或 {@code null}。
     * 运行时会检查值类型与字段声明类型的兼容性，不兼容时抛出 {@link IllegalArgumentException}。
     *
     * @param entityClass 实体类型
     * @param getter      字段 getter 方法引用
     * @param value       字段值，应与字段类型一致，可以为 null
     * @return 实体列表
     * @throws IllegalArgumentException 如果 value 的运行时类型与字段声明类型不兼容
     */
    default <T, R> @NonNull List<T> findAllByField(@NonNull Class<T> entityClass,
                                                    @NonNull SFunction<T, R> getter,
                                                    @Nullable Object value) {
        // 运行时类型检查
        String fieldName = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getter);
        Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
        io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
        return entity(entityClass).query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(entityClass)
                .eq(getter, value)
                .build())
            .list();
    }

    /**
     * 按单字段判断记录是否存在
     *
     * @param entityClass 实体类
     * @param getter      字段的 Lambda 方法引用
     * @param value       字段值
     * @return 是否存在匹配记录
     */
    default <T, R> boolean existsByField(@NonNull Class<T> entityClass,
                                          @NonNull SFunction<T, R> getter,
                                          @Nullable Object value) {
        // 运行时类型检查
        String fieldName = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getter);
        Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
        io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
        return entity(entityClass).query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(entityClass)
                .eq(getter, value)
                .build())
            .exists();
    }

    /**
     * 按单字段统计记录数
     *
     * @param entityClass 实体类
     * @param getter      字段的 Lambda 方法引用
     * @param value       字段值
     * @return 匹配记录数
     */
    default <T, R> long countByField(@NonNull Class<T> entityClass,
                                      @NonNull SFunction<T, R> getter,
                                      @Nullable Object value) {
        // 运行时类型检查
        String fieldName = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getter);
        Class<?> fieldType = io.github.afgprojects.framework.data.core.util.TypeDescriptorUtils.resolveFieldTypeFromLambda(getter);
        io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder.checkFieldType(fieldName, fieldType, value);
        return entity(entityClass).query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(entityClass)
                .eq(getter, value)
                .build())
            .count();
    }

    /**
     * 按条件删除记录
     * <p>
     * 软删除实体自动变软删除。
     *
     * @param entityClass 实体类
     * @param condition   删除条件
     * @return 删除的记录数
     */
    default <T> long deleteByCondition(@NonNull Class<T> entityClass,
                                        @NonNull Condition condition) {
        return entity(entityClass).deleteByCondition(condition);
    }

    /**
     * 更新实体
     * <p>
     * 与 save() 不同，此方法不检查实体是否已存在，直接执行 UPDATE。
     *
     * @param entityClass 实体类
     * @param entity      要更新的实体（必须已设置 ID）
     * @return 更新后的实体
     */
    default <T> @NonNull T update(@NonNull Class<T> entityClass, @NonNull T entity) {
        return entity(entityClass).update(entity);
    }

    /**
     * 按条件批量更新
     *
     * @param entityClass 实体类
     * @param condition   更新条件
     * @param updates     字段更新映射（字段名 -> 新值）
     * @return 更新的记录数
     */
    default <T> long updateAll(@NonNull Class<T> entityClass,
                                @NonNull Condition condition,
                                @NonNull Map<String, Object> updates) {
        return entity(entityClass).updateAll(condition, updates);
    }

    /**
     * 恢复软删除的记录
     *
     * @param entityClass 实体类
     * @param id          记录 ID
     */
    default <T> void restoreById(@NonNull Class<T> entityClass, @NonNull Object id) {
        entity(entityClass).restoreById(id);
    }

    /**
     * 查找所有实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).findAll()}
     *
     * @param entityClass 实体类型
     * @return 实体列表
     */
    default <T> @NonNull List<T> findAll(@NonNull Class<T> entityClass) {
        return entity(entityClass).findAll();
    }

    /**
     * 根据条件查找唯一实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).findOne(condition)}
     *
     * @param entityClass 实体类型
     * @param condition   查询条件
     * @return 实体可选值
     */
    default <T> @NonNull Optional<T> findOne(@NonNull Class<T> entityClass, @NonNull Condition condition) {
        return entity(entityClass).findOne(condition);
    }

    /**
     * 根据条件查找所有匹配实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).query().where(condition).list()}
     *
     * @param entityClass 实体类型
     * @param condition   查询条件
     * @return 实体列表
     */
    default <T> @NonNull List<T> findList(@NonNull Class<T> entityClass, @NonNull Condition condition) {
        return entity(entityClass).query().where(condition).list();
    }

    /**
     * 根据条件查找实体列表（自动应用数据权限）
     */
    default <T> @NonNull List<T> findListWithDataScope(@NonNull Class<T> entityClass,
                                                        @NonNull Condition condition) {
        return entity(entityClass).query().withDataScope().where(condition).list();
    }

    /**
     * 根据条件查找实体列表（指定数据权限字段）
     */
    default <T> @NonNull List<T> findListWithDataScope(@NonNull Class<T> entityClass,
                                                        @NonNull String deptField,
                                                        @NonNull Condition condition) {
        return entity(entityClass).query().withDataScope(deptField).where(condition).list();
    }

    /**
     * 统计实体总数（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).count()}
     *
     * @param entityClass 实体类型
     * @return 实体总数
     */
    default <T> long count(@NonNull Class<T> entityClass) {
        return entity(entityClass).count();
    }

    /**
     * 根据条件统计实体数量（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).query().where(condition).count()}
     *
     * @param entityClass 实体类型
     * @param condition   查询条件
     * @return 匹配的实体数量
     */
    default <T> long countByCondition(@NonNull Class<T> entityClass, @NonNull Condition condition) {
        return entity(entityClass).query().where(condition).count();
    }

    /**
     * 根据ID删除实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).deleteById(id)}
     *
     * @param entityClass 实体类型
     * @param id          主键值
     */
    default <T> void deleteById(@NonNull Class<T> entityClass, @NonNull Object id) {
        entity(entityClass).deleteById(id);
    }

    /**
     * 保存实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).save(entity)}
     *
     * @param entityClass 实体类型
     * @param entity      实体实例
     * @return 保存后的实体
     */
    @SuppressWarnings("unchecked")
    default <T> @NonNull T save(@NonNull Class<T> entityClass, @NonNull T entity) {
        return entity(entityClass).save(entity);
    }

    /**
     * 批量保存实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).saveAll(entities)}
     *
     * @param entityClass 实体类型
     * @param entities    实体集合
     * @return 保存后的实体列表
     */
    default <T> @NonNull List<T> saveAll(@NonNull Class<T> entityClass, @NonNull Iterable<? extends T> entities) {
        List<T> list = new ArrayList<>();
        entities.forEach(e -> list.add(e));
        return entity(entityClass).saveAll(list);
    }

    /**
     * 批量插入实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).insertAll(entities)}
     *
     * @param entityClass 实体类型
     * @param entities    实体集合
     * @return 插入后的实体列表
     */
    default <T> @NonNull List<T> insertAll(@NonNull Class<T> entityClass, @NonNull Iterable<T> entities) {
        return entity(entityClass).insertAll(entities);
    }

    /**
     * 根据多个ID批量删除（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).deleteAllById(ids)}
     *
     * @param entityClass 实体类型
     * @param ids         ID集合
     */
    default <T> void deleteAllById(@NonNull Class<T> entityClass, @NonNull Iterable<?> ids) {
        entity(entityClass).deleteAllById(ids);
    }

    /**
     * 根据多个ID查找实体（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).findAllById(ids)}
     *
     * @param entityClass 实体类型
     * @param ids         ID集合
     * @return 实体列表
     */
    default <T> @NonNull List<T> findAllById(@NonNull Class<T> entityClass, @NonNull Iterable<?> ids) {
        return entity(entityClass).findAllById(ids);
    }

    /**
     * 判断实体是否存在（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).existsById(id)}
     *
     * @param entityClass 实体类型
     * @param id          主键值
     * @return 是否存在
     */
    default <T> boolean existsById(@NonNull Class<T> entityClass, @NonNull Object id) {
        return entity(entityClass).existsById(id);
    }

    /**
     * 根据条件判断是否存在（快捷方法）
     * <p>
     * 等价于 {@code entity(entityClass).query().where(condition).exists()}
     *
     * @param entityClass 实体类型
     * @param condition   查询条件
     * @return 是否存在
     */
    default <T> boolean existsByCondition(@NonNull Class<T> entityClass, @NonNull Condition condition) {
        return entity(entityClass).query().where(condition).exists();
    }

}