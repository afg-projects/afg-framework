package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.Sort;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * SQL 查询构建器
 * <p>
 * 链式 Fluent API，与 Condition 条件构建器风格保持一致
 */
public interface SqlQueryBuilder {

    // ==================== SELECT ====================

    /**
     * 指定查询列
     */
    @NonNull SqlQueryBuilder select(@NonNull String... columns);

    /**
     * 指定查询列（Lambda 方式）
     */
    @NonNull SqlQueryBuilder select(@NonNull SFunction<?, ?>... getters);

    /**
     * 指定 DISTINCT
     */
    @NonNull SqlQueryBuilder distinct();

    /**
     * 指定查询所有列
     */
    @NonNull SqlQueryBuilder selectAll();

    // ==================== 聚合函数 ====================

    /**
     * 添加 COUNT(column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder count(@NonNull String column);

    /**
     * 添加 COUNT(*) 聚合函数
     *
     * @return this
     */
    @NonNull SqlQueryBuilder count();

    /**
     * 添加 COUNT(DISTINCT column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder countDistinct(@NonNull String column);

    /**
     * 添加 SUM(column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder sum(@NonNull String column);

    /**
     * 添加 AVG(column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder avg(@NonNull String column);

    /**
     * 添加 MAX(column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder max(@NonNull String column);

    /**
     * 添加 MIN(column) 聚合函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder min(@NonNull String column);

    // ==================== 窗口函数 ====================

    /**
     * 添加 ROW_NUMBER() 窗口函数
     *
     * @return this
     */
    @NonNull SqlQueryBuilder rowNumber();

    /**
     * 添加 RANK() 窗口函数
     *
     * @return this
     */
    @NonNull SqlQueryBuilder rank();

    /**
     * 添加 DENSE_RANK() 窗口函数
     *
     * @return this
     */
    @NonNull SqlQueryBuilder denseRank();

    /**
     * 添加 LEAD(column) 窗口函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder lead(@NonNull String column);

    /**
     * 添加 LEAD(column, offset) 窗口函数
     *
     * @param column 列名
     * @param offset 偏移量
     * @return this
     */
    @NonNull SqlQueryBuilder lead(@NonNull String column, int offset);

    /**
     * 添加 LEAD(column, offset, defaultValue) 窗口函数
     *
     * @param column       列名
     * @param offset       偏移量
     * @param defaultValue 默认值
     * @return this
     */
    @NonNull SqlQueryBuilder lead(@NonNull String column, int offset, Object defaultValue);

    /**
     * 添加 LAG(column) 窗口函数
     *
     * @param column 列名
     * @return this
     */
    @NonNull SqlQueryBuilder lag(@NonNull String column);

    /**
     * 添加 LAG(column, offset) 窗口函数
     *
     * @param column 列名
     * @param offset 偏移量
     * @return this
     */
    @NonNull SqlQueryBuilder lag(@NonNull String column, int offset);

    /**
     * 添加 LAG(column, offset, defaultValue) 窗口函数
     *
     * @param column       列名
     * @param offset       偏移量
     * @param defaultValue 默认值
     * @return this
     */
    @NonNull SqlQueryBuilder lag(@NonNull String column, int offset, Object defaultValue);

    /**
     * 添加 ROW_NUMBER() OVER(...) 窗口函数
     *
     * @param partitionBy PARTITION BY 列（可多个，逗号分隔）
     * @param orderBy     ORDER BY 子句
     * @return this
     */
    @NonNull SqlQueryBuilder rowNumberOver(@NonNull String partitionBy, @NonNull String orderBy);

    /**
     * 添加 ROW_NUMBER() OVER(...) 窗口函数（使用 Sort）
     *
     * @param partitionBy PARTITION BY 列
     * @param orderBy     ORDER BY 排序对象
     * @return this
     */
    @NonNull SqlQueryBuilder rowNumberOver(@NonNull String partitionBy, @NonNull Sort orderBy);

