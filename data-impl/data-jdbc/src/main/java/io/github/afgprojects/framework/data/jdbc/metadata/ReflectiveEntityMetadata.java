package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.DatabaseEntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.DatabaseFieldMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.jspecify.annotations.Nullable;
import io.github.afgprojects.framework.commons.naming.NamingUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    // ==================== EntityMetadata 新增方法 ====================

    @Override
    public String getIdFieldName() {
        DatabaseFieldMetadata idField = getIdField();
        return idField != null ? idField.getPropertyName() : "id";
    }

    @Override
    public @Nullable FieldMetadata getSoftDeleteField() {
        FieldMetadata deleted = getField("deleted");
        return deleted != null ? deleted : getField("deletedAt");
    }

    @Override
    public @Nullable FieldMetadata getTenantField() {
        return getField("tenantId");
    }

    @Override
    public Map<String, String> getColumnToFieldMap() {
        Map<String, String> map = new HashMap<>();
        for (DatabaseFieldMetadata field : fields) {
            map.put(field.getColumnName(), field.getPropertyName());
        }
        return map;
    }

    @Override
    public Map<String, String> getFieldToColumnMap() {
        Map<String, String> map = new HashMap<>();
        for (DatabaseFieldMetadata field : fields) {
            map.put(field.getPropertyName(), field.getColumnName());
        }
        return map;
    }

    @Override
    public boolean hasTrait(EntityTrait trait) {
        return getTraits().contains(trait);
    }

    @Override
    public Set<EntityTrait> getTraits() {
        Set<EntityTrait> traits = EnumSet.noneOf(EntityTrait.class);
        // 软删除特性检测
        if (getField("deleted") != null) {
            traits.add(EntityTrait.SOFT_DELETABLE);
        } else if (getField("deletedAt") != null) {
            traits.add(EntityTrait.TIMESTAMP_SOFT_DELETABLE);
        }
        // 多租户特性检测
        if (getField("tenantId") != null) {
            traits.add(EntityTrait.TENANT_AWARE);
        }
        // 审计特性检测
        if (getField("createdAt") != null && getField("updatedAt") != null) {
            traits.add(EntityTrait.AUDITABLE);
        }
        // 乐观锁特性检测
        if (getField("version") != null) {
            traits.add(EntityTrait.VERSIONED);
        }
        // 数据权限特性检测
        if (inferDataScopeAware()) {
            traits.add(EntityTrait.DATA_SCOPE_AWARE);
        }
        return traits;
    }

    @Override
    public @Nullable Condition getDefaultCondition() {
        return inferDefaultCondition();
    }

/**
     * 推断实体是否需要数据权限过滤
     * <p>
     * 通过反射检测实体类上的 @DataScope 注解（core 模块），
     * 或检测实体是否包含典型的数据权限字段（deptId、createBy 等）。
     * <p>
     * 这是 APT 不可用时的安全降级方案，确保数据权限保护在反射模式下仍然生效。
     *
     * @return 如果实体需要数据权限过滤则返回 true
     */
    @SuppressWarnings("unchecked")
    private boolean inferDataScopeAware() {
        // 1. 检查 core 模块的 @DataScope 注解（类级别）
        try {
            Class<? extends java.lang.annotation.Annotation> dataScopeAnnotationClass =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(
                            "io.github.afgprojects.framework.core.security.datascope.DataScope");
            if (entityClass.isAnnotationPresent(dataScopeAnnotationClass)) {
                return true;
            }
            // 检查 @DataScope 是 @Repeatable 的，可能有容器注解
            Class<? extends java.lang.annotation.Annotation> dataScopesAnnotationClass =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(
                            "io.github.afgprojects.framework.core.security.datascope.DataScopes");
            if (entityClass.isAnnotationPresent(dataScopesAnnotationClass)) {
                return true;
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // core 模块不在类路径中，跳过注解检测
        }

        // 2. 检查典型数据权限字段
        // 如果实体包含 deptId 字段，很可能需要数据权限过滤
        if (getField("deptId") != null) {
            return true;
        }

        return false;
    }

    /**
     * 推断实体的默认查询条件
     * <p>
     * 基于字段元数据构建软删除的默认条件。
     * 这是 APT 不可用时的安全降级方案，确保软删除过滤在反射模式下仍然生效。
     * <p>
     * <b>注意：</b>租户隔离条件不在此处构建，因为元数据是缓存的单例对象，
     * 而租户 ID 是请求级别的上下文。租户过滤由运行时 SQL 改写处理
     * （JdbcEntityProxy / EntityConditionalHandler 中已包含租户条件注入逻辑）。
     * <p>
     * <b>安全性说明：</b>此方法确保反射降级时不会静默绕过安全保护。
     * 之前硬编码返回 null 会导致软删除条件被完全绕过。
     *
     * @return 默认条件，如果没有需要自动过滤的字段则返回 null
     */
    private @Nullable Condition inferDefaultCondition() {
        List<Condition> conditions = new ArrayList<>(1);

        // 1. 软删除过滤
        boolean hasSoftDeleteTrait = hasTrait(EntityTrait.SOFT_DELETABLE) || hasTrait(EntityTrait.TIMESTAMP_SOFT_DELETABLE);
        if (hasSoftDeleteTrait) {
            FieldMetadata deletedAtField = getField("deletedAt");
            if (deletedAtField != null) {
                // 时间戳型软删除：deleted_at IS NULL
                conditions.add(Conditions.isNull(deletedAtField.getColumnName()));
            } else {
                FieldMetadata deletedField = getField("deleted");
                if (deletedField != null) {
                    // 布尔型软删除：deleted = false
                    conditions.add(Conditions.eq(deletedField.getColumnName(), false));
                }
            }
        }

        // 2. 租户隔离不在元数据层处理（运行时由 JdbcEntityProxy 等注入）
        // 因为 EntityMetadata 是单例缓存，而 tenantId 是请求级上下文

        if (conditions.isEmpty()) {
            return null;
        }
        if (conditions.size() == 1) {
            return conditions.getFirst();
        }
        return Conditions.allOf(conditions.toArray(new Condition[0]));
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
        return NamingUtils.toSnakeCase(clazz.getSimpleName());
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

}
