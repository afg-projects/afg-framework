package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.WindowFunctionBuilder;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 窗口函数 OVER 子句构建器实现
 */
public class WindowFunctionBuilderImpl implements WindowFunctionBuilder {

    private final SqlQueryBuilderImpl queryBuilder;
    private final Dialect dialect;
    private final String windowFunction;
    private List<String> partitionByColumns;
    private Sort orderBySort;

    /**
     * 构造函数
     *
     * @param queryBuilder   父查询构建器
     * @param windowFunction 窗口函数表达式（如 ROW_NUMBER()、RANK() 等）
     */
    public WindowFunctionBuilderImpl(SqlQueryBuilderImpl queryBuilder, String windowFunction) {
        this.queryBuilder = queryBuilder;
        this.dialect = queryBuilder.getDialect();
        this.windowFunction = windowFunction;
        this.partitionByColumns = new ArrayList<>();
    }

    @Override
    public @NonNull WindowFunctionBuilder partitionBy(@NonNull String... columns) {
        for (String column : columns) {
            partitionByColumns.add(column);
        }
        return this;
    }

    @Override
    public @NonNull WindowFunctionBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction) {
        this.orderBySort = direction == Sort.Direction.ASC ? Sort.asc(column) : Sort.desc(column);
        return this;
    }

    @Override
    public @NonNull WindowFunctionBuilder orderBy(@NonNull Sort sort) {
        this.orderBySort = sort;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder end() {
        String expression = buildExpression();
        queryBuilder.addWindowFunctionExpression(expression);
        return queryBuilder;
    }

    /**
     * 构建完整的窗口函数表达式
     */
    private String buildExpression() {
        StringBuilder sb = new StringBuilder(windowFunction);
        sb.append(" OVER (");

        if (!partitionByColumns.isEmpty()) {
            sb.append("PARTITION BY ");
            List<String> quotedColumns = new ArrayList<>();
            for (String column : partitionByColumns) {
                quotedColumns.add(dialect.quoteIdentifier(column));
            }
            sb.append(String.join(", ", quotedColumns));
        }

        if (orderBySort != null && orderBySort.isSorted()) {
            if (!partitionByColumns.isEmpty()) {
                sb.append(" ");
            }
            sb.append("ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (Sort.Order order : orderBySort.getOrders()) {
                orderParts.add(dialect.quoteIdentifier(order.getProperty()) + " " + order.getDirection().getSymbol());
            }
            sb.append(String.join(", ", orderParts));
        }

        sb.append(")");
        return sb.toString();
    }
}
