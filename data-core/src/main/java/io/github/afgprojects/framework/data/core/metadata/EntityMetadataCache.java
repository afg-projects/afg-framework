package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.commons.naming.NamingUtils;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体元数据缓存
 * <p>
 * 使用加载器链模式，按优先级尝试加载实体元数据：
 * <ol>
 *   <li>APT 生成：编译时生成 {@code EntityNameMetadata} 类，零反射开销</li>
 *   <li>运行时反射：使用 {@code ReflectiveEntityMetadata} 动态解析</li>
 *   <li>空元数据：最终降级方案</li>
 * </ol>
 *
 * <p>支持通过 SPI 扩展自定义加载器。
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
 * @see EntityMetadataLoader
 * @see ColumnNameAware
 */
public class EntityMetadataCache {

    private final ConcurrentHashMap<Class<?>, EntityMetadata<?>> cache = new ConcurrentHashMap<>();
    private final List<EntityMetadataLoader> loaders;

    /**
     * 创建默认缓存实例
     * <p>
     * 自动发现并注册所有可用的加载器。
     */
    public EntityMetadataCache() {
        this.loaders = discoverLoaders();
    }

    /**
     * 创建自定义加载器列表的缓存实例
     *
     * @param loaders 加载器列表
     */
    public EntityMetadataCache(List<EntityMetadataLoader> loaders) {
        this.loaders = new ArrayList<>(loaders);
        this.loaders.sort(Comparator.comparingInt(EntityMetadataLoader::getPriority));
    }

    /**
     * 发现并注册所有可用的加载器
     */
    private List<EntityMetadataLoader> discoverLoaders() {
        List<EntityMetadataLoader> discovered = new ArrayList<>();

        // 通过 SPI 发现加载器
        ServiceLoader<EntityMetadataLoader> spiLoaders = ServiceLoader.load(EntityMetadataLoader.class);
        for (EntityMetadataLoader loader : spiLoaders) {
            discovered.add(loader);
        }

        // 如果 SPI 没有发现任何加载器，使用默认加载器
        if (discovered.isEmpty()) {
            discovered.add(new AptMetadataLoader());
            discovered.add(new ReflectiveMetadataLoader());
        }

        // 按优先级排序
        discovered.sort(Comparator.comparingInt(EntityMetadataLoader::getPriority));

        return discovered;
    }

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
     * 按优先级尝试加载器链，返回第一个成功加载的元数据。
     */
    private <T> EntityMetadata<T> resolveMetadata(Class<T> entityClass) {
        // 按优先级尝试每个加载器
        for (EntityMetadataLoader loader : loaders) {
            if (loader.supports(entityClass)) {
                EntityMetadata<T> metadata = loader.load(entityClass);
                if (metadata != null) {
                    return metadata;
                }
            }
        }

        // 最终降级：返回空元数据
        return new EmptyEntityMetadata<>(entityClass);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 获取已注册的加载器列表
     *
     * @return 加载器列表（不可变）
     */
    public List<EntityMetadataLoader> getLoaders() {
        return List.copyOf(loaders);
    }

    /**
     * 空元数据实现（降级用）
     * <p>
     * 当所有加载器都无法加载时，提供最基本的元数据信息
     */
    private record EmptyEntityMetadata<T>(Class<T> entityClass) implements EntityMetadata<T> {

        @Override
        public Class<T> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return NamingUtils.toSnakeCase(entityClass.getSimpleName());
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
        public List<RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return Optional.empty();
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
