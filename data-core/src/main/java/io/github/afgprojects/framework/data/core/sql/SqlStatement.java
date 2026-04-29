package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * SQL 语句对象
 * <p>
 * 表示解析后的 SQL 语句结构
 */
public interface SqlStatement {

    /**
     * 获取 SQL 类型
     */
    @NonNull SqlType getType();

    /**
     * 获取涉及的表名列表
     */
    @NonNull List<String> getTables();

    /**
     * 获取涉及的列名列表
     */
    @NonNull List<String> getColumns();

    /**
     * 获取 WHERE 条件
     */
    @NonNull String getWhereClause();

    /**
     * 获取数据库类型
     */
    @NonNull DatabaseType getDatabaseType();

    /**
     * 转换为 SQL 字符串
     */
    @NonNull String toSql();

    /**
     * SQL 类型枚举
     */
    enum SqlType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        CREATE,
        ALTER,
        DROP,
        OTHER
    }
}