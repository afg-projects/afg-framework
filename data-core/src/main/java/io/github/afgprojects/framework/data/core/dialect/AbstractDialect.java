package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 方言抽象基类，提供通用的 SQL 类型映射和标识符引用逻辑。
 * 子类只需覆盖差异部分即可。
 */
public abstract class AbstractDialect implements Dialect {

    private final Map<Class<?>, String> sqlTypeMap;

    protected AbstractDialect() {
        this.sqlTypeMap = createDefaultSqlTypeMap();
        configureSqlTypeMap(sqlTypeMap);
    }

    /**
     * 创建默认的 Java 类型到 SQL 类型的映射表。
     * 子类可通过 {@link #configureSqlTypeMap(Map)} 覆盖特定类型的映射。
     */
    private static Map<Class<?>, String> createDefaultSqlTypeMap() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "VARCHAR(255)");
        map.put(Character.class, "CHAR(1)");
        map.put(char.class, "CHAR(1)");
        map.put(Integer.class, "INT");
        map.put(int.class, "INT");
        map.put(Long.class, "BIGINT");
        map.put(long.class, "BIGINT");
        map.put(Short.class, "SMALLINT");
        map.put(short.class, "SMALLINT");
        map.put(Byte.class, "TINYINT");
        map.put(byte.class, "TINYINT");
        map.put(Float.class, "FLOAT");
        map.put(float.class, "FLOAT");
        map.put(Double.class, "DOUBLE");
        map.put(double.class, "DOUBLE");
        map.put(Boolean.class, "BOOLEAN");
        map.put(boolean.class, "BOOLEAN");
        map.put(BigDecimal.class, "DECIMAL(19,2)");
        map.put(BigInteger.class, "DECIMAL(19,2)");
        map.put(java.sql.Date.class, "DATE");
        map.put(java.sql.Time.class, "TIME");
        map.put(Timestamp.class, "TIMESTAMP");
        map.put(LocalDate.class, "DATE");
        map.put(LocalTime.class, "TIME");
        map.put(LocalDateTime.class, "TIMESTAMP");
        map.put(OffsetDateTime.class, "TIMESTAMP WITH TIME ZONE");
        map.put(ZonedDateTime.class, "TIMESTAMP WITH TIME ZONE");
        map.put(Instant.class, "TIMESTAMP");
        map.put(UUID.class, "VARCHAR(36)");
        map.put(byte[].class, "BLOB");
        return map;
    }

    /**
     * 子类覆盖此方法以自定义 SQL 类型映射。
     * 示例：map.put(String.class, "VARCHAR2(4000)");
     *
     * @param map 可修改的类型映射表
     */
    protected void configureSqlTypeMap(Map<Class<?>, String> map) {
        // 默认无自定义
    }

    @Override
    public @NonNull String getSqlType(@NonNull Class<?> javaType) {
        String sqlType = sqlTypeMap.get(javaType);
        if (sqlType != null) {
            return sqlType;
        }
        // 枚举类型默认映射为 VARCHAR
        if (Enum.class.isAssignableFrom(javaType)) {
            return "VARCHAR(50)";
        }
        return "VARCHAR(255)";
    }

    @Override
    public @NonNull String quoteIdentifier(@NonNull String identifier) {
        String quote = getIdentifierQuote();
        if (quote.isEmpty()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        throw new UnsupportedOperationException(
                getDatabaseType().getCode() + " does not support sequences");
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
    public @NonNull String getLimitSql(@NonNull String sql, long limit) {
        if (supportsFetchFirst()) {
            return "SELECT * FROM (" + sql + ") FETCH FIRST " + limit + " ROWS ONLY";
        }
        return sql + " LIMIT " + limit;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "AUTO_INCREMENT";
    }

    @Override
    public @NonNull String getForUpdateSyntax() {
        return "FOR UPDATE";
    }
}
