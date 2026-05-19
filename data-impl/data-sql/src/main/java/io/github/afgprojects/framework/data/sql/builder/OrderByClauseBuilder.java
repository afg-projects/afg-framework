package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.Sort;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ORDER BY 子句构建器
 * <p>
 * 处理 ORDER BY 排序
 */
public class OrderByClauseBuilder {

    private final Dialect dialect;
    private Sort orderBySort;

    public OrderByClauseBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    /**
     * ORDER BY 排序
     */
    public OrderByClauseBuilder orderBy(@NonNull Sort sort) {
        this.orderBySort = sort;
        return this;
    }

    /**
     * ORDER BY 排序
     */
    public OrderByClauseBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction) {
        this.orderBySort = direction == Sort.Direction.ASC ? Sort.asc(column) : Sort.desc(column);
        return this;
    }

    /**
     * 构建 ORDER BY 子句 SQL
     *
     * @return ORDER BY 子句字符串（含前导空格）
     */
    public String build() {
        if (orderBySort == null || !orderBySort.isSorted()) {
            return "";
        }
        StringBuilder sql = new StringBuilder(" ORDER BY ");
        List<String> orderParts = new ArrayList<>();
        for (Sort.Order order : orderBySort.getOrders()) {
            orderParts.add(dialect.quoteIdentifier(order.getProperty()) + " " + order.getDirection().getSymbol());
        }
        sql.append(String.join(", ", orderParts));
        return sql.toString();
    }

    /**
     * 是否有排序
     */
    public boolean hasOrderBy() {
        return orderBySort != null && orderBySort.isSorted();
    }

    /**
     * 获取 Sort 对象
     */
    public Sort getSort() {
        return orderBySort;
    }

    /**
     * 设置 Sort 对象
     */
    public void setSort(Sort sort) {
        this.orderBySort = sort;
    }
}