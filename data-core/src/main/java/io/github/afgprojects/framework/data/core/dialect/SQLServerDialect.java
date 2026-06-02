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
    public @NonNull String getLimitSql(@NonNull String sql, long limit) {
        // SQL Server 的 OFFSET...FETCH 语法需要 ORDER BY
        // 简单检测：统计 SELECT 和 ORDER BY 的出现位置来避免误判子查询中的 ORDER BY
        String upperSql = sql.toUpperCase();
        int lastOrderBy = upperSql.lastIndexOf(" ORDER BY ");
        int lastCloseParen = upperSql.lastIndexOf(')');

        if (lastOrderBy > 0 && lastOrderBy > lastCloseParen) {
            // ORDER BY 不在子查询中，直接追加
            return sql + " OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
        // 无 ORDER BY 或 ORDER BY 在子查询中，包装子查询
        return "SELECT * FROM (" + sql + ") AS _limited OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY";
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

    // ==================== JSON 支持（SQL Server 2016+） ====================

    @Override
    public @NonNull String getJsonContainsExpression(@NonNull String column) {
        // SQL Server 无原生 JSON_CONTAINS，降级为 LIKE
        return "(" + column + " LIKE '%' + ? + '%')";
    }

    @Override
    public @NonNull String getJsonContainedExpression(@NonNull String column) {
        // SQL Server 无原生 JSON_CONTAINED，降级为 LIKE 反向
        return "(? LIKE '%' + " + column + " + '%')";
    }

    @Override
    public @NonNull String getJsonPathExpression(@NonNull String column) {
        // SQL Server 2016+: JSON_VALUE 使用绑定变量传入路径
        return "JSON_VALUE(" + column + ", ?)";
    }
}
