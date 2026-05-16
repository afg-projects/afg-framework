package io.github.afgprojects.framework.data.core.metadata;

/**
 * 主键感知特征
 * <p>
 * 实现此接口的元数据可以提供主键字段信息。
 * 用于数据库场景，支持识别实体的主键字段。
 *
 * <pre>
 * 示例：
 * {@code
 * DatabaseEntityMetadata<User> metadata = ...;
 * FieldMetadata idField = metadata.getIdField();
 * String idPropertyName = idField.getPropertyName(); // "id"
 * }
 * </pre>
 *
 * @see DatabaseEntityMetadata
 * @see DatabaseFieldMetadata
 */
public interface IdAware {

    /**
     * 获取主键字段元数据
     *
     * @return 主键字段元数据，不存在返回 null
     */
    FieldMetadata getIdField();
}