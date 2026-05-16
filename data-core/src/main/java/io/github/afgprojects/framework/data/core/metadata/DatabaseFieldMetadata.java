package io.github.afgprojects.framework.data.core.metadata;

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
 * 此接口主要用于类型系统，标识该元数据适用于数据库场景。
 * 与缓存、搜索等其他场景的元数据接口形成对照。
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
 * }
 * </pre>
 *
 * @see DatabaseEntityMetadata
 * @see FieldMetadata
 */
public interface DatabaseFieldMetadata extends FieldMetadata {
    // 继承 FieldMetadata 的所有方法
    // 此接口主要用于类型标识，区分数据库场景与其他场景（缓存、搜索等）
}
