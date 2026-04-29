package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 简单字段元数据实现
 */
public class SimpleFieldMetadata implements FieldMetadata {

    private final String propertyName;
    private final Class<?> fieldType;
    private final boolean idField;
    private final Field reflectedField;
    private final @Nullable FieldAccessor fieldAccessor;

    public SimpleFieldMetadata(String propertyName, Class<?> fieldType) {
        this.propertyName = propertyName;
        this.fieldType = fieldType;
        this.idField = "id".equals(propertyName);
        this.reflectedField = null;
        this.fieldAccessor = null;
    }

    /**
     * 基于反射创建字段元数据，支持注解识别主键
     *
     * @param reflectedField 反射字段
     */
    public SimpleFieldMetadata(Field reflectedField) {
        this.propertyName = reflectedField.getName();
        this.fieldType = reflectedField.getType();
        this.reflectedField = reflectedField;
        this.idField = detectIdField(reflectedField);
        // 创建缓存访问器以优化反射性能
        this.fieldAccessor = new CachedFieldAccessor(reflectedField);
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String getColumnName() {
        // 转换为下划线命名
        StringBuilder columnName = new StringBuilder();
        for (int i = 0; i < propertyName.length(); i++) {
            char c = propertyName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(c));
        }
        return columnName.toString();
    }

    @Override
    public Class<?> getFieldType() {
        return fieldType;
    }

    @Override
    public boolean isId() {
        return idField;
    }

    @Override
    public boolean isGenerated() {
        return isId();
    }

    /**
     * 获取字段访问器
     * <p>
     * 用于高效地获取/设置字段值，避免重复的反射查找开销。
     *
     * @return 字段访问器，如果字段未通过反射创建则返回 null
     */
    public @Nullable FieldAccessor getFieldAccessor() {
        return fieldAccessor;
    }

    /**
     * 获取字段值
     * <p>
     * 便捷方法，使用缓存访问器获取字段值。
     *
     * @param entity 实体对象
     * @return 字段值
     * @throws IllegalStateException 如果字段访问器不可用
     */
    public @Nullable Object getValue(Object entity) {
        if (fieldAccessor == null) {
            throw new IllegalStateException("FieldAccessor not available for field: " + propertyName);
        }
        return fieldAccessor.getValue(entity);
    }

    /**
     * 设置字段值
     * <p>
     * 便捷方法，使用缓存访问器设置字段值。
     *
     * @param entity 实体对象
     * @param value 新值
     * @throws IllegalStateException 如果字段访问器不可用
     */
    public void setValue(Object entity, @Nullable Object value) {
        if (fieldAccessor == null) {
            throw new IllegalStateException("FieldAccessor not available for field: " + propertyName);
        }
        fieldAccessor.setValue(entity, value);
    }

    /**
     * 检测字段是否为主键字段
     * <p>
     * 支持以下主键识别方式（按优先级）：
     * <ol>
     *   <li>jakarta.persistence.Id 注解</li>
     *   <li>jakarta.persistence.EmbeddedId 注解</li>
     *   <li>org.springframework.data.annotation.Id 注解</li>
     *   <li>com.baomidou.mybatisplus.annotation.TableId 注解</li>
     *   <li>字段名为 "id"（后备方案）</li>
     * </ol>
     *
     * @param field 反射字段
     * @return 是否为主键字段
     */
    private static boolean detectIdField(Field field) {
        // 1. 检查 Jakarta Persistence @Id 注解
        if (hasAnnotation(field, "jakarta.persistence.Id")) {
            return true;
        }

        // 2. 检查 Jakarta Persistence @EmbeddedId 注解（复合主键）
        if (hasAnnotation(field, "jakarta.persistence.EmbeddedId")) {
            return true;
        }

        // 3. 检查 Spring Data @Id 注解
        if (hasAnnotation(field, "org.springframework.data.annotation.Id")) {
            return true;
        }

        // 4. 检查 MyBatis-Plus @TableId 注解
        if (hasAnnotation(field, "com.baomidou.mybatisplus.annotation.TableId")) {
            return true;
        }

        // 5. 后备方案：字段名为 "id"
        return "id".equals(field.getName());
    }

    /**
     * 检查字段是否具有指定注解
     * <p>
     * 使用字符串类名检查，避免对可选依赖的编译期依赖。
     *
     * @param field 反射字段
     * @param annotationClassName 注解全限定名
     * @return 是否具有该注解
     */
    private static boolean hasAnnotation(Field field, String annotationClassName) {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return true;
            }
        }
        return false;
    }
}
