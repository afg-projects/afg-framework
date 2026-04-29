package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * MySQL 数据库方言
 */
public class MySQLDialect implements Dialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
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
        return false;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "`";
    }

    @Override
    public @NonNull String quoteIdentifier(@NonNull String identifier) {
        return getIdentifierQuote() + identifier + getIdentifierQuote();
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "NOW()";
    }

    @Override
    public @NonNull String getCurrentDateFunction() {
        return "CURDATE()";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "NOW()";
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;
    }

    @Override
    public boolean supportsSequence() {
        return true;  // MySQL 8.0+ supports sequences
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "AUTO_INCREMENT";
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT NEXTVAL(" + quoteIdentifier(sequenceName) + ")";
    }

    @Override
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        if (javaType == String.class) return "VARCHAR(255)";
        if (javaType == Integer.class || javaType == int.class) return "INT";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Double.class || javaType == double.class) return "DOUBLE";
        if (javaType == Float.class || javaType == float.class) return "FLOAT";
        if (javaType == Boolean.class || javaType == boolean.class) return "TINYINT(1)";
        if (javaType == java.time.LocalDateTime.class) return "DATETIME";
        if (javaType == java.time.LocalDate.class) return "DATE";
        if (javaType == java.time.LocalTime.class) return "TIME";
        if (javaType == java.math.BigDecimal.class) return "DECIMAL(19,4)";
        if (javaType == byte[].class) return "BLOB";
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