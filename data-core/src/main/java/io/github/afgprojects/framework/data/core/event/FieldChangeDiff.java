package io.github.afgprojects.framework.data.core.event;

import org.jspecify.annotations.Nullable;

/**
 * 字段级变更差异记录。
 * <p>
 * 用于审计追踪，记录实体字段在更新操作中的变更前后值。
 *
 * @param fieldName  字段名（Java 属性名）
 * @param columnName 数据库列名
 * @param oldValue   变更前的值（可读字符串表示），新增时为 null
 * @param newValue   变更后的值（可读字符串表示），删除时为 null
 */
public record FieldChangeDiff(
    String fieldName,
    String columnName,
    @Nullable String oldValue,
    @Nullable String newValue
) {

    /**
     * 创建一个字段变更差异记录。
     */
    public static FieldChangeDiff of(String fieldName, String columnName,
                                     @Nullable String oldValue, @Nullable String newValue) {
        return new FieldChangeDiff(fieldName, columnName, oldValue, newValue);
    }
}
