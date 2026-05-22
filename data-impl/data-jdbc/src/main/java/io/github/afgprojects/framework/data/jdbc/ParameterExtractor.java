package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.CachedFieldAccessor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参数提取器
 * <p>
 * 负责从实体对象中提取 SQL 参数，包括 INSERT、UPDATE 参数以及 ID 值的获取和设置。
 * 从 EntityQueryHelper 中提取，专注于参数提取和字段访问逻辑。
 *
 * @param <T> 实体类型
 */
@Slf4j
class ParameterExtractor<T> {

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;

    /**
     * 字段访问器缓存
     */
    private final Map<String, FieldAccessor> fieldAccessorCache = new ConcurrentHashMap<>();

    ParameterExtractor(Class<T> entityClass, EntityMetadata<T> metadata) {
        this.entityClass = entityClass;
        this.metadata = metadata;
        // 初始化字段访问器缓存
        initFieldAccessors();
    }

    /**
     * 初始化字段访问器缓存
     */
    private void initFieldAccessors() {
        try {
            for (FieldMetadata fieldMeta : metadata.getFields()) {
                String propertyName = fieldMeta.getPropertyName();
                Field field = findDeclaredField(entityClass, propertyName);
                if (field != null) {
                    fieldAccessorCache.put(propertyName, new CachedFieldAccessor(field));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to initialize field accessors for entity {}: {}",
                    entityClass.getSimpleName(), e.getMessage());
        }
    }

    /**
     * 在类层次结构中查找字段
     */
    private Field findDeclaredField(Class<?> clazz, String fieldName) {
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

    /**
     * 获取字段访问器
     */
    private FieldAccessor getFieldAccessor(String propertyName) {
        return fieldAccessorCache.get(propertyName);
    }

    /**
     * 获取字段值
     */
    private Object getFieldValue(T entity, String propertyName) {
        FieldAccessor accessor = getFieldAccessor(propertyName);
        if (accessor != null) {
            Object value = accessor.getValue(entity);
            // 处理枚举类型：转换为字符串名称
            if (value instanceof Enum<?> enumValue) {
                return enumValue.name();
            }
            return value;
        }
        return null;
    }

    /**
     * 设置字段值
     */
    private void setFieldValue(T entity, String propertyName, Object value) {
        FieldAccessor accessor = getFieldAccessor(propertyName);
        if (accessor != null) {
            accessor.setValue(entity, value);
        }
    }

    // ==================== 参数提取 ====================

    /**
     * 提取 INSERT 参数
     */
    List<Object> extractInsertParams(T entity) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated()) {
                params.add(getFieldValue(entity, field.getPropertyName()));
            }
        }
        return params;
    }

    /**
     * 提取包含 ID 的 INSERT 参数（用于预设 ID 的插入）
     */
    List<Object> extractInsertWithIdParams(T entity) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            params.add(getFieldValue(entity, field.getPropertyName()));
        }
        return params;
    }

    /**
     * 提取 UPDATE 参数
     *
     * @param entity      实体对象
     * @param isVersioned 是否为版本化实体
     * @return 参数列表
     */
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

    /**
     * 获取 ID 值
     */
    @Nullable Object getIdValue(T entity) {
        FieldMetadata idField = metadata.getIdField();
        if (idField != null) {
            return getFieldValue(entity, idField.getPropertyName());
        }
        return null;
    }

    /**
     * 设置 ID 值
     */
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

    /**
     * 获取实体类
     */
    Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取元数据
     */
    EntityMetadata<T> getMetadata() {
        return metadata;
    }
}
