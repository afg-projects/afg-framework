package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * SQL Server 方言实现。
 */
public class SQLServerDialect extends AbstractDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.SQLSERVER;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "[";
    }

    @Override
    public @NonNull String quoteIdentifier(@NonNull String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public boolean supportsLimitOffset() {
        return false;
    }

    @Override
    public boolean supportsFetchFirst() {
        return true;
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        String upperSql = sql.toUpperCase().trim();
        if (!upperSql.contains("ORDER BY")) {
            return "SELECT * FROM (" + sql + ") AS _paged OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "CONVERT(TIME, GETDATE())";
    }

    @Override
    public @NonNull String getCurrentDateFunction() {
        return "CONVERT(DATE, GETDATE())";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "GETDATE()";
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "IDENTITY(1,1)";
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT NEXT VALUE FOR " + quoteIdentifier(sequenceName);
    }

    @Override
    public @NonNull String getForUpdateSyntax() {
        return "WITH (UPDLOCK, ROWLOCK)";
    }

    @Override
    protected void configureSqlTypeMap(java.util.Map<Class<?>, String> map) {
        map.put(String.class, "NVARCHAR(255)");
        map.put(Double.class, "FLOAT");
        map.put(double.class, "FLOAT");
        map.put(Float.class, "REAL");
        map.put(float.class, "REAL");
        map.put(Boolean.class, "BIT");
        map.put(boolean.class, "BIT");
        map.put(java.math.BigDecimal.class, "DECIMAL(19,4)");
        map.put(byte[].class, "VARBINARY(MAX)");
        map.put(java.time.LocalDateTime.class, "DATETIME2");
        map.put(java.time.OffsetDateTime.class, "DATETIMEOFFSET");
        map.put(Object.class, "NVARCHAR(255)");
    }
}
