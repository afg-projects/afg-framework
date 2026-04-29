package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * Microsoft SQL Server 数据库方言
 * <p>
 * 支持 SQL Server 2012+ 的 OFFSET FETCH 分页语法
 */
public class SQLServerDialect implements Dialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.SQLSERVER;
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        // SQL Server 2012+ 使用 OFFSET FETCH 语法
        // 要求必须有 ORDER BY 子句，这里假设原始 SQL 已包含 ORDER BY
        // 如果没有 ORDER BY，需要包装子查询
        if (containsOrderBy(sql)) {
            return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        } else {
            // 包装子查询以确保分页语法正确
            return "SELECT * FROM (" + sql + ") AS _paged OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, @NonNull PageRequest pageable) {
        return getPaginationSql(sql, pageable.offset(), pageable.size());
    }

    /**
     * 检查 SQL 是否包含 ORDER BY 子句
     */
    private boolean containsOrderBy(String sql) {
        String upperSql = sql.toUpperCase();
        return upperSql.contains(" ORDER BY ");
    }

    @Override
    public boolean supportsLimitOffset() {
        return false;  // SQL Server 使用 OFFSET FETCH，不是传统的 LIMIT OFFSET
    }

    @Override
    public boolean supportsFetchFirst() {
        return true;  // SQL Server 2012+ 支持
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "[";
    }

    @Override
    public @NonNull String quoteIdentifier(@NonNull String identifier) {
        // SQL Server 使用方括号，左括号和右括号不同
        return "[" + identifier.replace("]", "]]") + "]";
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
    public boolean supportsAutoIncrement() {
        return true;  // SQL Server 使用 IDENTITY
    }

    @Override
    public boolean supportsSequence() {
        return true;  // SQL Server 2012+ 支持序列
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
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        if (javaType == String.class) return "NVARCHAR(255)";
        if (javaType == Integer.class || javaType == int.class) return "INT";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Double.class || javaType == double.class) return "FLOAT";
        if (javaType == Float.class || javaType == float.class) return "REAL";
        if (javaType == Boolean.class || javaType == boolean.class) return "BIT";
        if (javaType == java.time.LocalDateTime.class) return "DATETIME2";
        if (javaType == java.time.LocalDate.class) return "DATE";
        if (javaType == java.time.LocalTime.class) return "TIME";
        if (javaType == java.math.BigDecimal.class) return "DECIMAL(19,4)";
        if (javaType == byte[].class) return "VARBINARY(MAX)";
        return "NVARCHAR(255)";
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
        // SQL Server 使用 WITH (UPDLOCK, ROWLOCK) 提示来实现行级锁定
        return "WITH (UPDLOCK, ROWLOCK)";
    }
}
