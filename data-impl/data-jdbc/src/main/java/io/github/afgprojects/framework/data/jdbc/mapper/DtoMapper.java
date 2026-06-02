package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.mapper.MappingField;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTO 映射器，将 ResultSet 映射到非实体类型
 * <p>
 * 继承 {@link AbstractResultSetMapper} 以获得数据库特定类型规范化、安全 LOB 读取、
 * 列名大小写不敏感匹配等公共能力。
 * <p>
 * 支持两种目标类型：
 * <ul>
 *   <li>Java Record：通过规范构造器映射，支持 {@link MappingField} 注解</li>
 *   <li>POJO：通过无参构造器 + 字段反射映射，支持继承字段和 {@link MappingField} 注解</li>
 * </ul>
 * 字段匹配策略：{@link MappingField} 注解 → 同名匹配 → snake_case 匹配
 *
 * @param <R> DTO 类型
 */
@Slf4j
public class DtoMapper<R> extends AbstractResultSetMapper<R> {

    private final Class<R> dtoType;
    private final boolean isRecord;
    private volatile Map<String, FieldMapping> fieldMappings;

    public DtoMapper(Class<R> dtoType, TypeHandlerRegistry typeHandlerRegistry) {
        super(typeHandlerRegistry);
        this.dtoType = dtoType;
        this.isRecord = dtoType.isRecord();
    }

    @Override
    public R map(ResultSet rs, int rowNum) throws SQLException {
        if (isRecord) {
            return mapRecord(rs);
        } else {
            return mapPojo(rs);
        }
    }

    private R mapRecord(ResultSet rs) throws SQLException {
        RecordComponent[] components = dtoType.getRecordComponents();
        if (components.length == 0) {
            try {
                Constructor<R> ctor = dtoType.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception e) {
                throw new EntityMappingException(
                    "Failed to create record instance: " + dtoType.getSimpleName(), null, e);
            }
        }

        Map<String, FieldMapping> mappings = resolveFieldMappingsForRecord(rs, components);
        Map<String, Integer> columnIndexMap = buildColumnIndexMap(rs);
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent comp = components[i];
            paramTypes[i] = comp.getType();
            String columnName = mappings.getOrDefault(comp.getName(),
                new FieldMapping(NamingUtils.fieldNameToColumnName(comp.getName()), comp.getType())).columnName;

            // 优先按索引获取值（避免列名大小写问题）
            Integer colIndex = columnIndexMap.get(columnName.toLowerCase());
            Object value;
            if (colIndex != null) {
                value = readAndNormalizeValue(rs, colIndex);
            } else {
                try {
                    value = rs.getObject(columnName);
                    value = normalizeDatabaseSpecificValue(value);
                } catch (Exception e) {
                    value = null;
                }
            }
            args[i] = typeHandlerRegistry.convert(value, comp.getType());
        }

