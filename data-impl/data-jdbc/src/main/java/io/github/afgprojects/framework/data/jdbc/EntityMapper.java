package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveFieldMetadata;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 实体映射器
 * <p>
 * 负责将 ResultSet 映射到实体对象，实现 ResultMapper 接口。
 * 类型转换委托给 TypeHandlerRegistry，字段访问使用 ReflectiveFieldMetadata 的 getValue/setValue。
 *
 * @param <T> 实体类型
 */
@Slf4j
class EntityMapper<T> implements ResultMapper<T> {

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;
    private final TypeHandlerRegistry typeHandlerRegistry;

    EntityMapper(Class<T> entityClass, EntityMetadata<T> metadata, TypeHandlerRegistry typeHandlerRegistry) {
        this.entityClass = entityClass;
        this.metadata = metadata;
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    EntityMapper(Class<T> entityClass, EntityMetadata<T> metadata) {
        this(entityClass, metadata, TypeHandlerRegistry.defaultRegistry());
    }

    @Override
    public T map(ResultSet rs, int rowNum) throws SQLException {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                // 使用 getColumnLabel（优先返回别名），回退到 getColumnName
                String columnName;
                try {
                    columnName = metaData.getColumnLabel(i);
                } catch (Exception e) {
                    columnName = metaData.getColumnName(i);
                }
                if (columnName == null || columnName.isEmpty()) {
                    columnName = metaData.getColumnName(i);
                }
                String fieldName = NamingUtils.columnNameToFieldName(columnName, metadata);
                Object value = rs.getObject(i);

                // 处理 CLOB/CLOB 类型
                if (value instanceof java.sql.Clob clob) {
                    value = clob.getSubString(1, (int) clob.length());
                } else if (value instanceof java.sql.Blob blob) {
                    value = blob.getBytes(1, (int) blob.length());
                }

                FieldMetadata field = metadata.getField(fieldName);
                if (field != null) {
                    value = typeHandlerRegistry.convert(value, field.getFieldType());
                    setFieldValue(entity, fieldName, value);
                }
            }
            return entity;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityMappingException(
                    "Failed to map ResultSet row to entity " + entityClass.getSimpleName(), null, e);
        }
    }

    /**
     * Spring RowMapper 适配方法
     */
    T mapRow(ResultSet rs, int rowNum) {
        try {
            return map(rs, rowNum);
        } catch (SQLException e) {
            throw new EntityMappingException(
                    "Failed to map ResultSet row to entity " + entityClass.getSimpleName(), null, e);
        }
    }

    private void setFieldValue(T entity, String propertyName, Object value) {
        FieldMetadata field = metadata.getField(propertyName);
        if (field instanceof ReflectiveFieldMetadata reflective) {
            FieldAccessor accessor = reflective.getFieldAccessor();
            if (accessor != null) {
                accessor.setValue(entity, value);
            }
        } else {
            // APT 生成的元数据没有 getValue/setValue，使用原始反射
            try {
                java.lang.reflect.Field declaredField = findDeclaredField(entityClass, propertyName);
                if (declaredField != null) {
                    declaredField.setAccessible(true);
                    declaredField.set(entity, value);
                }
            } catch (Exception e) {
                throw new EntityMappingException(entityClass, propertyName,
                        "Failed to set field value: " + e.getMessage());
            }
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

    // ==================== 工具方法 ====================

    String columnNameToFieldName(String columnName) {
        return NamingUtils.columnNameToFieldName(columnName, metadata);
    }

    String fieldNameToColumnName(String fieldName) {
        return NamingUtils.fieldNameToColumnName(fieldName);
    }

    Class<T> getEntityClass() {
        return entityClass;
    }

    EntityMetadata<T> getMetadata() {
        return metadata;
    }

    TypeHandlerRegistry getTypeHandlerRegistry() {
        return typeHandlerRegistry;
    }
}