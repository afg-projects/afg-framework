package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * PostgreSQL 数据库方言
 */
public class PostgreSQLDialect implements Dialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, @NonNull PageRequest pageable) {
        return getPaginationSql(sql, pageable.offset(), pageable.size());
    }

    @Override
    public boolean supportsLimitOffset() {
        return true;
    }

    @Override
    public boolean supportsFetchFirst() {
        return true;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "\"";
    }

    @Override
    public @NonNull String quoteIdentifier(@NonNull String identifier) {
        return getIdentifierQuote() + identifier + getIdentifierQuote();
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "CURRENT_TIME";
    }

    @Override
    public @NonNull String getCurrentDateFunction() {
        return "CURRENT_DATE";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "CURRENT_TIMESTAMP";
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;  // PostgreSQL 10+ 支持 GENERATED ALWAYS AS IDENTITY
    }

    @Override
    public boolean supportsSequence() {
        return true;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "GENERATED ALWAYS AS IDENTITY";
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT nextval('" + sequenceName + "')";
    }

    @Override
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        if (javaType == String.class) return "VARCHAR(255)";
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
        return "VARCHAR(255)";
    }

    @Override
    public @NonNull String getLikeWildcard() {
        return "%";
    }

    @Override
    public boolean supportsForUpdate() {
        return true;
    }

    @Override
    public @NonNull String getForUpdateSyntax() {
        return "FOR UPDATE";
    }
}