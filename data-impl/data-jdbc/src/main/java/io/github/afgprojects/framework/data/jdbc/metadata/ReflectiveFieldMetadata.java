package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.DatabaseFieldMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 基于反射的数据库字段元数据实现
 * <p>
 * 通过反射从实体类字段提取元数据，支持：
 * <ul>
 *   <li>从 @Column 注解读取列名</li>
 *   <li>从 @Id 注解识别主键</li>
 *   <li>自动 camelCase 转 snake_case</li>
 * </ul>
 *
 * <pre>
 * 示例：
 * {@code
 * @Table(name = "sys_user")
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     @Column(name = "is_deleted")
 *     private Boolean deleted;
 *
 *     private String userName; // 自动映射为 user_name
 * }
 * }
 * </pre>
 */
public class ReflectiveFieldMetadata implements DatabaseFieldMetadata {

    private final String propertyName;
    private final String columnName;
    private final Class<?> fieldType;
    private final boolean idField;
    private final boolean generated;
    private final @Nullable FieldAccessor fieldAccessor;

    /**
     * 基于反射字段创建元数据
     *
     * @param field 反射字段
     */
    public ReflectiveFieldMetadata(Field field) {
        this.propertyName = field.getName();
        this.columnName = inferColumnName(field);
        this.fieldType = field.getType();
        this.idField = detectIdField(field);
        this.generated = this.idField;
        this.fieldAccessor = new io.github.afgprojects.framework.data.jdbc.metadata.CachedFieldAccessor(field);
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String getColumnName() {
        return columnName;
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
        return generated;
    }

    /**
     * 获取字段访问器
     *
     * @return 字段访问器
     */
    public @Nullable FieldAccessor getFieldAccessor() {
        return fieldAccessor;
    }

    /**
     * 获取字段值
     *
     * @param entity 实体对象
     * @return 字段值
     */
    public @Nullable Object getValue(Object entity) {
        if (fieldAccessor == null) {
            throw new IllegalStateException("FieldAccessor not available for field: " + propertyName);
        }
        return fieldAccessor.getValue(entity);
    }

    /**
     * 设置字段值
     *
     * @param entity 实体对象
     * @param value 新值
     */
    public void setValue(Object entity, @Nullable Object value) {
        if (fieldAccessor == null) {
            throw new IllegalStateException("FieldAccessor not available for field: " + propertyName);
        }
        fieldAccessor.setValue(entity, value);
    }

    /**
     * 推断列名
     * <p>
     * 支持以下列名识别方式（按优先级）：
     * <ol>
     *   <li>jakarta.persistence.Column 注解的 name 属性</li>
     *   <li>字段名转换为 snake_case（默认行为）</li>
     * </ol>
     *
     * @param field 反射字段
     * @return 列名
     */
    private static String inferColumnName(Field field) {
        // 1. 检查 @Column 注解
        try {
            jakarta.persistence.Column columnAnnotation = field.getAnnotation(jakarta.persistence.Column.class);
            if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
                return columnAnnotation.name();
            }
        } catch (NoClassDefFoundError e) {
            // JPA API 不在类路径中
        }

        // 2. 默认：camelCase → snake_case
        return toSnakeCase(field.getName());
    }

    /**
     * 检测是否为主键字段
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

    /**
     * camelCase 转 snake_case
     *
     * @param name 属性名
     * @return snake_case 格式的列名
     */
    private static String toSnakeCase(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}
