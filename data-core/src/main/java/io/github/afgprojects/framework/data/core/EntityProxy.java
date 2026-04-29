package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 实体操作代理
 * <p>
 * 类型安全的实体操作入口，支持链式调用配置企业级特性。
 * <p>
 * 使用示例：
 * <pre>
 * // 基础 CRUD
 * dataManager.entity(User.class).save(user);
 * dataManager.entity(User.class).findById(1L);
 *
 * // 条件查询
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .findAll(Conditions.builder().eq("status", 1).build());
 *
 * // 企业级特性
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .withDataScope(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT))
 *     .withTenant("tenant-001")
 *     .findAll();
 * </pre>
 *
 * @param <T> 实体类型
 */
public interface EntityProxy<T> {

    // ==================== 基础 CRUD ====================

    /**
     * 保存实体（新增或更新）
     */
    @NonNull T save(@NonNull T entity);

    /**
     * 批量保存实体
     */
    @NonNull List<T> saveAll(@NonNull Iterable<T> entities);

    /**
     * 插入实体
     */
    @NonNull T insert(@NonNull T entity);

    /**
     * 批量插入实体
     */
    @NonNull List<T> insertAll(@NonNull Iterable<T> entities);

    /**
     * 更新实体
     */
    @NonNull T update(@NonNull T entity);

    /**
     * 批量更新实体
     */
    @NonNull List<T> updateAll(@NonNull Iterable<T> entities);

    /**
     * 根据ID查询实体
     */
    @NonNull Optional<T> findById(@NonNull Object id);

    /**
     * 查询所有实体
     */
    @NonNull List<T> findAll();

    /**
     * 根据多个ID查询实体
     */
    @NonNull List<T> findAllById(@NonNull Iterable<?> ids);

    /**
     * 统计实体总数
     */
    long count();

    /**
     * 判断实体是否存在
     */
    boolean existsById(@NonNull Object id);

    /**
     * 根据ID删除实体
     */
    void deleteById(@NonNull Object id);

    /**
     * 删除实体
     */
    void delete(@NonNull T entity);

    /**
     * 根据多个ID批量删除
     */
    void deleteAllById(@NonNull Iterable<?> ids);

    /**
     * 批量删除实体
     */
    void deleteAll(@NonNull Iterable<? extends T> entities);

    // ==================== 条件查询 ====================

    /**
     * 根据条件查询实体列表
     */
    @NonNull List<T> findAll(@NonNull Condition condition);

    /**
     * 根据条件分页查询
     */
    @NonNull Page<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageable);

    /**
     * 根据条件统计数量
     */
    long count(@NonNull Condition condition);

    /**
     * 根据条件判断是否存在
     */
    boolean exists(@NonNull Condition condition);

    /**
     * 根据条件查询唯一实体
     */
    @NonNull Optional<T> findOne(@NonNull Condition condition);

    /**
     * 根据条件查询第一个实体
     */
    @NonNull Optional<T> findFirst(@NonNull Condition condition);

    // ==================== 条件更新/删除 ====================

    /**
     * 根据条件批量更新
     */
    long updateAll(@NonNull Condition condition, @NonNull Map<String, Object> updates);

    /**
     * 根据条件批量删除
     */
    long deleteAll(@NonNull Condition condition);

    // ==================== 企业级特性 ====================

    /**
     * 设置数据权限
     */
    @NonNull EntityProxy<T> withDataScope(@NonNull DataScope scope);

    /**
     * 设置多个数据权限
     */
    @NonNull EntityProxy<T> withDataScopes(@NonNull DataScope... scopes);

    /**
     * 设置租户ID
     */
    @NonNull EntityProxy<T> withTenant(@NonNull String tenantId);

    /**
     * 设置数据源
     */
    @NonNull EntityProxy<T> withDataSource(@NonNull String name);

    /**
     * 设置只读模式
     */
    @NonNull EntityProxy<T> withReadOnly();

    /**
     * 包含已删除记录（软删除场景）
     */
    @NonNull EntityProxy<T> includeDeleted();

    // ==================== 软删除扩展 ====================

    /**
     * 根据ID恢复删除
     */
    void restoreById(@NonNull Object id);

    /**
     * 根据多个ID恢复删除
     */
    void restoreAllById(@NonNull Iterable<?> ids);

    // ==================== 关联查询 ====================

    /**
     * 急加载指定关联
     * <p>
     * 配置查询时加载关联数据，支持链式调用配置多个关联。
     * <p>
     * 使用示例：
     * <pre>
     * List&lt;User&gt; users = dataManager.entity(User.class)
     *     .withAssociation("profile")
     *     .withAssociation("orders")
     *     .findAll();
     * </pre>
     *
     * @param name 关联字段名
     * @return 实体操作代理（支持链式调用）
     */
    @NonNull EntityProxy<T> withAssociation(@NonNull String name);

    /**
     * 急加载多个关联
     * <p>
     * 配置查询时加载多个关联数据。
     *
     * @param names 关联字段名数组
     * @return 实体操作代理（支持链式调用）
     */
    @NonNull EntityProxy<T> withAssociations(@NonNull String... names);

    /**
     * 加载实体的关联数据
     * <p>
     * 对已加载的实体，按需加载指定关联字段的数据。
     * <p>
     * 使用示例：
     * <pre>
     * User user = dataManager.entity(User.class).findById(1L);
     * List&lt;Order&gt; orders = dataManager.entity(User.class).fetch(user, "orders");
     * </pre>
     *
     * @param entity     实体实例
     * @param name       关联字段名
     * @param <R>        关联数据类型
     * @return 关联数据
     */
    <R> @NonNull R fetch(@NonNull T entity, @NonNull String name);

    /**
     * 批量加载实体的关联数据
     * <p>
     * 对多个实体批量加载指定关联字段的数据，避免 N+1 问题。
     *
     * @param entities 实体实例列表
     * @param name     关联字段名
     */
    void fetchAll(@NonNull Iterable<T> entities, @NonNull String name);

    /**
     * 清除关联加载配置
     * <p>
     * 清除之前配置的关联加载设置，恢复默认行为。
     *
     * @return 实体操作代理
     */
    @NonNull EntityProxy<T> clearAssociations();
}