package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.exception.EntityMappingException;
import io.github.afgprojects.framework.data.core.mapper.MappingField;
import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DTO 映射器，将 ResultSet 映射到非实体类型
 * <p>
 * 支持两种目标类型：
 * <ul>
 *   <li>Java Record：通过规范构造器映射</li>
 *   <li>POJO：通过无参构造器 + 字段反射映射</li>
 * </ul>
 * 字段匹配策略：同名 + @MappingField 注解
 *
 * @param <R> DTO 类型
 */
public class DtoMapper<R> implements ResultMapper<R> {

    private final Class<R> dtoType;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private final boolean isRecord;
    private volatile Map<String, FieldMapping> fieldMappings;

    public DtoMapper(Class<R> dtoType, TypeHandlerRegistry typeHandlerRegistry) {
        this.dtoType = dtoType;
        this.typeHandlerRegistry = typeHandlerRegistry;
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

        Map<String, FieldMapping> mappings = resolveFieldMappings(rs, components);
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent comp = components[i];
            paramTypes[i] = comp.getType();
            String columnName = mappings.getOrDefault(comp.getName(),
                    new FieldMapping(NamingUtils.toSnakeCase(comp.getName()), comp.getType())).columnName;
            Object value = rs.getObject(columnName);
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
            Map<String, FieldMapping> mappings = resolveFieldMappings(rs, dtoType.getDeclaredFields());

            for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
                FieldMapping mapping = entry.getValue();
                Object value = rs.getObject(mapping.columnName);
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

    private Map<String, FieldMapping> resolveFieldMappings(ResultSet rs, RecordComponent[] components) throws SQLException {
        if (fieldMappings != null) {
            return fieldMappings;
        }
        synchronized (this) {
            if (fieldMappings != null) {
                return fieldMappings;
            }
            Map<String, FieldMapping> mappings = new HashMap<>();
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            Set<String> availableColumns = new HashSet<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String label;
                try {
                    label = metaData.getColumnLabel(i);
                } catch (Exception e) {
                    label = metaData.getColumnName(i);
                }
                if (label != null && !label.isEmpty()) {
                    availableColumns.add(label.toLowerCase());
                }
            }

            for (RecordComponent comp : components) {
                String fieldName = comp.getName();
                String columnName = resolveColumnName(fieldName, availableColumns);
                mappings.put(fieldName, new FieldMapping(columnName, comp.getType()));
            }
            fieldMappings = mappings;
            return mappings;
        }
    }

    private Map<String, FieldMapping> resolveFieldMappings(ResultSet rs, Field[] fields) throws SQLException {
        if (fieldMappings != null) {
            return fieldMappings;
        }
        synchronized (this) {
            if (fieldMappings != null) {
                return fieldMappings;
            }
            Map<String, FieldMapping> mappings = new HashMap<>();
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            Set<String> availableColumns = new HashSet<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String label;
                try {
                    label = metaData.getColumnLabel(i);
                } catch (Exception e) {
                    label = metaData.getColumnName(i);
                }
                if (label != null && !label.isEmpty()) {
                    availableColumns.add(label.toLowerCase());
                }
            }

            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String fieldName = field.getName();
                String columnName = resolveColumnName(field, fieldName, availableColumns);
                mappings.put(fieldName, new FieldMapping(columnName, field.getType()));
            }
            fieldMappings = mappings;
            return mappings;
        }
    }

    private String resolveColumnName(String fieldName, Set<String> availableColumns) {
        // 1. 同名匹配
        String snakeName = NamingUtils.toSnakeCase(fieldName);
        if (availableColumns.contains(fieldName.toLowerCase()) || availableColumns.contains(snakeName.toLowerCase())) {
            return availableColumns.contains(fieldName.toLowerCase()) ? fieldName : snakeName;
        }
        // 2. 尝试 snake_case 匹配
        if (availableColumns.contains(snakeName.toLowerCase())) {
            return snakeName;
        }
        // 3. 回退到 snake_case
        return snakeName;
    }

    private String resolveColumnName(Field field, String fieldName, Set<String> availableColumns) {
        // 1. @MappingField 注解
        MappingField ann = field.getAnnotation(MappingField.class);
        if (ann != null) {
            if (!ann.column().isEmpty()) {
                return ann.column();
            }
            if (!ann.source().isEmpty()) {
                return ann.source();
            }
        }

        // 2. 同名匹配
        String snakeName = NamingUtils.toSnakeCase(fieldName);
        if (availableColumns.contains(fieldName.toLowerCase()) || availableColumns.contains(snakeName.toLowerCase())) {
            return availableColumns.contains(fieldName.toLowerCase()) ? fieldName : snakeName;
        }
        // 3. 尝试 snake_case 匹配
        if (availableColumns.contains(snakeName.toLowerCase())) {
            return snakeName;
        }
        // 4. 回退到 snake_case
        return snakeName;
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

    /**
     * 内部命名工具类（不依赖 data-jdbc 的 NamingUtils）
     */
    private static final class NamingUtils {
        static String toSnakeCase(String camelCase) {
            if (camelCase == null || camelCase.isEmpty()) {
                return camelCase;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toLowerCase(camelCase.charAt(0)));
            for (int i = 1; i < camelCase.length(); i++) {
                char c = camelCase.charAt(i);
                if (Character.isUpperCase(c)) {
                    sb.append('_').append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}