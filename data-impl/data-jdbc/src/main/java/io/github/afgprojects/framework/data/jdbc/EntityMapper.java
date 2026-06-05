package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.jdbc.mapper.AbstractResultSetMapper;
import io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveFieldMetadata;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体映射器
 * <p>
 * 负责将 ResultSet 映射到实体对象。继承 {@link AbstractResultSetMapper} 以获得数据库特定类型规范化、
 * 安全 LOB 读取、列名大小写不敏感匹配等公共能力。
 * 类型转换委托给 TypeHandlerRegistry，字段访问使用 ReflectiveFieldMetadata 的 getValue/setValue。
 * <p>
 * <strong>性能优化：</strong>
 * <ul>
 *   <li>缓存 Constructor，避免每次映射时反射获取</li>
 *   <li>缓存 Field，避免每次遍历类层次结构</li>
 *   <li>缓存 columnIndexMap，相同列结构的 ResultSet 复用</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
@Slf4j
public class EntityMapper<T> extends AbstractResultSetMapper<T> {

    private final Class<T> entityClass;
    private final EntityMetadata<T> metadata;

    /**
     * 缓存的构造器（线程安全）
     */
    private final Constructor<T> cachedConstructor;

    /**
     * 字段缓存：类名 + 字段名 -> Field（线程安全）
     */
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 列索引映射缓存：列签名 -> columnIndexMap（线程安全）
     * 列签名由列名列表拼接而成，用于识别相同列结构的 ResultSet
     */
    private static final ConcurrentHashMap<String, Map<String, Integer>> COLUMN_INDEX_CACHE = new ConcurrentHashMap<>();

    public EntityMapper(Class<T> entityClass, EntityMetadata<T> metadata, TypeHandlerRegistry typeHandlerRegistry) {
        super(typeHandlerRegistry);
        this.entityClass = entityClass;
        this.metadata = metadata;
        this.cachedConstructor = getDeclaredConstructor(entityClass);
    }

    public EntityMapper(Class<T> entityClass, EntityMetadata<T> metadata) {
        this(entityClass, metadata, TypeHandlerRegistry.defaultRegistry());
    }

    /**
     * 获取并缓存构造器
     */
    @SuppressWarnings("unchecked")
    private Constructor<T> getDeclaredConstructor(Class<T> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Constructor<T>) constructor;
        } catch (NoSuchMethodException e) {
            throw new EntityMappingException(entityClass, null,
                "No default constructor found for entity " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public T map(ResultSet rs, int rowNum) throws SQLException {
        try {
            T entity = cachedConstructor.newInstance();
            Map<String, Integer> columnIndexMap = getCachedColumnIndexMap(rs);

            for (FieldMetadata field : metadata.getFields()) {
                String columnName = field.getColumnName();
                Integer colIndex = columnIndexMap.get(columnName.toLowerCase());

                if (colIndex == null) {
                    // 尝试别名匹配（属性名 snake_case）
                    String alias = NamingUtils.fieldNameToColumnName(field.getPropertyName());
                    colIndex = columnIndexMap.get(alias.toLowerCase());
                }

                if (colIndex != null) {
                    Object value = readAndNormalizeValue(rs, colIndex);
                    if (value != null) {
                        try {
                            value = typeHandlerRegistry.convert(value, field.getFieldType());
                        } catch (Exception e) {
                            throw new EntityMappingException(entityClass, field.getPropertyName(),
                                String.format("Failed to convert column '%s' value [%s] to type %s: %s",
                                    columnName, value, field.getFieldType().getSimpleName(), e.getMessage()), e);
                        }
                        setFieldValue(entity, field.getPropertyName(), value);
                    }
                }
            }
            // 触发 afterLoad 生命周期回调（类似 JPA @PostLoad）
            LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::afterLoad);
            return entity;
        } catch (SQLException | EntityMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityMappingException(
                "Failed to map ResultSet row to entity " + entityClass.getSimpleName(), null, e);
        }
    }

    /**
     * 获取缓存的列索引映射
     * <p>
     * 使用列签名作为缓存 key，相同列结构的 ResultSet 可以复用映射。
     */
    private Map<String, Integer> getCachedColumnIndexMap(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 构建列签名（列名列表拼接）
        StringBuilder signatureBuilder = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            String label = getColumnLabel(metaData, i);
            if (label != null) {
                signatureBuilder.append(label.toLowerCase()).append(',');
            }
        }
        String signature = signatureBuilder.toString();

        // 从缓存获取或构建新的
        return COLUMN_INDEX_CACHE.computeIfAbsent(signature, s -> {
            Map<String, Integer> map = new java.util.HashMap<>();
            try {
                for (int i = 1; i <= columnCount; i++) {
                    String label = getColumnLabel(metaData, i);
                    if (label != null && !label.isEmpty()) {
                        map.put(label.toLowerCase(), i);
                    }
                }
            } catch (SQLException e) {
                log.debug("Failed to build column index map", e);
            }
            return map;
        });
    }

    /**
     * Spring RowMapper 适配方法
     */
    public T mapRow(ResultSet rs, int rowNum) {
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
            // APT 生成的元数据没有 getValue/setValue，使用原始反射（带缓存）
            try {
                Field declaredField = findDeclaredFieldCached(entityClass, propertyName);
                if (declaredField != null) {
                    declaredField.setAccessible(true);
                    declaredField.set(entity, value);
                }
            } catch (Exception e) {
                throw new EntityMappingException(entityClass, propertyName,
                    "Failed to set field value: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 在类层次结构中查找字段（带缓存）
     */
    private Field findDeclaredFieldCached(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#" + fieldName;
        return FIELD_CACHE.computeIfAbsent(cacheKey, k -> findDeclaredFieldUncached(clazz, fieldName));
    }

    /**
     * 在类层次结构中查找字段（无缓存）
     */
    private Field findDeclaredFieldUncached(Class<?> clazz, String fieldName) {
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