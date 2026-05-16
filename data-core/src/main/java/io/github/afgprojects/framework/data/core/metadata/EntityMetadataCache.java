package io.github.afgprojects.framework.data.core.metadata;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体元数据缓存
 * <p>
 * 优先使用 APT 生成的元数据，降级到运行时反射。
 * 支持两种元数据加载策略：
 *
 * <ul>
 *   <li>APT 生成：编译时生成 {@code EntityNameMetadata} 类，零反射开销</li>
 *   <li>运行时反射：使用 {@code ReflectiveEntityMetadata} 动态解析</li>
 * </ul>
 *
 * <pre>
 * 使用示例：
 * {@code
 * EntityMetadataCache cache = new EntityMetadataCache();
 *
 * // 获取实体元数据
 * EntityMetadata<User> metadata = cache.get(User.class);
 *
 * // 获取列名（如果元数据实现了 ColumnNameAware）
 * if (metadata instanceof ColumnNameAware aware) {
 *     String columnName = aware.getColumnName("userName"); // "user_name"
 * }
 * }
 * </pre>
 *
 * @see EntityMetadata
 * @see ColumnNameAware
 */
public class EntityMetadataCache {

    private final ConcurrentHashMap<Class<?>, EntityMetadata<?>> cache = new ConcurrentHashMap<>();

    /**
     * 获取实体元数据
     *
     * @param entityClass 实体类
     * @return 实体元数据
     */
    @SuppressWarnings("unchecked")
    public <T> EntityMetadata<T> get(Class<T> entityClass) {
        return (EntityMetadata<T>) cache.computeIfAbsent(entityClass, this::resolveMetadata);
    }

    /**
     * 解析实体元数据
     * <p>
     * 1. 尝试加载 APT 生成的元数据类
     * 2. 降级到运行时反射解析
     */
    private <T> EntityMetadata<T> resolveMetadata(Class<T> entityClass) {
        // 1. 尝试加载 APT 生成的元数据类
        EntityMetadata<T> generated = loadGeneratedMetadata(entityClass);
        if (generated != null) {
            return generated;
        }

        // 2. 降级：尝试使用 ReflectiveEntityMetadata（如果可用）
        EntityMetadata<T> reflective = loadReflectiveMetadata(entityClass);
        if (reflective != null) {
            return reflective;
        }

        // 3. 最终降级：返回空元数据
        return new EmptyEntityMetadata<>(entityClass);
    }

    /**
     * 加载 APT 生成的元数据类
     * <p>
     * APT 生成的类命名规则：{@code 实体包名.metadata.实体名Metadata}
     * 例如：{@code com.example.entity.User} → {@code com.example.entity.metadata.UserMetadata}
     */
    @SuppressWarnings("unchecked")
    private <T> EntityMetadata<T> loadGeneratedMetadata(Class<T> entityClass) {
        try {
            String packageName = entityClass.getPackageName();
            String simpleName = entityClass.getSimpleName();
            String metadataClassName = packageName + ".metadata." + simpleName + "Metadata";
            Class<?> metadataClass = Class.forName(metadataClassName);
            return (EntityMetadata<T>) metadataClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // APT 生成的元数据类不存在，降级到反射
            return null;
        }
    }

    /**
     * 尝试加载 ReflectiveEntityMetadata
     * <p>
     * 如果 data-jdbc 模块在类路径中，使用反射实现
     */
    @SuppressWarnings("unchecked")
    private <T> EntityMetadata<T> loadReflectiveMetadata(Class<T> entityClass) {
        try {
            Class<?> reflectiveClass = Class.forName(
                "io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveEntityMetadata"
            );
            java.lang.reflect.Method createMethod = reflectiveClass.getMethod("create", Class.class);
            return (EntityMetadata<T>) createMethod.invoke(null, entityClass);
        } catch (Exception e) {
            // ReflectiveEntityMetadata 不可用
            return null;
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 空元数据实现（降级用）
     * <p>
     * 当 APT 和反射都不可用时，提供最基本的元数据信息
     */
    private static class EmptyEntityMetadata<T> implements EntityMetadata<T> {

        private final Class<T> entityClass;

        EmptyEntityMetadata(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public Class<T> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return toSnakeCase(entityClass.getSimpleName());
        }

        @Override
        public FieldMetadata getIdField() {
            return null;
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of();
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return null;
        }

        @Override
        public boolean isSoftDeletable() {
            return false;
        }

        @Override
        public boolean isTenantAware() {
            return false;
        }

        @Override
        public boolean isAuditable() {
            return false;
        }

        @Override
        public boolean isVersioned() {
            return false;
        }

        @Override
        public List<io.github.afgprojects.framework.data.core.relation.RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public java.util.Optional<io.github.afgprojects.framework.data.core.relation.RelationMetadata> getRelation(String fieldName) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }

        /**
         * camelCase 转 snake_case
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
}
