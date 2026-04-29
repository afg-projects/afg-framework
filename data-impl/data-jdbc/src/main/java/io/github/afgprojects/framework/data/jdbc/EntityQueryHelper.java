package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleFieldMetadata;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 实体查询辅助类
 * <p>
 * 负责 SQL 构建、参数提取、结果映射等辅助功能
 */
class EntityQueryHelper<T> {

    private final Class<T> entityClass;
    private final Dialect dialect;
    private final SimpleEntityMetadata<T> metadata;

    EntityQueryHelper(Class<T> entityClass, Dialect dialect, SimpleEntityMetadata<T> metadata) {
        this.entityClass = entityClass;
        this.dialect = dialect;
        this.metadata = metadata;
    }

    // ==================== SQL 构建 ====================

    /**
     * 构建 INSERT SQL
     */
    String buildInsertSql() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated()) {
                columns.add(field.getColumnName());
                placeholders.add("?");
            }
        }
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (").append(String.join(", ", placeholders)).append(")");
        return sql.toString();
    }

    /**
     * 构建包含 ID 的 INSERT SQL（用于预设 ID 的插入）
     */
    String buildInsertWithIdSql() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (var field : metadata.getFields()) {
            columns.add(field.getColumnName());
            placeholders.add("?");
        }
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (").append(String.join(", ", placeholders)).append(")");
        return sql.toString();
    }

    /**
     * 构建 UPDATE SQL
     *
     * @param isVersioned 是否为版本化实体
     * @return UPDATE SQL
     */
    String buildUpdateSql(boolean isVersioned) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" SET ");

        List<String> setParts = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isId() && !field.isGenerated()) {
                if (isVersioned && "version".equals(field.getPropertyName())) {
                    setParts.add(field.getColumnName() + " = " + field.getColumnName() + " + 1");
                } else {
                    setParts.add(field.getColumnName() + " = ?");
                }
            }
        }
        sql.append(String.join(", ", setParts));
        sql.append(" WHERE id = ?");

        if (isVersioned) {
            sql.append(" AND version = ?");
        }

        return sql.toString();
    }

    /**
     * 构建 SELECT SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(@Nullable String whereClause) {
        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> columns = new ArrayList<>();
        for (var field : metadata.getFields()) {
            columns.add(field.getColumnName());
        }
        sql.append(String.join(", ", columns));
        sql.append(" FROM ").append(dialect.quoteIdentifier(metadata.getTableName()));

        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    /**
     * 构建 DELETE SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return DELETE SQL
     */
    String buildDeleteSql(@Nullable String whereClause) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName()));

        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    // ==================== 参数提取 ====================

    /**
     * 提取 INSERT 参数
     */
    List<Object> extractInsertParams(T entity) {
        List<Object> params = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated() && field instanceof SimpleFieldMetadata simpleField) {
                params.add(simpleField.getValue(entity));
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
            if (field instanceof SimpleFieldMetadata simpleField) {
                params.add(simpleField.getValue(entity));
            }
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
                if (field instanceof SimpleFieldMetadata simpleField) {
                    params.add(simpleField.getValue(entity));
                }
            }
        }
        // 添加 ID 用于 WHERE 子句
        SimpleFieldMetadata idField = getIdField();
        if (idField != null) {
            params.add(idField.getValue(entity));
        }

        // 添加版本号用于乐观锁条件
        if (isVersioned) {
            SimpleFieldMetadata versionField = findFieldByName("version");
            if (versionField != null) {
                params.add(versionField.getValue(entity));
            }
        }
        return params;
    }

    // ==================== 字段访问 ====================

    /**
     * 获取 ID 字段
     */
    @Nullable SimpleFieldMetadata getIdField() {
        return metadata.getIdField() instanceof SimpleFieldMetadata sf ? sf : null;
    }

    /**
     * 获取 ID 值
     */
    @Nullable Object getIdValue(T entity) {
        SimpleFieldMetadata idField = getIdField();
        if (idField != null) {
            return idField.getValue(entity);
        }
        return null;
    }

    /**
     * 设置 ID 值
     */
    void setIdValue(T entity, long id) {
        SimpleFieldMetadata idField = getIdField();
        if (idField != null) {
            Class<?> fieldType = idField.getFieldType();
            if (fieldType == Long.class || fieldType == long.class) {
                idField.setValue(entity, id);
            } else if (fieldType == Integer.class || fieldType == int.class) {
                idField.setValue(entity, (int) id);
            } else {
                idField.setValue(entity, id);
            }
        }
    }

    /**
     * 根据属性名查找字段
     */
    @Nullable SimpleFieldMetadata findFieldByName(String propertyName) {
        for (var field : metadata.getFields()) {
            if (field.getPropertyName().equals(propertyName) && field instanceof SimpleFieldMetadata sf) {
                return sf;
            }
        }
        return null;
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
                String fieldName = columnNameToFieldName(columnName);
                Object value = rs.getObject(i);

                SimpleFieldMetadata field = findFieldByName(fieldName);
                if (field != null) {
                    value = convertValue(value, field.getFieldType());
                    field.setValue(entity, value);
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
        return value;
    }

    // ==================== 工具方法 ====================

    /**
     * 列名转字段名（snake_case to camelCase）
     */
    String columnNameToFieldName(String columnName) {
        StringBuilder fieldName = new StringBuilder();
        boolean nextUpper = false;
        for (char c : columnName.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                fieldName.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return fieldName.toString();
    }

    /**
     * 字段名转列名（camelCase to snake_case）
     */
    String fieldNameToColumnName(String fieldName) {
        StringBuilder columnName = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(c));
        }
        return columnName.toString();
    }

    /**
     * 设置实体字段值
     */
    void setFieldValue(T entity, String fieldName, Object value) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to set field '" + fieldName + "' on entity " + entityClass.getSimpleName(),
                    e
            );
        }
    }

    /**
     * 获取表名
     */
    String getTableName() {
        return metadata.getTableName();
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
    SimpleEntityMetadata<T> getMetadata() {
        return metadata;
    }

    /**
     * 获取方言
     */
    Dialect getDialect() {
        return dialect;
    }
}
