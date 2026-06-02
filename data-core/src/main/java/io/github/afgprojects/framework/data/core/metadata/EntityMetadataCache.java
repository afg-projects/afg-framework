package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.commons.naming.NamingUtils;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
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
 * <p>异常会被缓存，避免对同一个实体类重复执行失败的加载操作。
 * 可通过 {@link #clear()} 清除缓存（包括异常缓存）以重试。
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

    private final ConcurrentHashMap<Class<?>, Object> cache = new ConcurrentHashMap<>();
    private final List<EntityMetadataLoader> loaders;

    /**
     * 异常缓存标记对象
     * <p>
     * 当元数据加载失败时，缓存此标记以避免重复重试。
     */
    private static final Object FAILED_METADATA = new Object();

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
        Object cached = cache.computeIfAbsent(entityClass, this::resolveMetadata);
        if (cached == FAILED_METADATA) {
            // 返回空元数据作为降级，但异常已被缓存，不会重复尝试加载
            return new EmptyEntityMetadata<>(entityClass);
        }
        return (EntityMetadata<T>) cached;
    }

    /**
     * 解析实体元数据
     * <p>
     * 按优先级尝试加载器链，返回第一个成功加载的元数据。
     * 如果所有加载器都失败，缓存 FAILED_METADATA 标记以避免重复重试。
     */
    private <T> Object resolveMetadata(Class<T> entityClass) {
        // 按优先级尝试每个加载器
        for (EntityMetadataLoader loader : loaders) {
            if (loader.supports(entityClass)) {
                try {
                    EntityMetadata<T> metadata = loader.load(entityClass);
                    if (metadata != null) {
                        return metadata;
                    }
                } catch (Exception e) {
                    // 加载器抛出异常，继续尝试下一个加载器
                    // 异常不在此处缓存，因为后续加载器可能成功
                }
            }
        }

        // 所有加载器都失败，缓存 FAILED_METADATA 标记
        return FAILED_METADATA;
    }

    /**
     * 清空缓存
     * <p>
     * 同时清除元数据缓存和异常缓存，允许重新尝试加载。
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
        public String getIdFieldName() {
            return "id";
        }

        @Override
        public FieldMetadata getSoftDeleteField() {
            return null;
        }

        @Override
        public FieldMetadata getTenantField() {
            return null;
        }

        @Override
        public Map<String, String> getColumnToFieldMap() {
            return Map.of();
        }

        @Override
        public Map<String, String> getFieldToColumnMap() {
            return Map.of();
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
        public boolean hasTrait(EntityTrait trait) {
            return false;
        }

        @Override
        public Set<EntityTrait> getTraits() {
            return Set.of();
        }

        @Override
        public boolean isDataScopeAware() {
            return false;
        }

        @Override
        public Condition getDefaultCondition() {
            return null;
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }
}
