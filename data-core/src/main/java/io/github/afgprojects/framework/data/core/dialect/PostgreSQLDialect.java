package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * PostgreSQL 方言实现。
 */
public class PostgreSQLDialect extends AbstractDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "\"";
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "GENERATED ALWAYS AS IDENTITY";
    }

    @Override
    public boolean supportsFetchFirst() {
        return true;
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT nextval('" + sequenceName + "')";
    }

    @Override
    protected void configureSqlTypeMap(java.util.Map<Class<?>, String> map) {
        map.put(String.class, "VARCHAR(255)");
        map.put(Integer.class, "INTEGER");
        map.put(int.class, "INTEGER");
        map.put(Double.class, "DOUBLE PRECISION");
        map.put(double.class, "DOUBLE PRECISION");
        map.put(Float.class, "REAL");
        map.put(float.class, "REAL");
        map.put(Boolean.class, "BOOLEAN");
        map.put(boolean.class, "BOOLEAN");
        map.put(java.math.BigDecimal.class, "NUMERIC(19,4)");
        map.put(byte[].class, "BYTEA");
        map.put(java.util.UUID.class, "UUID");
    }
}
