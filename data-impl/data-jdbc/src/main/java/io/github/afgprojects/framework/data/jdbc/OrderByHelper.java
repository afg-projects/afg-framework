package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Sort;

/**
 * ORDER BY 子句构建工具类
 * <p>
 * 提取自 JdbcEntityQuery 和 JdbcProjectedQuery 中的重复逻辑，
 * 统一处理字段名到列名的转换和 SQL 拼接。
 */
final class OrderByHelper {

    private OrderByHelper() {
        // 工具类不允许实例化
    }

    /**
     * 根据 Sort 构建 ORDER BY 子句
     *
     * @param sort     排序条件
     * @param dialect  数据库方言
     * @param metadata 实体元数据
     * @return ORDER BY 子句字符串（不含 "ORDER BY " 前缀），无排序时返回空字符串
     */
    static String buildOrderByClause(Sort sort, Dialect dialect, EntityMetadata<?> metadata) {
        if (sort == null || !sort.isSorted()) {
            return "";
        }
        return buildOrderByFromOrders(sort.getOrders(), dialect, metadata);
    }

    /**
     * 根据 PageRequest 中的排序信息构建 ORDER BY 子句
     *
     * @param pageRequest 分页请求
     * @param dialect     数据库方言
     * @param metadata    实体元数据
     * @return ORDER BY 子句字符串（不含 "ORDER BY " 前缀），无排序时返回空字符串
     */
    static String buildOrderByFromPageRequest(PageRequest pageRequest, Dialect dialect, EntityMetadata<?> metadata) {
        if (pageRequest.sort() == null || !pageRequest.sort().isSorted()) {
            return "";
        }
        return buildOrderByFromOrders(pageRequest.sort().getOrders(), dialect, metadata);
    }

    /**
     * 从 Sort.Order 列表构建 ORDER BY 子句
     */
    private static String buildOrderByFromOrders(Iterable<Sort.Order> orders, Dialect dialect, EntityMetadata<?> metadata) {
        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : orders) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            // 将字段名转换为数据库列名
            String fieldName = order.getProperty();
            var fieldMetadata = metadata.getField(fieldName);
            String columnName = fieldMetadata != null ? fieldMetadata.getColumnName() : fieldName;
            sb.append(dialect.quoteIdentifier(columnName));
            if (order.isDescending()) {
                sb.append(" DESC");
            }
        }
        return sb.toString();
    }
}
