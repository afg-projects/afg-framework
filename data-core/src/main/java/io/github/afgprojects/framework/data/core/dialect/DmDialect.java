package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * 达梦数据库方言
 * <p>
 * 达梦数据库兼容 Oracle 语法，具有以下特点：
 * <ul>
 *     <li>支持 ROWNUM 分页和 FETCH FIRST 语法</li>
 *     <li>标识符使用双引号</li>
 *     <li>支持序列</li>
 *     <li>数据类型与 Oracle 类似</li>
 * </ul>
 */
public class DmDialect implements Dialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.DM;
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        // 达梦支持 FETCH FIRST 语法（兼容 Oracle 12c+）
        return "SELECT * FROM (" + sql + ") OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, @NonNull PageRequest pageable) {
        return getPaginationSql(sql, pageable.offset(), pageable.size());
    }

    @Override
    public boolean supportsLimitOffset() {
        return false;  // 达梦使用 FETCH FIRST 语法
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
        return "SYSDATE";
    }

    @Override
    public @NonNull String getCurrentDateFunction() {
        return "SYSDATE";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "SYSTIMESTAMP";
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;  // 达梦支持自增列（IDENTITY）
    }

    @Override
    public boolean supportsSequence() {
        return true;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "IDENTITY";
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
    }

    @Override
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        // 达梦数据类型与 Oracle 类似
        if (javaType == String.class) return "VARCHAR2(255)";
        if (javaType == Integer.class || javaType == int.class) return "INTEGER";
        if (javaType == Long.class || javaType == long.class) return "BIGINT";
        if (javaType == Double.class || javaType == double.class) return "DOUBLE";
        if (javaType == Float.class || javaType == float.class) return "FLOAT";
        if (javaType == Boolean.class || javaType == boolean.class) return "BIT";
        if (javaType == java.time.LocalDateTime.class) return "TIMESTAMP";
        if (javaType == java.time.LocalDate.class) return "DATE";
        if (javaType == java.time.LocalTime.class) return "TIME";
        if (javaType == java.math.BigDecimal.class) return "DECIMAL(19,4)";
        if (javaType == byte[].class) return "BLOB";
        return "VARCHAR2(255)";
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
