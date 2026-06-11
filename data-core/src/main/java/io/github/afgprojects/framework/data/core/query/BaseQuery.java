package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * 查询构建器公共接口
 * <p>
 * 定义了 {@link EntityQuery} 和 {@link ProjectedQuery} 共享的查询配置方法，
 * 包括条件设置、排序、数据权限、多租户、软删除、分页限制等。
 * <p>
 * 使用自类型泛型 {@code Q} 确保链式调用返回具体的查询类型，
 * 而不是返回抽象的基类类型。
 * <p>
 * 本接口不直接使用，而是通过 {@link EntityQuery} 或 {@link ProjectedQuery} 间接继承。
 *
 * @param <Q> 具体查询类型（自类型，用于链式调用类型安全）
 * @param <R> 查询结果类型（实体类型或 DTO 类型）
 */
public interface BaseQuery<Q, R> {

    /**
     * 设置查询条件
     *
     * @param condition 查询条件
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q where(@NonNull Condition condition);

    /**
     * 追加 AND 条件
     * <p>
     * 在已有条件基础上追加 AND 条件。
     * 如果尚未设置条件，等同于 {@code where(condition)}。
     *
     * @param condition 要追加的条件
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q and(@NonNull Condition condition);

    /**
     * 追加 OR 条件
     * <p>
     * 在已有条件基础上追加 OR 条件。
     * 如果尚未设置条件，等同于 {@code where(condition)}。
     *
     * @param condition 要追加的条件
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q or(@NonNull Condition condition);

    /**
     * 设置排序
     *
     * @param sort 排序规则
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q orderBy(@NonNull Sort sort);

    /**
     * 自动应用当前用户的数据权限
     * <p>
     * 自动检测实体中的部门字段（如 deptId、dept_id）。
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q withDataScope();

    /**
     * 自动应用当前用户的数据权限，指定关联字段
     *
     * @param deptField 部门字段名（如 "deptId"）
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q withDataScope(@NonNull String deptField);

    /**
     * 使用指定的数据范围类型
     *
     * @param scopeType 数据范围类型
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q withDataScope(@NonNull DataScopeType scopeType);

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q withTenant(@NonNull String tenantId);

    /**
     * 包含已删除记录（软删除场景）
     * <p>
     * 默认情况下，软删除的记录会被自动过滤。调用此方法后，
     * 查询结果将包含已标记为删除的记录。
     *
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q includeDeleted();

    /**
     * 设置查询限制
     *
     * @param limit 最大返回数量
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q limit(int limit);

    /**
     * 设置查询偏移量
     *
     * @param offset 偏移量
     * @return 查询构建器（支持链式调用）
     */
    @NonNull Q offset(int offset);

    // ==================== 执行方法 ====================

    /**
     * 执行查询，返回列表
     *
     * @return 查询结果列表
     */
    @NonNull List<R> list();

    /**
     * 执行查询，返回唯一结果
     * <p>
     * 如果查询结果超过一条，抛出异常。
     *
     * @return 查询结果可选值
     */
    @NonNull Optional<R> one();

    /**
     * 执行查询，返回第一个结果
     *
     * @return 查询结果可选值
     */
    @NonNull Optional<R> first();

    /**
     * 执行分页查询
     *
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @NonNull PageData<R> page(@NonNull PageRequest pageRequest);

    /**
     * 执行查询，统计数量
     *
     * @return 匹配的记录数量
     */
    long count();
}