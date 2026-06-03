package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * DTO 投影查询接口
 * <p>
 * 提供基于条件的投影查询操作，将实体查询结果映射为指定的 DTO 类型。
 * 支持分页、排序、数据权限、多租户等企业级特性。
 * <p>
 * 通过 {@link EntityQuery#project(Class)} 或 {@link EntityQuery#project(Projection)} 创建实例。
 * <p>
 * 使用示例：
 * <pre>
 * // 投影到 DTO 类型
 * List&lt;UserDTO&gt; dtos = dataManager.entity(User.class)
 *     .query()
 *     .project(UserDTO.class)
 *     .where(Conditions.builder(User.class).eq(User::getStatus, 1).build())
 *     .list();
 *
 * // 投影查询 + 分页
 * Page&lt;UserDTO&gt; page = dataManager.entity(User.class)
 *     .query()
 *     .project(UserDTO.class)
 *     .where(condition)
 *     .page(PageRequest.of(1, 10));
 *
 * // 投影查询 + 数据权限
 * List&lt;UserDTO&gt; dtos = dataManager.entity(User.class)
 *     .query()
 *     .project(UserDTO.class)
 *     .withDataScope()
 *     .where(condition)
 *     .list();
 * </pre>
 *
 * @param <T> 实体类型
 * @param <R> 投影结果类型（DTO）
 */
public interface ProjectedQuery<T, R> extends BaseQuery<ProjectedQuery<T, R>, R> {

    /**
     * 选择部分字段查询（Lambda 字段引用）
     * <p>
     * 类型安全的字段选择方式，只查询指定的字段并映射到 DTO。
     *
     * @param getters 字段 getter 方法引用
     * @return 查询构建器（支持链式调用）
     */
    @NonNull ProjectedQuery<T, R> select(@NonNull SFunction<T, ?>... getters);

    /**
     * 选择部分字段查询（字符串字段名）
     * <p>
     * 默认查询所有字段，使用此方法可以指定只查询部分字段。
     *
     * @param fields 要查询的字段名
     * @return 查询构建器（支持链式调用）
     */
    @NonNull ProjectedQuery<T, R> select(@NonNull String... fields);

    /**
     * 设置查询条件
     *
     * @param condition 查询条件
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> where(@NonNull Condition condition);

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
    @NonNull ProjectedQuery<T, R> and(@NonNull Condition condition);

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
    @NonNull ProjectedQuery<T, R> or(@NonNull Condition condition);

    /**
     * 设置排序
     *
     * @param sort 排序规则
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> orderBy(@NonNull Sort sort);

    /**
     * 自动应用当前用户的数据权限
     * <p>
     * 自动检测实体中的部门字段（如 deptId、dept_id）。
     *
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> withDataScope();

    /**
     * 自动应用当前用户的数据权限，指定关联字段
     *
     * @param deptField 部门字段名（如 "deptId"）
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> withDataScope(@NonNull String deptField);

    /**
     * 使用指定的数据范围类型
     *
     * @param scopeType 数据范围类型
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> withDataScope(@NonNull DataScopeType scopeType);

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> withTenant(@NonNull String tenantId);

    /**
     * 包含已删除记录（软删除场景）
     * <p>
     * 默认情况下，软删除的记录会被自动过滤。调用此方法后，
     * 查询结果将包含已标记为删除的记录。
     *
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> includeDeleted();

    /**
     * 设置查询限制
     *
     * @param limit 最大返回数量
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> limit(int limit);

    /**
     * 设置查询偏移量
     *
     * @param offset 偏移量
     * @return 查询构建器（支持链式调用）
     */
    @Override
    @NonNull ProjectedQuery<T, R> offset(int offset);

    // ==================== 执行方法 ====================

    /**
     * 执行查询，返回 DTO 列表
     *
     * @return DTO 列表
     */
    @Override
    @NonNull List<R> list();

    /**
     * 执行查询，返回唯一结果
     * <p>
     * 如果查询结果超过一条，抛出异常。
     *
     * @return DTO 可选值
     */
    @Override
    @NonNull Optional<R> one();

    /**
     * 执行查询，返回第一个结果
     *
     * @return DTO 可选值
     */
    @Override
    @NonNull Optional<R> first();

    /**
     * 执行分页查询
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    @NonNull Page<R> page(@NonNull PageRequest pageRequest);

    /**
     * 执行查询，统计数量
     *
     * @return 匹配的记录数量
     */
    @Override
    long count();
}