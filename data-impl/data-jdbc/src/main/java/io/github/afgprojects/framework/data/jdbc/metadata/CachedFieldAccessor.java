package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * 缓存 Field 对象的字段访问器实现
 * <p>
 * 通过缓存 Field 对象和预调用 setAccessible()，避免重复查找和权限检查开销。
 * 适用于频繁访问同一字段的场景（如批量操作、结果集映射等）。
 *
 * <p>性能优化点：
 * <ul>
 *   <li>缓存 Field 对象，避免重复调用 getDeclaredField()</li>
 *   <li>构造时调用 setAccessible(true)，避免每次访问时的权限检查</li>
 *   <li>提供直接的 get/set 方法，减少方法调用层次</li>
 * </ul>
 */
public class CachedFieldAccessor implements FieldAccessor {

    private final Field field;
    private final Class<?> fieldType;
    private final String fieldName;

    /**
     * 构造缓存字段访问器
     *
     * @param field 反射字段对象
     */
    public CachedFieldAccessor(Field field) {
        this.field = field;
        this.fieldType = field.getType();
        this.fieldName = field.getName();
        // 预调用 setAccessible，避免每次访问时的权限检查
        field.setAccessible(true);
    }

    @Override
    public @Nullable Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    @Override
    public void setValue(Object entity, @Nullable Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        } catch (IllegalArgumentException e) {
            // 提供更详细的错误信息
            String expectedType = fieldType.getName();
            String actualType = value != null ? value.getClass().getName() : "null";
            throw new IllegalArgumentException(
                    String.format("Cannot set field '%s' of type %s with value of type %s",
                            fieldName, expectedType, actualType), e);
        }
    }

    @Override
    public Class<?> getFieldType() {
        return fieldType;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 获取底层的 Field 对象
     * <p>
     * 用于需要直接访问 Field API 的场景（如注解处理）
     *
     * @return Field 对象
     */
    public Field getField() {
        return field;
    }
}
