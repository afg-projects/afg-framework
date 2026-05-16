package io.github.afgprojects.framework.data.core.metadata;

/**
 * 列名感知特征（数据库场景）
 * <p>
 * 实现此接口的元数据可以根据属性名获取数据库列名。
 * 支持从实体属性名映射到数据库列名，处理以下场景：
 *
 * <ul>
 *   <li>camelCase 属性名转换为 snake_case 列名</li>
 *   <li>Boolean 类型属性的 is_ 前缀处理</li>
 *   <li>@Column 注解指定的自定义列名</li>
 * </ul>
 *
 * <pre>
 * 示例：
 * {@code
 * DatabaseEntityMetadata<User> metadata = ...;
 *
 * // 普通字段：userName → user_name
 * String columnName1 = metadata.getColumnName("userName");
 *
 * // Boolean 字段：deleted → is_deleted（通过 @Column(name="is_deleted")）
 * String columnName2 = metadata.getColumnName("deleted");
 * }
 * </pre>
 *
 * @see DatabaseEntityMetadata
 * @see DatabaseFieldMetadata
 */
public interface ColumnNameAware {

    /**
     * 根据属性名获取数据库列名
     *
     * @param propertyName 属性名（Java 字段名），如 "userName"
     * @return 数据库列名，如 "user_name"
     */
    String getColumnName(String propertyName);
}