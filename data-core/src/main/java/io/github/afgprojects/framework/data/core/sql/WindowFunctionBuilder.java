package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.query.Sort;
import org.jspecify.annotations.NonNull;

/**
 * 窗口函数 OVER 子句构建器
 * <p>
 * 用于构建窗口函数的 OVER 子句，支持 PARTITION BY 和 ORDER BY。
 * <p>
 * 使用示例：
 * <pre>
 * // 简单用法
 * builder.select("id", "name")
 *     .rowNumber().over().partitionBy("category").orderBy("score", Direction.DESC)
 *     .from("products")
 *
 * // 多个分区列
 * builder.select("id", "name")
 *     .rank().over().partitionBy("dept", "team").orderBy("salary", Direction.DESC)
 *     .from("employees")
 * </pre>
 */
public interface WindowFunctionBuilder {

    /**
     * 添加 PARTITION BY 列
     *
     * @param columns 分区列名
     * @return this
     */
    @NonNull WindowFunctionBuilder partitionBy(@NonNull String... columns);

    /**
     * 添加 ORDER BY 子句
     *
     * @param column    排序列
     * @param direction 排序方向
     * @return this
     */
    @NonNull WindowFunctionBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction);

    /**
     * 添加 ORDER BY 子句（使用 Sort 对象）
     *
     * @param sort 排序对象
     * @return this
     */
    @NonNull WindowFunctionBuilder orderBy(@NonNull Sort sort);

    /**
     * 完成 OVER 子句构建，返回 SqlQueryBuilder
     *
     * @return SqlQueryBuilder
     */
    @NonNull SqlQueryBuilder end();
}