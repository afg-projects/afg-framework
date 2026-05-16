package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.DatabaseEntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.DatabaseFieldMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于反射的数据库实体元数据实现
 * <p>
 * 通过反射从实体类提取元数据，支持：
 * <ul>
 *   <li>从 @Table 注解读取表名</li>
 *   <li>从 @Column 注解读取列名</li>
 *   <li>从 @Id 注解识别主键</li>
 *   <li>自动 camelCase 转 snake_case</li>
 * </ul>
 *
 * <p>
 * 此类是运行时反射实现，作为 APT 编译时生成的降级方案。
 * 当 APT 生成的元数据类不可用时，使用此类提供元数据支持。
 *
 * <pre>
 * 示例：
 * {@code
 * // 创建反射元数据
 * DatabaseEntityMetadata<User> metadata = ReflectiveEntityMetadata.create(User.class);
 *
 * // 获取表名
 * String tableName = metadata.getTableName(); // "sys_user"
 *
 * // 获取列名
 * String columnName = metadata.getColumnName("userName"); // "user_name"
 *
 * // 获取主键字段
 * DatabaseFieldMetadata idField = metadata.getIdField();
 * }
 * </pre>
 *
 * @param <T> 实体类型
 */
public class ReflectiveEntityMetadata<T> implements DatabaseEntityMetadata<T> {

    private static final ConcurrentHashMap<Class<?>, ReflectiveEntityMetadata<?>> CACHE = new ConcurrentHashMap<>();

    private final Class<T> entityClass;
    private final String tableName;
    private final List<DatabaseFieldMetadata> fields;
    private final List<RelationMetadata> relations;

    private ReflectiveEntityMetadata(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = inferTableName(entityClass);
        this.fields = extractFields(entityClass);
        this.relations = extractRelations(entityClass);
    }

    /**
     * 创建或获取反射元数据实例（带缓存）
     * <p>
     * 使用缓存避免重复反射开销。
     *
     * @param entityClass 实体类
     * @return 反射元数据实例
     */
    @SuppressWarnings("unchecked")
    public static <T> ReflectiveEntityMetadata<T> create(Class<T> entityClass) {
        return (ReflectiveEntityMetadata<T>) CACHE.computeIfAbsent(entityClass, ReflectiveEntityMetadata::new);
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        CACHE.clear();
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FieldMetadata> getFields() {
        // DatabaseFieldMetadata extends FieldMetadata, so this cast is safe for read-only access
        return (List<FieldMetadata>) (List<?>) fields;
    }

    @Override
    public @Nullable DatabaseFieldMetadata getField(String propertyName) {
        for (DatabaseFieldMetadata field : fields) {
            if (field.getPropertyName().equals(propertyName)) {
                return field;
            }
        }
        return null;
    }

    @Override
    public @Nullable DatabaseFieldMetadata getIdField() {
        for (DatabaseFieldMetadata field : fields) {
            if (field.isId()) {
                return field;
            }
        }
        // 后备：查找名为 id 的字段
        for (DatabaseFieldMetadata field : fields) {
            if ("id".equals(field.getPropertyName())) {
                return field;
            }
        }
        return null;
    }

    @Override
    public boolean isSoftDeletable() {
        return getField("deleted") != null || getField("deletedAt") != null;
    }

    @Override
    public boolean isTenantAware() {
        return getField("tenantId") != null;
    }

    @Override
    public boolean isAuditable() {
        return getField("createdAt") != null && getField("updatedAt") != null;
    }

    @Override
    public boolean isVersioned() {
        return getField("version") != null;
    }

    // ==================== 关联元数据 ====================

    @Override
    public List<RelationMetadata> getRelations() {
        return relations;
    }

    @Override
    public Optional<RelationMetadata> getRelation(String fieldName) {
        for (RelationMetadata relation : relations) {
            if (relation.getFieldName().equals(fieldName)) {
                return Optional.of(relation);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean hasRelation(String fieldName) {
        return getRelation(fieldName).isPresent();
    }

    /**
     * 推断表名
     * <p>
     * 支持以下表名识别方式（按优先级）：
     * <ol>
     *   <li>jakarta.persistence.Table 注解的 name 属性</li>
     *   <li>类名转换为 snake_case（默认行为）</li>
     * </ol>
     *
     * @param clazz 实体类
     * @return 表名
     */
    private static String inferTableName(Class<?> clazz) {
        // 1. 检查 @Table 注解
        try {
            jakarta.persistence.Table tableAnnotation = clazz.getAnnotation(jakarta.persistence.Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                return tableAnnotation.name();
            }
        } catch (NoClassDefFoundError e) {
            // JPA API 不在类路径中
        }

        // 2. 默认：类名转 snake_case
        return toSnakeCase(clazz.getSimpleName());
    }

    /**
     * 提取字段元数据
     * <p>
     * 遍历类层次结构，收集所有非静态字段，跳过关联字段。
     *
     * @param clazz 实体类
     * @return 字段元数据列表
     */
    private static List<DatabaseFieldMetadata> extractFields(Class<?> clazz) {
        List<DatabaseFieldMetadata> result = new ArrayList<>();

        // 遍历类层次结构
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // 跳过静态字段
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                // 跳过关联字段（@ManyToOne, @OneToMany, @OneToOne, @ManyToMany）
                if (ReflectiveFieldMetadata.isAssociationField(field)) {
                    continue;
                }
                result.add(new ReflectiveFieldMetadata(field));
            }
            currentClass = currentClass.getSuperclass();
        }

        return result;
    }

    /**
     * 提取关联元数据
     * <p>
     * 遍历类层次结构，收集所有关联字段。
     *
     * @param clazz 实体类
     * @return 关联元数据列表
     */
    private static List<RelationMetadata> extractRelations(Class<?> clazz) {
        List<RelationMetadata> result = new ArrayList<>();

        // 遍历类层次结构
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // 跳过静态字段
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                // 只处理关联字段
                if (ReflectiveFieldMetadata.isAssociationField(field)) {
                    result.add(new ReflectiveRelationMetadata(clazz, field));
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return result;
    }

    /**
     * camelCase 转 snake_case
     *
     * @param name 属性名
     * @return snake_case 格式的列名
     */
    private static String toSnakeCase(String name) {
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
