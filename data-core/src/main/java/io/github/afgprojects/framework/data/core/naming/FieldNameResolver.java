package io.github.afgprojects.framework.data.core.naming;

import io.github.afgprojects.framework.commons.naming.NamingUtils;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.metadata.ColumnNameAware;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;

/**
 * 字段名解析器
 * <p>
 * Lambda → 列名转换的统一入口。
 * 支持通过 APT 生成的元数据或运行时反射获取列名。
 *
 * <p>转换流程：
 *
 * <ol>
 *   <li>从 Lambda 方法引用提取属性名（如 {@code User::getUserName} → "userName"）</li>
 *   <li>获取实体元数据（优先 APT 生成，降级反射）</li>
 *   <li>如果元数据支持 {@link ColumnNameAware}，使用元数据转换</li>
 *   <li>降级：camelCase → snake_case</li>
 * </ol>
 *
 * <pre>
 * 使用示例：
 * {@code
 * FieldNameResolver resolver = new FieldNameResolver(new EntityMetadataCache());
 *
 * // Lambda → 列名
 * String columnName = resolver.resolveColumnName(User.class, User::getUserName);
 * // 结果：如果 User 有 @Column(name="user_name") 或元数据，返回 "user_name"
 * //       否则降级到 snake_case 转换，返回 "user_name"
 *
 * // Boolean 字段处理
 * String deletedColumn = resolver.resolveColumnName(User.class, User::getDeleted);
 * // 如果 User.deleted 有 @Column(name="is_deleted")，返回 "is_deleted"
 * }
 * </pre>
 *
 * @see EntityMetadataCache
 * @see ColumnNameAware
 * @see Conditions#getFieldName(SFunction)
 */
public class FieldNameResolver {

    private final EntityMetadataCache cache;

    /**
     * 创建字段名解析器
     *
     * @param cache 实体元数据缓存
     */
    public FieldNameResolver(EntityMetadataCache cache) {
        this.cache = cache;
    }

    /**
     * 解析 Lambda 方法引用对应的数据库列名
     *
     * @param entityClass 实体类
     * @param getter Lambda 方法引用
     * @return 数据库列名
     */
    public <T, R> String resolveColumnName(Class<T> entityClass, SFunction<T, R> getter) {
        // 1. 从 Lambda 提取属性名
        String propertyName = Conditions.getFieldName(getter);

        // 2. 获取实体元数据
        EntityMetadata<?> metadata = cache.get(entityClass);

        // 3. 如果支持列名感知，使用元数据转换
        if (metadata instanceof ColumnNameAware columnNameAware) {
            return columnNameAware.getColumnName(propertyName);
        }

        // 4. 降级：camelCase → snake_case
        return NamingUtils.toSnakeCase(propertyName);
    }
}