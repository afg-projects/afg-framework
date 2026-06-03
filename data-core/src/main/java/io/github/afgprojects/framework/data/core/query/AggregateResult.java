package io.github.afgprojects.framework.data.core.query;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 聚合查询结果
 * <p>
 * 封装单行聚合查询结果，通过别名访问各聚合列的值。
 * 支持 GROUP BY 场景下同时获取分组字段值和聚合函数值。
 *
 * @author afg
 */
public final class AggregateResult {

    private final Map<String, Object> data;

    public AggregateResult(@NonNull Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    /**
     * 获取所有列的值（不可变）
     */
    public @NonNull Map<String, Object> asMap() {
        return data;
    }

    /**
     * 获取指定别名的值
     */
    public @Nullable Object get(@NonNull String alias) {
        return data.get(alias);
    }

    /**
     * 获取 Long 值（适用于 COUNT 结果）
     */
    public @Nullable Long getLong(@NonNull String alias) {
        Object value = data.get(alias);
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.valueOf(value.toString());
    }

    /**
     * 获取 BigDecimal 值（适用于 SUM/AVG 结果）
     */
    public @Nullable BigDecimal getBigDecimal(@NonNull String alias) {
        Object value = data.get(alias);
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    /**
     * 获取 Double 值（适用于 AVG 结果）
     */
    public @Nullable Double getDouble(@NonNull String alias) {
        Object value = data.get(alias);
        if (value == null) return null;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        return Double.valueOf(value.toString());
    }

    /**
     * 获取 String 值（适用于 GROUP BY 字段）
     */
    public @Nullable String getString(@NonNull String alias) {
        Object value = data.get(alias);
        if (value == null) return null;
        return value.toString();
    }

    /**
     * 获取 Integer 值
     */
    public @Nullable Integer getInteger(@NonNull String alias) {
        Object value = data.get(alias);
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.valueOf(value.toString());
    }

    @Override
    public String toString() {
        return "AggregateResult" + data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregateResult that)) return false;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
