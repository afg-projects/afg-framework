package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * MySQL 方言实现。
 */
public class MySQLDialect extends AbstractDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "`";
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public @NonNull String getCurrentDateFunction() {
        return "CURDATE()";
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "AUTO_INCREMENT";
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "NOW()";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "NOW()";
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT NEXTVAL(" + quoteIdentifier(sequenceName) + ")";
    }

    @Override
    protected void configureSqlTypeMap(java.util.Map<Class<?>, String> map) {
        map.put(java.math.BigDecimal.class, "DECIMAL(19,4)");
        map.put(Boolean.class, "TINYINT(1)");
        map.put(boolean.class, "TINYINT(1)");
        map.put(java.time.LocalDateTime.class, "DATETIME");
    }

    @Override
    public @NonNull String getJsonContainsExpression(@NonNull String column) {
        return "JSON_CONTAINS(" + column + ", ?)";
    }

    @Override
    public @NonNull String getJsonContainedExpression(@NonNull String column) {
        return "JSON_CONTAINS(?, " + column + ")";
    }

    @Override
    public @NonNull String getJsonPathExpression(@NonNull String column) {
        return "JSON_EXTRACT(" + column + ", ?) IS NOT NULL";
    }
}
