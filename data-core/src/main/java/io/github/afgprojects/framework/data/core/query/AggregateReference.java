package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 聚合函数引用
 * <p>
 * 用于表示 SELECT 子句中的聚合函数表达式（如 COUNT(*)、SUM(salary)），
 * 以及 HAVING 子句中对聚合结果的引用。
 *
 * @param function 聚合函数类型
 * @param field    字段名（COUNT(*) 时为 null）
 * @param alias    结果别名
 * @author afg
 */
public record AggregateReference(
        @NonNull AggregateFunction function,
        @Nullable String field,
        @NonNull String alias
) {

    /**
     * 创建 COUNT(*) 引用
     *
     * @param alias 结果别名
     * @return COUNT(*) 聚合引用
     */
    public static AggregateReference countAll(@NonNull String alias) {
        return new AggregateReference(AggregateFunction.COUNT, null, alias);
    }

    /**
     * 创建聚合函数引用
     *
     * @param function 聚合函数类型
     * @param field    字段名
     * @param alias    结果别名
     * @return 聚合引用
     */
    public static AggregateReference of(@NonNull AggregateFunction function, @NonNull String field, @NonNull String alias) {
        return new AggregateReference(function, field, alias);
    }

        /**
     * 生成 SQL 表达式（不含别名）
     * <p>
     * 示例：{@code COUNT(*)}, {@code COUNT(DISTINCT col)}, {@code SUM(col)}
     *
     * @return SQL 表达式
     */
    public String toSqlExpression() {
        if (function == AggregateFunction.COUNT && field == null) {
            return "COUNT(*)";
        }
        if (function == AggregateFunction.COUNT_DISTINCT) {
            return "COUNT(DISTINCT " + field + ")";
        }
        return function.getSqlKeyword() + "(" + field + ")";
    }
}
