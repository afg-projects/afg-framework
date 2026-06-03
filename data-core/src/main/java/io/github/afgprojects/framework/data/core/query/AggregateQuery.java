package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 聚合查询接口
 * <p>
 * 提供单表聚合统计查询能力，支持 GROUP BY、HAVING 和聚合函数。
 * 通过 {@link EntityQuery#aggregate()} 创建实例。
 * <p>
 * 使用示例：
 * <pre>
 * // 按部门统计人数和平均薪资
 * List&lt;AggregateResult&gt; results = dataManager.entity(User.class)
 *     .query()
 *     .aggregate()
 *     .groupBy("dept_id")
 *     .count("id", "userCount")
 *     .avg("salary", "avgSalary")
 *     .having(Conditions.gt("userCount", 5))
 *     .list();
 *
 * // 全局统计（无 GROUP BY）
 * AggregateResult result = dataManager.entity(User.class)
 *     .query()
 *     .aggregate()
 *     .count("totalCount")
 *     .sum("salary", "totalSalary")
 *     .single();
 * </pre>
 *
 * @param <T> 实体类型
 * @author afg
 */
public interface AggregateQuery<T> {

    // ==================== GROUP BY ====================

    /**
     * 添加 GROUP BY 字段（字符串字段名）
     */
    @NonNull AggregateQuery<T> groupBy(@NonNull String... fields);

    /**
     * 添加 GROUP BY 字段（Lambda 方式）
     */
    @NonNull AggregateQuery<T> groupBy(@NonNull SFunction<T, ?>... getters);

    // ==================== 聚合函数（字符串字段名） ====================

    /**
     * 添加 COUNT(field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> count(@NonNull String field, @NonNull String alias);

    /**
     * 添加 COUNT(*) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> count(@NonNull String alias);

    /**
     * 添加 COUNT(DISTINCT field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> countDistinct(@NonNull String field, @NonNull String alias);

    /**
     * 添加 SUM(field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> sum(@NonNull String field, @NonNull String alias);

    /**
     * 添加 AVG(field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> avg(@NonNull String field, @NonNull String alias);

    /**
     * 添加 MAX(field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> max(@NonNull String field, @NonNull String alias);

    /**
     * 添加 MIN(field) 聚合，指定别名
     */
    @NonNull AggregateQuery<T> min(@NonNull String field, @NonNull String alias);

    // ==================== 聚合函数（Lambda） ====================

    /**
     * 添加 COUNT(getter) 聚合（Lambda），指定别名
     */
    <R> @NonNull AggregateQuery<T> count(@NonNull SFunction<T, R> getter, @NonNull String alias);

    /**
     * 添加 SUM(getter) 聚合（Lambda），指定别名
     */
    <R extends Number> @NonNull AggregateQuery<T> sum(@NonNull SFunction<T, R> getter, @NonNull String alias);

    /**
     * 添加 AVG(getter) 聚合（Lambda），指定别名
     */
    <R extends Number> @NonNull AggregateQuery<T> avg(@NonNull SFunction<T, R> getter, @NonNull String alias);

    /**
     * 添加 MAX(getter) 聚合（Lambda），指定别名
     */
    <R extends Comparable<?>> @NonNull AggregateQuery<T> max(@NonNull SFunction<T, R> getter, @NonNull String alias);

    /**
     * 添加 MIN(getter) 聚合（Lambda），指定别名
     */
    <R extends Comparable<?>> @NonNull AggregateQuery<T> min(@NonNull SFunction<T, R> getter, @NonNull String alias);

    // ==================== WHERE / HAVING ====================

    /**
     * 设置 WHERE 过滤条件（在 GROUP BY 之前应用）
     */
    @NonNull AggregateQuery<T> where(@NonNull Condition condition);

    /**
     * 设置 HAVING 过滤条件（在 GROUP BY 之后应用）
     * <p>
     * HAVING 条件中的字段名应为聚合别名（如 "userCount"），
     * 而非原始列名。
     */
    @NonNull AggregateQuery<T> having(@NonNull Condition condition);

    // ==================== 排序 ====================

    /**
     * 设置排序
     */
    @NonNull AggregateQuery<T> orderBy(@NonNull Sort sort);

    // ==================== 数据权限 ====================

    /**
     * 设置数据权限
     */
    @NonNull AggregateQuery<T> withDataScope(@NonNull DataScope scope);

    // ==================== 软删除 ====================

    /**
     * 包含已删除记录（软删除场景）
     */
    @NonNull AggregateQuery<T> includeDeleted();

    // ==================== 执行 ====================

    /**
     * 执行聚合查询，返回结果列表
     * <p>
     * 每行结果包含 GROUP BY 字段值和聚合函数值，
     * 通过 {@link AggregateResult#getLong(String)} 等方法按别名访问。
     */
    @NonNull List<AggregateResult> list();

    /**
     * 执行聚合查询，返回单条结果
     * <p>
     * 适用于没有 GROUP BY 时的全局统计。
     *
     * @return 全局聚合结果
     */
    @NonNull AggregateResult single();
}
