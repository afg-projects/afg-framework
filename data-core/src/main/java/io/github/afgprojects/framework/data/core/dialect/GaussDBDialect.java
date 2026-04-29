package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * 高斯数据库方言（GaussDB）
 * <p>
 * GaussDB 是华为推出的企业级分布式数据库，具有以下特点：
 * <ul>
 *     <li>兼容 PostgreSQL 语法</li>
 *     <li>使用 LIMIT/OFFSET 分页</li>
 *     <li>标识符使用双引号</li>
 *     <li>支持序列</li>
 *     <li>有特殊的数据类型（如 Oracle 兼容类型）</li>
 * </ul>
 */
public class GaussDBDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.GAUSSDB;
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
        // GaussDB 数据类型，支持 Oracle 兼容类型
        if (javaType == String.class) return "VARCHAR2(255)";
        if (javaType == Integer.class || javaType == int.class) return "INTEGER";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Double.class || javaType == double.class) return "DOUBLE PRECISION";
        if (javaType == Float.class || javaType == float.class) return "REAL";
        if (javaType == Boolean.class || javaType == boolean.class) return "BOOLEAN";
        if (javaType == java.time.LocalDateTime.class) return "TIMESTAMP";
        if (javaType == java.time.LocalDate.class) return "DATE";
        if (javaType == java.time.LocalTime.class) return "TIME";
        if (javaType == java.math.BigDecimal.class) return "NUMERIC(19,4)";
        if (javaType == byte[].class) return "BYTEA";
        return "VARCHAR2(255)";
    }
}
