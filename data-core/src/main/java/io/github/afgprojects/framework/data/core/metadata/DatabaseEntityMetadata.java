package io.github.afgprojects.framework.data.core.metadata;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据库实体元数据
 * <p>
 * 组合 EntityMetadata 和数据库相关特征接口，提供完整的数据库实体元数据。
 * 实现此接口的类可以提供：
 * <ul>
 *   <li>基础实体信息（来自 EntityMetadata）</li>
 *   <li>表名信息（来自 TableAware）</li>
 *   <li>主键字段信息（来自 IdAware）</li>
 *   <li>属性名到列名的映射（来自 ColumnNameAware）</li>
 * </ul>
 *
 * <p>
 * 此接口采用特征组合模式，通过继承多个特征接口实现功能组合。
 * 相比单一接口继承，这种设计具有以下优势：
 * <ul>
 *   <li>灵活性：可以按需组合不同特征</li>
 *   <li>可扩展性：新增特征不影响现有接口</li>
 *   <li>类型安全：编译时检查特征支持</li>
 * </ul>
 *
 * <pre>
 * 示例：
 * {@code
 * DatabaseEntityMetadata<User> metadata = ...;
 *
 * // 基础信息
 * Class<User> entityClass = metadata.getEntityClass();
 *
 * // 表名（来自 TableAware）
 * String tableName = metadata.getTableName(); // "sys_user"
 *
 * // 主键字段（来自 IdAware）
 * DatabaseFieldMetadata idField = metadata.getIdField();
 *
 * // 列名映射（来自 ColumnNameAware）
 * String columnName = metadata.getColumnName("userName"); // "user_name"
 *
 * // 字段列表（返回 DatabaseFieldMetadata）
 * List<DatabaseFieldMetadata> fields = metadata.getFields();
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @see DatabaseFieldMetadata
 * @see EntityMetadata
 * @see TableAware
 * @see IdAware
 * @see ColumnNameAware
 */
public interface DatabaseEntityMetadata<T>
    extends EntityMetadata<T>, TableAware, IdAware, ColumnNameAware {

    /**
     * 获取所有字段元数据
     * <p>
     * 继承自 EntityMetadata，返回 FieldMetadata 类型。
     * 实现类应确保返回的元素都是 DatabaseFieldMetadata 实例。
     *
     * @return 字段元数据列表
     */
    // 注意：由于 Java 泛型不变性，无法将 List<DatabaseFieldMetadata> 作为 List<FieldMetadata> 的子类型
    // 实现类应确保 getFields() 返回的元素都是 DatabaseFieldMetadata 实例
    // 使用方可通过 getField(propertyName) 获取 DatabaseFieldMetadata 类型

    /**
     * 根据属性名获取字段元数据
     * <p>
     * 重写 EntityMetadata 的方法，返回 DatabaseFieldMetadata 类型。
     *
     * @param propertyName 属性名
     * @return 数据库字段元数据，不存在返回 null
     */
    @Override
    @Nullable DatabaseFieldMetadata getField(String propertyName);

    /**
     * 获取主键字段元数据
     * <p>
     * 重写 IdAware 的方法，返回 DatabaseFieldMetadata 类型。
     * 默认实现从字段列表中查找标记为主键的字段。
     *
     * @return 主键字段元数据，不存在返回 null
     */
    @Override
    default @Nullable DatabaseFieldMetadata getIdField() {
        // 注意：getFields() 返回 List<FieldMetadata>，但实现类应确保元素为 DatabaseFieldMetadata
        // 使用 FieldMetadata.isId() 进行过滤（DatabaseFieldMetadata 继承了此方法）
        return (DatabaseFieldMetadata) getFields().stream()
            .filter(FieldMetadata::isId)
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据属性名获取数据库列名
     * <p>
     * 重写 ColumnNameAware 的方法。
     * 默认实现从字段元数据中获取列名，如果字段不存在则转换为 snake_case。
     *
     * @param propertyName 属性名
     * @return 数据库列名
     */
    @Override
    default String getColumnName(String propertyName) {
        DatabaseFieldMetadata field = getField(propertyName);
        if (field != null) {
            return field.getColumnName();
        }
        // 降级：转换为 snake_case
        return toSnakeCase(propertyName);
    }

    /**
     * camelCase 转 snake_case
     *
     * @param name 属性名
     * @return snake_case 格式的列名
     */
    private String toSnakeCase(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}
