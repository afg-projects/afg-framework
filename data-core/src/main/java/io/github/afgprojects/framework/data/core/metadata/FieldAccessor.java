package io.github.afgprojects.framework.data.core.metadata;

import org.jspecify.annotations.Nullable;

/**
 * 字段访问器接口
 * <p>
 * 提供统一的字段访问抽象，支持反射优化实现。
 * 通过缓存 Field 对象和预调用 setAccessible()，避免重复查找和权限检查开销。
 *
 * <p>使用示例:
 * <pre>{@code
 * FieldAccessor accessor = new CachedFieldAccessor(field);
 * Object value = accessor.getValue(entity);
 * accessor.setValue(entity, newValue);
 * }</pre>
 */
public interface FieldAccessor {

    /**
     * 获取字段值
     *
     * @param entity 实体对象
     * @return 字段值，可能为 null
     * @throws IllegalArgumentException 如果实体类型不匹配
     * @throws RuntimeException 如果访问失败
     */
    @Nullable Object getValue(Object entity);

    /**
     * 设置字段值
     *
     * @param entity 实体对象
     * @param value 新值，可以为 null
     * @throws IllegalArgumentException 如果实体类型不匹配或值类型不兼容
     * @throws RuntimeException 如果访问失败
     */
    void setValue(Object entity, @Nullable Object value);

    /**
     * 获取字段类型
     *
     * @return 字段类型
     */
    Class<?> getFieldType();

    /**
     * 获取字段名称
     *
     * @return 字段名称
     */
    String getFieldName();
}
