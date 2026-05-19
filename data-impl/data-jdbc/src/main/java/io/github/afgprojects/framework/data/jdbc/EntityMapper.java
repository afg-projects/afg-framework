package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.CachedFieldAccessor;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体映射器
 * <p>
 * 负责将 ResultSet 映射到实体对象，包括类型转换和字段名转换。
 * 从 EntityQueryHelper 中提取，专注于结果集映射逻辑。
 *
 * @param <T> 实体类型
 */
@Slf4j
class EntityMapper<T> {

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;

    /**
     * 字段访问器缓存
     */
    private final Map<String, FieldAccessor> fieldAccessorCache = new ConcurrentHashMap<>();

    EntityMapper(Class<T> entityClass, EntityMetadata<T> metadata) {
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
     * 设置字段值
     */
    private void setFieldValue(T entity, String propertyName, Object value) {
        FieldAccessor accessor = getFieldAccessor(propertyName);
        if (accessor != null) {
            accessor.setValue(entity, value);
        }
    }

    // ==================== 结果映射 ====================

    /**
     * 映射结果集行到实体
     */
    T mapRow(ResultSet rs, int rowNum) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                String fieldName = NamingUtils.columnNameToFieldName(columnName, metadata);
                Object value = rs.getObject(i);

                // 处理 CLOB 类型（Oracle 等数据库）
                if (value instanceof java.sql.Clob clob) {
                    value = clob.getSubString(1, (int) clob.length());
                }

                FieldMetadata field = metadata.getField(fieldName);
                if (field != null) {
                    value = convertValue(value, field.getFieldType());
                    setFieldValue(entity, fieldName, value);
                }
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map row to entity", e);
        }
    }

    /**
     * 转换值类型以匹配字段类型
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        // 处理 Long/Integer 转换
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number num) {
                return num.longValue();
            }
        }
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number num) {
                return num.intValue();
            }
        }
        // 处理 String 转换
        if (targetType == String.class && !(value instanceof String)) {
            return value.toString();
        }
        // 处理 LocalDateTime 转换
        if (targetType == LocalDateTime.class && value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        // 处理 LocalDate 转换
        if (targetType == java.time.LocalDate.class && value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        // 处理枚举类型转换
        if (targetType.isEnum() && value instanceof Number num) {
            int codeValue = num.intValue();
            // 尝试通过 code 字段匹配
            for (Object enumConstant : targetType.getEnumConstants()) {
                try {
                    java.lang.reflect.Method getCodeMethod = enumConstant.getClass().getMethod("getCode");
                    Object codeResult = getCodeMethod.invoke(enumConstant);
                    if (codeResult instanceof Number codeNum && codeNum.intValue() == codeValue) {
                        return enumConstant;
                    }
                } catch (NoSuchMethodException e) {
                    // 枚举没有 getCode 方法，跳出循环使用 ordinal 匹配
                    break;
                } catch (ReflectiveOperationException e) {
                    // 反射调用失败，忽略并继续尝试下一个枚举值
                }
            }
            // 回退到 ordinal 匹配
            Object[] enumConstants = targetType.getEnumConstants();
            if (codeValue >= 0 && codeValue < enumConstants.length) {
                return enumConstants[codeValue];
            }
        }
        return value;
    }

    // ==================== 工具方法 ====================

    /**
     * 列名转字段名（snake_case to camelCase）
     * <p>
     * 特殊处理阿里规约 boolean 字段：
     * <ul>
     *   <li>列名 is_active → 字段名 active（如果实体有 active 字段）</li>
     *   <li>列名 is_deleted → 字段名 deleted（如果实体有 deleted 字段）</li>
     * </ul>
     *
     * @see NamingUtils#columnNameToFieldName(String, EntityMetadata)
     */
    String columnNameToFieldName(String columnName) {
        return NamingUtils.columnNameToFieldName(columnName, metadata);
    }

    /**
     * 字段名转列名（camelCase to snake_case）
     *
     * @see NamingUtils#fieldNameToColumnName(String)
     */
    String fieldNameToColumnName(String fieldName) {
        return NamingUtils.fieldNameToColumnName(fieldName);
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
