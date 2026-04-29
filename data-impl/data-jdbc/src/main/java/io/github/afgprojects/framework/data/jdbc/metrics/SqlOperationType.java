package io.github.afgprojects.framework.data.jdbc.metrics;

import org.jspecify.annotations.NonNull;

/**
 * SQL 操作类型枚举
 *
 * @since 1.0.0
 */
public enum SqlOperationType {

    /**
     * SELECT 查询操作
     */
    SELECT("SELECT"),

    /**
     * INSERT 插入操作
     */
    INSERT("INSERT"),

    /**
     * UPDATE 更新操作
     */
    UPDATE("UPDATE"),

    /**
     * DELETE 删除操作
     */
    DELETE("DELETE"),

    /**
     * 其他操作（如 COUNT、EXISTS 等）
     */
    OTHER("OTHER");

    private final String name;

    SqlOperationType(@NonNull String name) {
        this.name = name;
    }

    /**
     * 获取操作类型名称
     *
     * @return 操作类型名称
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * 根据 SQL 语句推断操作类型
     *
     * @param sql SQL 语句
     * @return 操作类型
     */
    @NonNull
    public static SqlOperationType fromSql(@NonNull String sql) {
        String normalizedSql = sql.trim().toUpperCase();
        if (normalizedSql.startsWith("SELECT")) {
            return SELECT;
        } else if (normalizedSql.startsWith("INSERT")) {
            return INSERT;
        } else if (normalizedSql.startsWith("UPDATE")) {
            return UPDATE;
        } else if (normalizedSql.startsWith("DELETE")) {
            return DELETE;
        }
        return OTHER;
    }
}
