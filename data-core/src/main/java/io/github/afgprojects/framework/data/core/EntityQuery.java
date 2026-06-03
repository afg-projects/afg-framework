package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.mapper.Projection;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.BaseQuery;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.ProjectedQuery;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.query.AggregateQuery;
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
 * 继承自 {@link BaseQuery}，与 {@link ProjectedQuery} 共享公共查询配置方法。
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
 * @see BaseQuery 查询构建器公共接口
 * @see ProjectedQuery DTO 投影查询接口
 */
public interface EntityQuery<T> extends BaseQuery<EntityQuery<T>, T> {

    // ==================== BaseQuery 方法覆写 ====================

    /**
     * 设置查询条件
     *
     * @param condition 查询条件
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> where(@NonNull Condition condition);

    /**
     * 追加 AND 条件
     * <p>
     * 在已有条件基础上追加 AND 条件。
     * 如果尚未设置条件，等同于 {@code where(condition)}。
     *
     * @param condition 要追加的条件
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> and(@NonNull Condition condition);

    /**
     * 追加 OR 条件
     * <p>
     * 在已有条件基础上追加 OR 条件。
     * 如果尚未设置条件，等同于 {@code where(condition)}。
     *
     * @param condition 要追加的条件
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> or(@NonNull Condition condition);

    /**
     * 设置 DISTINCT 去重
     * <p>
     * 启用后查询将使用 {@code SELECT DISTINCT} 替代 {@code SELECT}，
     * 去除结果集中的重复行。
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> distinct();

    /**
     * 设置排序
     *
     * @param sort 排序规则
     * @return 查询构建器（支持链式调用）
     */
    @Override
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
     * 自动应用当前用户的数据权限
     * <p>自动检测实体中的部门字段（如 deptId、dept_id）
     */
    @Override
    @NonNull EntityQuery<T> withDataScope();

    /**
     * 自动应用当前用户的数据权限，指定关联字段
     *
     * @param deptField 部门字段名（如 "deptId"）
     */
    @Override
    @NonNull EntityQuery<T> withDataScope(@NonNull String deptField);

    /**
     * 使用指定的数据范围类型
     *
     * @param scopeType 数据范围类型
     */
    @Override
    @NonNull EntityQuery<T> withDataScope(@NonNull DataScopeType scopeType);

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> withTenant(@NonNull String tenantId);

    /**
     * 包含已删除记录（软删除场景）
     *
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> includeDeleted();

    /**
     * 设置查询限制
     *
     * @param limit 最大返回数量
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> limit(int limit);

    /**
     * 设置查询偏移量
     *
     * @param offset 偏移量
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull EntityQuery<T> offset(int offset);

    // ==================== 执行方法覆写 ====================

    /**
     * 执行查询，返回列表
     *
     * @return 实体列表
     */
    @Override
    @NonNull List<T> list();

    /**
     * 执行分页查询
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    @NonNull Page<T> page(@NonNull PageRequest pageRequest);

    /**
     * 执行查询，返回唯一结果
     * <p>
     * 如果查询结果超过一条，抛出异常。
     *
     * @return 实体（可能为空）
     */
    @Override
    @NonNull Optional<T> one();

    /**
     * 执行查询，返回第一个结果
     *
     * @return 实体（可能为空）
     */
    @Override
    @NonNull Optional<T> first();

    /**
     * 执行查询，统计数量
     *
     * @return 数量
     */
    @Override
    long count();

    // ==================== EntityQuery 独有方法 ====================

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
     * 设置多个数据权限
     *
     * @param scopes 数据权限范围数组
     * @return 查询构建器（支持链式调用）
     */
    @NonNull EntityQuery<T> withDataScopes(@NonNull DataScope... scopes);

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
     * 执行查询，判断是否存在
     *
     * @return 是否存在
     */
    boolean exists();

    // ==================== 聚合查询 ====================

    /**
     * 创建聚合查询构建器
     * <p>
     * 聚合查询支持 GROUP BY、聚合函数（COUNT/SUM/AVG/MAX/MIN）、HAVING 过滤。
     * 适用于单表统计报表场景。
     * <p>
     * 使用示例：
     * <pre>
     * // 按部门统计人数
     * List&lt;AggregateResult&gt; results = dataManager.entity(User.class)
     *     .query()
     *     .aggregate()
     *     .groupBy("dept_id")
     *     .count("id", "userCount")
     *     .avg("salary", "avgSalary")
     *     .list();
     * </pre>
     *
     * @return 聚合查询构建器
     */
    @NonNull AggregateQuery<T> aggregate();

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