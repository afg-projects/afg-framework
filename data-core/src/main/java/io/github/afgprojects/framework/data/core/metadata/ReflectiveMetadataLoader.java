package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.data.core.exception.MetadataLoadException;

import java.util.ServiceLoader;

/**
 * 反射元数据加载器
 * <p>
 * 使用运行时反射加载实体元数据，作为 APT 的降级方案。
 * 当 APT 生成的元数据类不存在时，使用此加载器。
 *
 * <p>优先级：最低（1000），仅在其他加载器都无法加载时使用。
 *
 * <p>此加载器通过 {@link MetadataProvider} SPI 接口获取元数据实例，
 * 不直接依赖 data-jdbc 模块的类，遵循依赖方向原则。
 * MetadataProvider 的实现由 data-jdbc 等具体模块通过 SPI 注册。
 *
 * <pre>
 * 示例：
 * {@code
 * ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
 * if (loader.supports(User.class)) {
 *     EntityMetadata<User> metadata = loader.load(User.class);
 * }
 * }
 * </pre>
 *
 * @see MetadataProvider
 */
public class ReflectiveMetadataLoader implements EntityMetadataLoader {

    private volatile MetadataProvider provider;
    private volatile boolean providerResolved = false;

    @Override
    @SuppressWarnings("unchecked")
    public <T> EntityMetadata<T> load(Class<T> entityClass) {
        MetadataProvider resolvedProvider = resolveProvider();
        if (resolvedProvider == null) {
            return null;
        }

        try {
            return resolvedProvider.createMetadata(entityClass);
        } catch (Exception e) {
            throw new MetadataLoadException(
                    "Failed to create reflective metadata via MetadataProvider: " + e.getMessage(),
                    entityClass.getName(), e);
        }
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        MetadataProvider resolvedProvider = resolveProvider();
        return resolvedProvider != null && resolvedProvider.supports(entityClass);
    }

    @Override
    public int getPriority() {
        return 1000; // 最低优先级
    }

    @Override
    public String getName() {
        return "Reflective";
    }

    /**
     * 设置 MetadataProvider（用于编程式注入，优先于 SPI 发现）
     *
     * @param provider 元数据提供者
     */
    public void setMetadataProvider(MetadataProvider provider) {
        this.provider = provider;
        this.providerResolved = true;
    }

    /**
     * 解析 MetadataProvider 实例
     * <p>
     * 优先使用编程式注入的 provider，否则通过 SPI 发现。
     * 使用双重检查锁定保证线程安全。
     */
    private MetadataProvider resolveProvider() {
        if (!providerResolved) {
            synchronized (this) {
                if (!providerResolved) {
                    provider = discoverProvider();
                    providerResolved = true;
                }
            }
        }
        return provider;
    }

    /**
     * 通过 SPI 发现 MetadataProvider 实现
     *
     * @return 发现的 MetadataProvider，如果没有则返回 null
     */
    private MetadataProvider discoverProvider() {
        ServiceLoader<MetadataProvider> providers = ServiceLoader.load(MetadataProvider.class);
        MetadataProvider best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (MetadataProvider p : providers) {
            if (p.getPriority() < bestPriority) {
                bestPriority = p.getPriority();
                best = p;
            }
        }
        return best;
    }
}
