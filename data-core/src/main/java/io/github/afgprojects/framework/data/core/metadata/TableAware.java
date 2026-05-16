package io.github.afgprojects.framework.data.core.metadata;

/**
 * 表名感知特征
 * <p>
 * 实现此接口的元数据可以提供数据库表名信息。
 * 用于数据库场景，支持从实体类获取对应的数据库表名。
 *
 * <pre>
 * 示例：
 * {@code
 * DatabaseEntityMetadata<User> metadata = ...;
 * String tableName = metadata.getTableName(); // "sys_user"
 * }
 * </pre>
 *
 * @see DatabaseEntityMetadata
 */
public interface TableAware {

    /**
     * 获取数据库表名
     *
     * @return 数据库表名，如 "sys_user"
     */
    String getTableName();
}