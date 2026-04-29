package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * openGauss 数据库方言
 * <p>
 * openGauss 基于 PostgreSQL，语法与 PostgreSQL 基本一致
 */
public class OpenGaussDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.OPENGAUSS;
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
        // openGauss 有一些特殊类型
        if (javaType == String.class) return "VARCHAR(255)";
        if (javaType == Integer.class || javaType == int.class) return "INTEGER";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Boolean.class || javaType == boolean.class) return "BOOLEAN";
        return super.getSqlType(javaType);
    }
}