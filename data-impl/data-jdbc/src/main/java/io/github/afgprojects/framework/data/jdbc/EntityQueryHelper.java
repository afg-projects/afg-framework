package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.jdbc.metadata.CachedFieldAccessor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体查询辅助类
 * <p>
 * 负责 SQL 构建、参数提取、结果映射等辅助功能
 */
class EntityQueryHelper<T> {

    private final Class<T> entityClass;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;

    /**
     * 字段访问器缓存
     */
    private final Map<String, FieldAccessor> fieldAccessorCache = new ConcurrentHashMap<>();

    /**
     * SQL 缓存（延迟初始化）
     */
    private volatile String insertSql;
    private volatile String insertWithIdSql;
    private volatile String updateSql;
    private volatile String updateVersionedSql;
    private volatile String selectBaseSql;

    EntityQueryHelper(Class<T> entityClass, Dialect dialect, EntityMetadata<T> metadata) {
        this.entityClass = entityClass;
        this.dialect = dialect;
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
            // 忽略初始化错误，延迟到使用时再处理
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
            return accessor.getValue(entity);
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

    // ==================== SQL 构建（带缓存） ====================

    /**
     * 构建 INSERT SQL（带缓存）
     */
    String buildInsertSql() {
        if (insertSql == null) {
            synchronized (this) {
                if (insertSql == null) {
                    insertSql = doBuildInsertSql();
                }
            }
        }
        return insertSql;
    }

    private String doBuildInsertSql() {
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
     * 构建包含 ID 的 INSERT SQL（用于预设 ID 的插入，带缓存）
     */
    String buildInsertWithIdSql() {
        if (insertWithIdSql == null) {
            synchronized (this) {
                if (insertWithIdSql == null) {
                    insertWithIdSql = doBuildInsertWithIdSql();
                }
            }
        }
        return insertWithIdSql;
    }

    private String doBuildInsertWithIdSql() {
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
     * 构建 UPDATE SQL（带缓存）
     *
     * @param isVersioned 是否为版本化实体
     * @return UPDATE SQL
     */
    String buildUpdateSql(boolean isVersioned) {
        if (isVersioned) {
            if (updateVersionedSql == null) {
                synchronized (this) {
                    if (updateVersionedSql == null) {
                        updateVersionedSql = doBuildUpdateSql(true);
                    }
                }
            }
            return updateVersionedSql;
        } else {
            if (updateSql == null) {
                synchronized (this) {
                    if (updateSql == null) {
                        updateSql = doBuildUpdateSql(false);
                    }
                }
            }
            return updateSql;
        }
    }

    private String doBuildUpdateSql(boolean isVersioned) {
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
     * 获取基础 SELECT SQL（不带 WHERE，带缓存）
     */
    String getSelectBaseSql() {
        if (selectBaseSql == null) {
            synchronized (this) {
                if (selectBaseSql == null) {
                    StringBuilder sql = new StringBuilder("SELECT ");
                    List<String> columns = new ArrayList<>();
                    for (var field : metadata.getFields()) {
                        columns.add(field.getColumnName());
                    }
                    sql.append(String.join(", ", columns));
                    sql.append(" FROM ").append(dialect.quoteIdentifier(metadata.getTableName()));
                    selectBaseSql = sql.toString();
                }
            }
        }
        return selectBaseSql;
    }

    /**
     * 构建 SELECT SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(@Nullable String whereClause) {
        StringBuilder sql = new StringBuilder(getSelectBaseSql());
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    /**
     * 构建 SELECT SQL（指定字段）
     *
     * @param fields      要查询的字段列表
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(List<String> fields, @Nullable String whereClause) {
        StringBuilder sql = new StringBuilder("SELECT ");
        if (fields == null || fields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", fields));
        }
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
     */
    String columnNameToFieldName(String columnName) {
        StringBuilder fieldName = new StringBuilder();
        boolean nextUpper = false;
        for (char c : columnName.toLowerCase().toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                fieldName.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        String result = fieldName.toString();

        // 阿里规约 boolean 字段特殊处理：
        // 如果转换后的字段名以 "is" 开头，且实体有不带 "is" 的对应字段，则使用不带 "is" 的字段名
        // 例如：is_active → active, is_deleted → deleted
        if (result.startsWith("is") && result.length() > 2 && Character.isUpperCase(result.charAt(2))) {
            String strippedName = Character.toLowerCase(result.charAt(2)) + result.substring(3);
            if (metadata.getField(strippedName) != null) {
                return strippedName;
            }
        }

        return result;
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
    EntityMetadata<T> getMetadata() {
        return metadata;
    }

    /**
     * 获取方言
     */
    Dialect getDialect() {
        return dialect;
    }
}
