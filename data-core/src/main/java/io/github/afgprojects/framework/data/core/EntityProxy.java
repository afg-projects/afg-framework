package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * 实体操作代理
 * <p>
 * 类型安全的实体操作入口，整合读取、写入、查询三大能力。
 * <p>
 * 该接口继承自 {@link EntityReader}、{@link EntityWriter}，
 * 提供完整的实体操作能力。接口拆分后职责更清晰，便于理解和维护。
 * <p>
 * 使用示例：
 * <pre>
 * // 基础 CRUD
 * User saved = dataManager.entity(User.class).save(user);
 * Optional&lt;User&gt; user = dataManager.entity(User.class).findById(1L);
 *
 * // 条件查询（推荐使用 query() 方法）
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .query()
 *     .where(Conditions.builder(User.class).eq(User::getStatus, 1).build())
 *     .list();
 *
 * // 分页查询
 * PageData&lt;User&gt; page = dataManager.entity(User.class)
 *     .query()
 *     .where(condition)
 *     .page(PageRequest.of(1, 10));
 *
 * // 企业级特性（便捷方法，等同于 query().withXxx()）
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .withDataScope(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT))
 *     .withTenant("tenant-001")
 *     .findAll(condition);
 * </pre>
 *
 * @param <T> 实体类型
 * @see EntityReader 实体读取操作接口
 * @see EntityWriter 实体写入操作接口
 * @see EntityQuery 实体条件查询接口
 */
public interface EntityProxy<T> extends EntityReader<T>, EntityWriter<T> {

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
     * @param entity 实体实例
     * @param name   关联字段名
     * @param <R>    关联数据类型
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

    // ==================== 便捷方法（委托到 query()） ====================

    /**
     * 设置数据权限（便捷方法）
     * <p>
     * 等同于 {@code query().withDataScope(scope)}
     *
     * @param scope 数据权限范围
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withDataScope(@NonNull DataScope scope) {
        return query().withDataScope(scope);
    }

    /**
     * 设置多个数据权限（便捷方法）
     * <p>
     * 等同于 {@code query().withDataScopes(scopes)}
     *
     * @param scopes 数据权限范围数组
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withDataScopes(@NonNull DataScope... scopes) {
        return query().withDataScopes(scopes);
    }

    /**
     * 设置租户ID（便捷方法）
     * <p>
     * 等同于 {@code query().withTenant(tenantId)}
     *
     * @param tenantId 租户ID
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withTenant(@NonNull String tenantId) {
        return query().withTenant(tenantId);
    }

    /**
     * 设置数据源（便捷方法）
     * <p>
     * 等同于 {@code query().withDataSource(name)}
     *
     * @param name 数据源名称
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withDataSource(@NonNull String name) {
        return query().withDataSource(name);
    }

    /**
     * 设置只读模式（便捷方法）
     * <p>
     * 等同于 {@code query().withReadOnly()}
     *
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withReadOnly() {
        return query().withReadOnly();
    }

    /**
     * 包含已删除记录（便捷方法）
     * <p>
     * 等同于 {@code query().includeDeleted()}
     *
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> includeDeleted() {
        return query().includeDeleted();
    }

    /**
     * 急加载指定关联（便捷方法）
     * <p>
     * 等同于 {@code query().withAssociation(name)}
     *
     * @param name 关联字段名
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withAssociation(@NonNull String name) {
        return query().withAssociation(name);
    }

    /**
     * 急加载多个关联（便捷方法）
     * <p>
     * 等同于 {@code query().withAssociations(names)}
     *
     * @param names 关联字段名数组
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> withAssociations(@NonNull String... names) {
        return query().withAssociations(names);
    }

    /**
     * 清除关联加载配置（便捷方法）
     * <p>
     * 等同于 {@code query().clearAssociations()}
     *
     * @return 查询构建器
     */
    default @NonNull EntityQuery<T> clearAssociations() {
        return query().clearAssociations();
    }

    // ==================== 条件查询便捷方法 ====================

    /**
     * 根据条件查询实体列表（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).list()}
     *
     * @param condition 查询条件
     * @return 实体列表
     */
    default java.util.@NonNull List<T> findAll(@NonNull Condition condition) {
        return query().where(condition).list();
    }

    /**
     * 根据条件分页查询（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).page(pageRequest)}
     *
     * @param condition   查询条件
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    default @NonNull PageData<T> findAll(@NonNull Condition condition, @NonNull PageRequest pageRequest) {
        return query().where(condition).page(pageRequest);
    }

    /**
     * 根据条件统计数量（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).count()}
     *
     * @param condition 查询条件
     * @return 数量
     */
    default long count(@NonNull Condition condition) {
        return query().where(condition).count();
    }

    /**
     * 根据条件判断是否存在（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).exists()}
     *
     * @param condition 查询条件
     * @return 是否存在
     */
    default boolean exists(@NonNull Condition condition) {
        return query().where(condition).exists();
    }

    /**
     * 根据条件查询唯一实体（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).one()}
     *
     * @param condition 查询条件
     * @return 实体（可能为空）
     */
    default java.util.@NonNull Optional<T> findOne(@NonNull Condition condition) {
        return query().where(condition).one();
    }

    /**
     * 根据条件查询第一个实体（便捷方法）
     * <p>
     * 等同于 {@code query().where(condition).first()}
     *
     * @param condition 查询条件
     * @return 实体（可能为空）
     */
    default java.util.@NonNull Optional<T> findFirst(@NonNull Condition condition) {
        return query().where(condition).first();
    }
}