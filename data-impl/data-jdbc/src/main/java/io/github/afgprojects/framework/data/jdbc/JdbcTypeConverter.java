package io.github.afgprojects.framework.data.jdbc;

import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.*;

/**
 * JDBC 类型转换器
 * <p>
 * 将 Java 时间类型转换为 JDBC 兼容的 SQL 类型。
 * PostgreSQL JDBC 驱动不支持直接 setObject(Instant)，需要转换为 Timestamp。
 * 其他时间类型同理转换以确保跨数据库兼容性。
 * <p>
 * 此类是线程安全的，所有方法都是静态方法。
 */
public final class JdbcTypeConverter {

    private JdbcTypeConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将 Java 值转换为 JDBC 兼容类型
     * <p>
     * 主要处理时间类型转换：
     * <ul>
     *   <li>{@link Instant} -> {@link Timestamp}</li>
     *   <li>{@link LocalDateTime} -> {@link Timestamp}</li>
     *   <li>{@link LocalDate} -> {@link java.sql.Date}</li>
     *   <li>{@link LocalTime} -> {@link java.sql.Time}</li>
     *   <li>{@link OffsetDateTime} -> {@link Timestamp}</li>
     *   <li>{@link ZonedDateTime} -> {@link Timestamp}</li>
     * </ul>
     *
     * @param value 要转换的值
     * @return 转换后的值，如果输入为 null 则返回 null
     */
    public static @Nullable Object convertForJdbc(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        if (value instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        }
        if (value instanceof LocalDate ld) {
            return java.sql.Date.valueOf(ld);
        }
        if (value instanceof LocalTime lt) {
            return java.sql.Time.valueOf(lt);
        }
        if (value instanceof OffsetDateTime odt) {
            return Timestamp.from(odt.toInstant());
        }
        if (value instanceof ZonedDateTime zdt) {
            return Timestamp.from(zdt.toInstant());
        }
        return value;
    }

    /**
     * 批量转换参数列表中的所有值
     * <p>
     * 对列表中的每个元素调用 {@link #convertForJdbc(Object)}。
     * 返回新列表，不修改原列表。
     *
     * @param params 参数列表
     * @return 转换后的新参数列表
     */
    public static java.util.List<Object> convertParamsForJdbc(java.util.List<Object> params) {
        if (params == null || params.isEmpty()) {
            return params;
        }
        java.util.List<Object> converted = new java.util.ArrayList<>(params.size());
        for (Object param : params) {
            converted.add(convertForJdbc(param));
        }
        return converted;
    }
}