        try {
            Constructor<R> ctor = dtoType.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new EntityMappingException(
                "Failed to create record instance: " + dtoType.getSimpleName(), null, e);
        }
    }

    private R mapPojo(ResultSet rs) throws SQLException {
        try {
            Constructor<R> ctor = dtoType.getDeclaredConstructor();
            ctor.setAccessible(true);
            R instance = ctor.newInstance();
            List<Field> allFields = getAllFields(dtoType);
            Map<String, FieldMapping> mappings = resolveFieldMappingsForPojo(rs, allFields);
            Map<String, Integer> columnIndexMap = buildColumnIndexMap(rs);

            for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
                FieldMapping mapping = entry.getValue();

                // 优先按索引获取值（避免列名大小写问题）
                Integer colIndex = columnIndexMap.get(mapping.columnName.toLowerCase());
                Object value;
                if (colIndex != null) {
                    value = readAndNormalizeValue(rs, colIndex);
                } else {
                    try {
                        value = rs.getObject(mapping.columnName);
                        value = normalizeDatabaseSpecificValue(value);
                    } catch (Exception e) {
                        value = null;
                    }
                }

                value = typeHandlerRegistry.convert(value, mapping.fieldType);
                Field field = findField(dtoType, entry.getKey());
                if (field != null) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }
            return instance;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new EntityMappingException(
                "Failed to map ResultSet to DTO: " + dtoType.getSimpleName(), null, e);
        }
    }

    // ==================== 字段映射解析 ====================

    private Map<String, FieldMapping> resolveFieldMappingsForRecord(ResultSet rs, RecordComponent[] components) throws SQLException {
        if (fieldMappings != null) {
            return fieldMappings;
        }
        synchronized (this) {
            if (fieldMappings != null) {
                return fieldMappings;
            }
            Map<String, FieldMapping> mappings = new HashMap<>();
            Set<String> availableColumns = getAvailableColumns(rs);

            for (RecordComponent comp : components) {
                String fieldName = comp.getName();
                String columnName = resolveColumnNameForRecordComponent(comp, fieldName, availableColumns);
                mappings.put(fieldName, new FieldMapping(columnName, comp.getType()));
            }
            fieldMappings = mappings;
            return mappings;
        }
    }

    private Map<String, FieldMapping> resolveFieldMappingsForPojo(ResultSet rs, List<Field> fields) throws SQLException {
        if (fieldMappings != null) {
            return fieldMappings;
        }
        synchronized (this) {
            if (fieldMappings != null) {
                return fieldMappings;
            }
            Map<String, FieldMapping> mappings = new HashMap<>();
            Set<String> availableColumns = getAvailableColumns(rs);

            for (Field field : fields) {
                String fieldName = field.getName();
                String columnName = resolveColumnNameForField(field, fieldName, availableColumns);
                mappings.put(fieldName, new FieldMapping(columnName, field.getType()));
            }
            fieldMappings = mappings;
            return mappings;
        }
    }

    private Set<String> getAvailableColumns(ResultSet rs) throws SQLException {
        Set<String> availableColumns = new HashSet<>();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = getColumnLabel(metaData, i);
            if (label != null && !label.isEmpty()) {
                availableColumns.add(label.toLowerCase());
            }
        }
        return availableColumns;
    }

    // ==================== 列名解析 ====================

    /**
     * 解析 RecordComponent 的列名
     * <p>
     * 优先级：{@link MappingField} 注解 → 同名匹配 → snake_case 匹配 → 回退 snake_case
     */
    private String resolveColumnNameForRecordComponent(RecordComponent comp, String fieldName, Set<String> availableColumns) {
        // 1. @MappingField 注解（RecordComponent 上）
        MappingField ann = comp.getAnnotation(MappingField.class);
        if (ann != null) {
            if (!ann.column().isEmpty()) return ann.column();
            if (!ann.source().isEmpty()) return ann.source();
        }

        // 2. 同名匹配
        String snakeName = NamingUtils.fieldNameToColumnName(fieldName);
        if (availableColumns.contains(fieldName.toLowerCase())) {
            return fieldName;
        }
        if (availableColumns.contains(snakeName.toLowerCase())) {
            return snakeName;
        }

        // 3. 回退到 snake_case
        return snakeName;
    }

    /**
     * 解析 Field 的列名
     * <p>
     * 优先级：{@link MappingField} 注解 → 同名匹配 → snake_case 匹配 → 回退 snake_case
     */
    private String resolveColumnNameForField(Field field, String fieldName, Set<String> availableColumns) {
        // 1. @MappingField 注解
        MappingField ann = field.getAnnotation(MappingField.class);
        if (ann != null) {
            if (!ann.column().isEmpty()) return ann.column();
            if (!ann.source().isEmpty()) return ann.source();
        }

        // 2. 同名匹配
        String snakeName = NamingUtils.fieldNameToColumnName(fieldName);
        if (availableColumns.contains(fieldName.toLowerCase())) {
            return fieldName;
        }
        if (availableColumns.contains(snakeName.toLowerCase())) {
            return snakeName;
        }

        // 3. 回退到 snake_case
        return snakeName;
    }

    // ==================== 字段遍历（支持继承） ====================

    /**
     * 获取类及其父类的所有非 static 字段
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    fields.add(f);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private record FieldMapping(String columnName, Class<?> fieldType) {}
}