    /**
     * 添加 RANK() OVER(...) 窗口函数
     *
     * @param partitionBy PARTITION BY 列
     * @param orderBy     ORDER BY 子句
     * @return this
     */
    @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull String orderBy);

    /**
     * 添加 RANK() OVER(...) 窗口函数（使用 Sort）
     *
     * @param partitionBy PARTITION BY 列
     * @param orderBy     ORDER BY 排序对象
     * @return this
     */
    @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull Sort orderBy);

    /**
     * 添加 DENSE_RANK() OVER(...) 窗口函数
     *
     * @param partitionBy PARTITION BY 列
     * @param orderBy     ORDER BY 子句
     * @return this
     */
    @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull String orderBy);

    /**
     * 添加 DENSE_RANK() OVER(...) 窗口函数（使用 Sort）
     *
     * @param partitionBy PARTITION BY 列
     * @param orderBy     ORDER BY 排序对象
     * @return this
     */
    @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull Sort orderBy);

    /**
     * 开始构建窗口函数 OVER 子句
     * <p>
     * 使用示例：
     * <pre>
     * builder.select("id", "name")
     *     .rowNumber().over().partitionBy("category").orderBy("score", Direction.DESC)
     *     .from("products")
     * </pre>
     *
     * @return 窗口函数构建器
     */
    @NonNull WindowFunctionBuilder over();

    // ==================== FROM ====================

    /**
     * 指定 FROM 表
     */
    @NonNull SqlQueryBuilder from(@NonNull String table);

    /**
     * 指定 FROM 表（带别名）
     */
    @NonNull SqlQueryBuilder from(@NonNull String table, @NonNull String alias);

    // ==================== JOIN ====================

    /**
     * JOIN 连接
     */
    @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull Condition on);

    /**
     * LEFT JOIN 连接
     */
    @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull Condition on);

    /**
     * RIGHT JOIN 连接
     */
    @NonNull SqlQueryBuilder rightJoin(@NonNull String table, @NonNull Condition on);

    /**
     * INNER JOIN 连接
     */
    @NonNull SqlQueryBuilder innerJoin(@NonNull String table, @NonNull Condition on);

    /**
     * JOIN 连接（带别名）
     */
    @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull String alias, @NonNull Condition on);

    /**
     * LEFT JOIN 连接（带别名）
     */
    @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull String alias, @NonNull Condition on);

    // ==================== WHERE ====================

    /**
     * 设置 WHERE 条件
     */
    @NonNull SqlQueryBuilder where(@NonNull Condition condition);

    /**
     * AND 连接条件
     */
    @NonNull SqlQueryBuilder and(@NonNull Condition condition);

    /**
     * OR 连接条件
     */
    @NonNull SqlQueryBuilder or(@NonNull Condition condition);

    // ==================== GROUP BY / HAVING ====================

    /**
     * GROUP BY 分组
     */
    @NonNull SqlQueryBuilder groupBy(@NonNull String... columns);

    /**
     * HAVING 条件
     */
    @NonNull SqlQueryBuilder having(@NonNull Condition condition);

    // ==================== ORDER BY ====================

    /**
     * ORDER BY 排序
     */
    @NonNull SqlQueryBuilder orderBy(@NonNull Sort sort);

    /**
     * ORDER BY 排序
     */
    @NonNull SqlQueryBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction);

    // ==================== LIMIT / OFFSET ====================

    /**
     * LIMIT 限制
     */
    @NonNull SqlQueryBuilder limit(long limit);

    /**
     * OFFSET 偏移
     */
    @NonNull SqlQueryBuilder offset(long offset);

    /**
     * 分页
     */
    @NonNull SqlQueryBuilder page(long page, long size);

    /**
     * 分页
     */
    @NonNull SqlQueryBuilder page(@NonNull PageRequest pageable);

    // ==================== CTE (Common Table Expression) ====================

    /**
     * 添加非递归 CTE（WITH 子句）
     *
     * @param name CTE 名称
     * @param cte  CTE 查询定义
     * @return this
     */
    @NonNull SqlQueryBuilder with(@NonNull String name, @NonNull SqlQueryBuilder cte);

    /**
     * 添加递归 CTE（WITH RECURSIVE 子句）
     *
     * @param name CTE 名称
     * @param cte  CTE 查询定义
     * @return this
     */
    @NonNull SqlQueryBuilder withRecursive(@NonNull String name, @NonNull SqlQueryBuilder cte);

    /**
     * 添加带列名的 CTE（WITH name(column1, column2, ...) AS (...)）
     *
     * @param name    CTE 名称
     * @param columns 列名数组
     * @param cte     CTE 查询定义
     * @return this
     */
    @NonNull SqlQueryBuilder withColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte);

    /**
     * 添加带列名的递归 CTE
     *
     * @param name    CTE 名称
     * @param columns 列名数组
     * @param cte     CTE 查询定义
     * @return this
     */
    @NonNull SqlQueryBuilder withRecursiveColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte);

    // ==================== 子查询 ====================

    /**
     * FROM 子查询
     */
    @NonNull SqlQueryBuilder fromSubquery(@NonNull SqlQueryBuilder subquery, @NonNull String alias);

    /**
     * EXISTS 子查询
     */
    @NonNull SqlQueryBuilder exists(@NonNull SqlQueryBuilder subquery);

    /**
     * NOT EXISTS 子查询
     */
    @NonNull SqlQueryBuilder notExists(@NonNull SqlQueryBuilder subquery);

    // ==================== 构建 ====================

    /**
     * 构建 SQL 字符串
     */
    @NonNull String toSql();

    /**
     * 获取参数值
     */
    @NonNull List<Object> getParameters();

    // ==================== 执行 ====================

    /**
     * 执行查询，返回列表
     */
    <T> @NonNull List<T> fetch(@NonNull Class<T> resultType);

    /**
     * 执行查询，返回单条记录
     */
    <T> @NonNull Optional<T> fetchOne(@NonNull Class<T> resultType);

    /**
     * 执行查询，返回第一条记录
     */
    <T> @NonNull Optional<T> fetchFirst(@NonNull Class<T> resultType);

    /**
     * 执行分页查询
     */
    <T> @NonNull Page<T> fetchPage(@NonNull Class<T> resultType, @NonNull PageRequest pageable);

    /**
     * 执行 COUNT 查询
     */
    long fetchCount();
}