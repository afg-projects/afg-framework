package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * 金仓数据库方言（KingbaseES）
 * <p>
 * 金仓数据库基于 PostgreSQL，具有以下特点：
 * <ul>
 *     <li>使用 LIMIT/OFFSET 分页</li>
 *     <li>标识符使用双引号</li>
 *     <li>支持序列</li>
 *     <li>数据类型与 PostgreSQL 类似</li>
 * </ul>
 */
public class KingbaseDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.KINGBASE;
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "CURRENT_TIME";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        // 金仓数据类型与 PostgreSQL 类似
        if (javaType == String.class) return "VARCHAR(255)";
        if (javaType == Integer.class || javaType == int.class) return "INTEGER";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Boolean.class || javaType == boolean.class) return "BOOLEAN";
        return super.getSqlType(javaType);
    }
}
