package io.github.afgprojects.framework.data.core.metadata;

import org.jspecify.annotations.Nullable;

/**
 * 数据库字段元数据
 * <p>
 * 扩展 FieldMetadata，用于数据库场景的字段元数据。
 * 继承基础字段元数据的所有属性，包括：
 * <ul>
 *   <li>属性名（getPropertyName）</li>
 *   <li>列名（getColumnName）</li>
 *   <li>字段类型（getFieldType）</li>
 *   <li>主键标识（isId）</li>
 *   <li>自动生成标识（isGenerated）</li>
 * </ul>
 *
 * <p>
 * 此接口新增数据库特有方法，提供 JDBC 类型、可空性、默认值、
 * 列长度、精度、唯一性等数据库列级别的元信息。
 *
 * <pre>
 * 示例：
 * {@code
 * DatabaseEntityMetadata<User> entityMetadata = ...;
 * DatabaseFieldMetadata fieldMetadata = entityMetadata.getField("userName");
 *
 * String propertyName = fieldMetadata.getPropertyName(); // "userName"
 * String columnName = fieldMetadata.getColumnName();      // "user_name"
 * Class<?> type = fieldMetadata.getFieldType();           // String.class
 * boolean isId = fieldMetadata.isId();                    // false
 * int jdbcType = fieldMetadata.getJdbcType();             // Types.VARCHAR
 * boolean nullable = fieldMetadata.isNullable();          // false
 * String defaultValue = fieldMetadata.getDefaultValue();  // null
 * }
 * </pre>
 *
 * @see DatabaseEntityMetadata
 * @see FieldMetadata
 */
public interface DatabaseFieldMetadata extends FieldMetadata {

    /**
     * 获取 JDBC 类型
     * <p>
     * 返回 java.sql.Types 中定义的 JDBC 类型常量。
     *
     * @return JDBC 类型，默认 {@link java.sql.Types#NULL} 表示未指定
     */
    default int getJdbcType() {
        return java.sql.Types.NULL;
    }

    /**
     * 是否允许 null 值
     *
     * @return 是否允许 null，默认 true
     */
    default boolean isNullable() {
        return true;
    }

    /**
     * 获取默认值
     *
     * @return 默认值，null 表示未指定
     */
    default @Nullable String getDefaultValue() {
        return null;
    }

    /**
     * 获取列长度
     * <p>
     * 适用于字符串类型字段（VARCHAR、CHAR 等）。
     *
     * @return 列长度，0 表示未指定
     */
    default int getLength() {
        return 0;
    }

    /**
     * 获取精度
     * <p>
     * 适用于数值类型字段（DECIMAL、NUMERIC 等）。
     *
     * @return 精度，0 表示未指定
     */
    default int getPrecision() {
        return 0;
    }

    /**
     * 获取小数位数
     * <p>
     * 适用于数值类型字段（DECIMAL、NUMERIC 等）。
     *
     * @return 小数位数，0 表示未指定
     */
    default int getScale() {
        return 0;
    }

    /**
     * 是否唯一
     *
     * @return 是否唯一，默认 false
     */
    default boolean isUnique() {
        return false;
    }

    /**
     * 是否可插入
     * <p>
     * 标记该字段是否参与 INSERT 操作。
     * 自动生成的主键字段通常不可插入。
     *
     * @return 是否可插入，默认 true
     */
    default boolean isInsertable() {
        return true;
    }

    /**
     * 是否可更新
     * <p>
     * 标记该字段是否参与 UPDATE 操作。
     * 自动生成的主键字段通常不可更新。
     *
     * @return 是否可更新，默认 true
     */
    default boolean isUpdatable() {
        return true;
    }

    /**
     * 获取列定义
     * <p>
     * 返回数据库特定的 DDL 片段，如 "VARCHAR(255) NOT NULL DEFAULT 'active'"。
     *
     * @return 列定义，null 表示未指定
     */
    default @Nullable String getColumnDefinition() {
        return null;
    }
}
