package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 实体条件查询接口
 * <p>
 * 提供基于条件的查询操作，支持分页、排序、数据权限等企业级特性。
 * <p>
 * 使用示例：
 * <pre>
 * // 条件查询列表
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .query()
 *     .where(Conditions.builder(User.class).eq(User::getStatus, 1).build())
 *     .list();
 *
 * // 分页查询
 * Page&lt;User&gt; page = dataManager.entity(User.class)
 *     .query()
 *     .where(condition)
 *     .page(PageRequest.of(1, 10, Sort.by(Sort.Order.desc("createdAt"))))
 *     .execute();
 *
 * // 数据权限
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .query()
 *     .withDataScope(DataScope.of("sys_user", "dept_id", DataScopeType.DEPT))
 *     .where(condition)
 *     .list();
 *
 * // 选择部分字段
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .query()
 *     .select(User::getId, User::getName)
 *     .where(condition)
 *     .list();
 * </pre>
 *
 * @param <T> 实体类型
 */
public interface EntityQuery<T> {

    /**
     * 设置查询条件
     *
     * @param condition 查询条件
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> where(@NonNull Condition condition);

    /**
     * 设置排序
     *
     * @param sort 排序规则
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> orderBy(@NonNull Sort sort);

    /**
     * 添加升序排序（Lambda 方式）
     * <p>
     * 类型安全的排序方式，避免字段名拼写错误。
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 查询构建器（支持链式调用）
     */
    default <R> @NonNull EntityQuery<T> orderByAsc(@NonNull SFunction<T, R> getter) {
        return orderBy(Sort.asc(getter));
    }

    /**
     * 添加降序排序（Lambda 方式）
     * <p>
     * 类型安全的排序方式，避免字段名拼写错误。
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 查询构建器（支持链式调用）
     */
    default <R> @NonNull EntityQuery<T> orderByDesc(@NonNull SFunction<T, R> getter) {
        return orderBy(Sort.desc(getter));
    }

    /**
     * 选择部分字段查询（字符串字段名）
     * <p>
     * 默认查询所有字段（SELECT *），使用此方法可以指定只查询部分字段。
     *
     * @param fields 要查询的字段名
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> select(@NonNull String... fields);

    /**
     * 选择部分字段查询（Lambda 字段引用）
     * <p>
     * 类型安全的字段选择方式。
     *
     * @param getters 字段 getter 方法引用
     * @return 查询构建器（支持链式调用）
     */
    @SuppressWarnings("unchecked")
    default @NonNull EntityQuery<T> select(@NonNull SFunction<T, ?>... getters) {
        String[] fields = new String[getters.length];
        for (int i = 0; i < getters.length; i++) {
            fields[i] = io.github.afgprojects.framework.data.core.condition.Conditions.getFieldName(getters[i]);
        }
        return select(fields);
    }

    /**
     * 排除部分字段查询
     * <p>
     * 查询除指定字段外的所有字段。
     *
     * @param fields 要排除的字段名
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> exclude(@NonNull String... fields);

    /**
     * 设置数据权限
     *
     * @param scope 数据权限范围
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withDataScope(@NonNull DataScope scope);

    /**
     * 自动应用当前用户的数据权限
     * <p>自动检测实体中的部门字段（如 deptId、dept_id）
     */
    @NonNull EntityQuery<T> withDataScope();

    /**
     * 自动应用当前用户的数据权限，指定关联字段
     *
     * @param deptField 部门字段名（如 "deptId"）
     */
    @NonNull EntityQuery<T> withDataScope(String deptField);

    /**
     * 使用指定的数据范围类型
     *
     * @param scopeType 数据范围类型
     */
    @NonNull EntityQuery<T> withDataScope(DataScopeType scopeType);

    /**
     * 设置多个数据权限
     *
     * @param scopes 数据权限范围数组
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withDataScopes(@NonNull DataScope... scopes);

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withTenant(@NonNull String tenantId);

    /**
     * 设置数据源
     *
     * @param name 数据源名称
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withDataSource(@NonNull String name);

    /**
     * 设置只读模式
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withReadOnly();

    /**
     * 包含已删除记录（软删除场景）
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> includeDeleted();

    /**
     * 急加载指定关联
     *
     * @param name 关联字段名
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withAssociation(@NonNull String name);

    /**
     * 急加载多个关联
     *
     * @param names 关联字段名数组
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withAssociations(@NonNull String... names);

    /**
     * 清除关联加载配置
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> clearAssociations();

    /**
     * 设置查询限制
     *
     * @param limit 最大返回数量
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> limit(int limit);

    /**
     * 设置查询偏移量
     *
     * @param offset 偏移量
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> offset(int offset);

    // ==================== DTO 投影 ====================

    /**
     * 投影到 DTO 类型
     * <p>
     * 使用同名匹配 + @MappingField 注解进行字段映射。
     *
     * @param dtoType DTO 类型
     * @param <R>     DTO 类型参数
     * @return DTO 投影查询构建器
     */
    <R> @NonNull ProjectedQuery<T, R> project(@NonNull Class<R> dtoType);

    /**
     * 投影到 DTO 类型（编程式映射）
     * <p>
     * 使用 Projection 接口自定义实体到 DTO 的映射逻辑。
     *
     * @param projection 投影映射
     * @param <R>        DTO 类型参数
     * @return DTO 投影查询构建器
     */
    <R> @NonNull ProjectedQuery<T, R> project(@NonNull Projection<T, R> projection);

    // ==================== 执行方法 ====================

    /**
     * 执行查询，返回列表
     *
     * @return 实体列表
     */
    @NonNull List<T> list();

    /**
     * 执行分页查询
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @NonNull Page<T> page(@NonNull PageRequest pageRequest);

    /**
     * 执行查询，返回唯一结果
     * <p>
     * 如果查询结果超过一条，抛出异常。
     *
     * @return 实体（可能为空）
     */
    @NonNull Optional<T> one();

    /**
     * 执行查询，返回第一个结果
     *
     * @return 实体（可能为空）
     */
    @NonNull Optional<T> first();

    /**
     * 执行查询，统计数量
     *
     * @return 数量
     */
    long count();

    /**
     * 执行查询，判断是否存在
     *
     * @return 是否存在
     */
    boolean exists();

    // ==================== 静态工厂方法 ====================

    /**
     * 创建空查询条件
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 空查询条件
     */
    static <T> @NonNull Condition empty(Class<T> entityClass) {
        return Condition.empty();
    }
}