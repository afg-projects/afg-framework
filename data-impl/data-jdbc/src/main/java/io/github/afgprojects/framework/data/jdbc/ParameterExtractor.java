package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveFieldMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数提取器
 * <p>
 * 负责从实体对象中提取 SQL 参数，使用 ReflectiveFieldMetadata 的 getValue/setValue
 * 消除重复的 fieldAccessorCache。
 *
 * @param <T> 实体类型
 */
@Slf4j
class ParameterExtractor<T> {

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;

    ParameterExtractor(Class<T> entityClass, EntityMetadata<T> metadata) {
        this.entityClass = entityClass;
        this.metadata = metadata;
    }

    // ==================== 字段访问 ====================

    @Nullable Object getFieldValue(T entity, String propertyName) {
        FieldMetadata field = metadata.getField(propertyName);
        if (field instanceof ReflectiveFieldMetadata reflective) {
            FieldAccessor accessor = reflective.getFieldAccessor();
            if (accessor != null) {
                Object value = accessor.getValue(entity);
                if (value instanceof Enum<?> enumValue) {
                    return enumValue.name();
                }
                return convertForJdbc(value);
            }
        }
        // APT 生成的元数据回退到原始反射
        try {
            java.lang.reflect.Field declaredField = findDeclaredField(entityClass, propertyName);
            if (declaredField != null) {
                declaredField.setAccessible(true);
                Object value = declaredField.get(entity);
                if (value instanceof Enum<?> enumValue) {
                    return enumValue.name();
                }
                return convertForJdbc(value);
            }
        } catch (Exception e) {
            throw new EntityMappingException(entityClass, propertyName,
                    "Failed to get field value: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将 Java 时间类型转换为 JDBC 兼容的 SQL类型
     * <p>
     * 委托给 {@link JdbcTypeConverter#convertForJdbc(Object)} 实现。
     * PostgreSQL JDBC 驱动不支持直接 setObject(Instant)，需要转换为 Timestamp。
     * 其他时间类型同理转换以确保跨数据库兼容性。
     */
    private @Nullable Object convertForJdbc(@Nullable Object value) {
        return JdbcTypeConverter.convertForJdbc(value);
    }

    void setFieldValue(T entity, String propertyName, Object value) {
        FieldMetadata field = metadata.getField(propertyName);
        if (field instanceof ReflectiveFieldMetadata reflective) {
            FieldAccessor accessor = reflective.getFieldAccessor();
            if (accessor != null) {
                accessor.setValue(entity, value);
                return;
            }
        }
        // APT 生成的元数据回退到原始反射
        try {
            java.lang.reflect.Field declaredField = findDeclaredField(entityClass, propertyName);
            if (declaredField != null) {
                declaredField.setAccessible(true);
                declaredField.set(entity, value);
            }
        } catch (Exception e) {
            throw new EntityMappingException(entityClass, propertyName,
                    "Failed to set field value: " + e.getMessage(), e);
        }
    }

    private java.lang.reflect.Field findDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    // ==================== 参数提取 ====================

    List<Object> extractInsertParams(T entity) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated()) {
                params.add(getFieldValue(entity, field.getPropertyName()));
            }
        }
        return params;
    }

    List<Object> extractInsertWithIdParams(T entity) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            params.add(getFieldValue(entity, field.getPropertyName()));
        }
        return params;
    }

    List<Object> extractUpdateParams(T entity, boolean isVersioned) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isId() && !field.isGenerated()) {
                if (isVersioned && "version".equals(field.getPropertyName())) {
                    continue;
                }
                params.add(getFieldValue(entity, field.getPropertyName()));
            }
        }
        // 添加 ID 用于 WHERE 子句
        FieldMetadata idField = metadata.getIdField();
        if (idField != null) {
            params.add(getFieldValue(entity, idField.getPropertyName()));
        }

        // 添加版本号用于乐观锁条件
        if (isVersioned) {
            params.add(getFieldValue(entity, "version"));
        }
        return params;
    }

    // ==================== 字段访问 ====================

    @Nullable Object getIdValue(T entity) {
        FieldMetadata idField = metadata.getIdField();
        if (idField != null) {
            return getFieldValue(entity, idField.getPropertyName());
        }
        return null;
    }

    void setIdValue(T entity, long id) {
        FieldMetadata idField = metadata.getIdField();
        if (idField != null) {
            Class<?> fieldType = idField.getFieldType();
            if (fieldType == Long.class || fieldType == long.class) {
                setFieldValue(entity, idField.getPropertyName(), id);
            } else if (fieldType == Integer.class || fieldType == int.class) {
                setFieldValue(entity, idField.getPropertyName(), (int) id);
            } else {
                setFieldValue(entity, idField.getPropertyName(), id);
            }
        }
    }

    Class<T> getEntityClass() {
        return entityClass;
    }

    EntityMetadata<T> getMetadata() {
        return metadata;
    }
}