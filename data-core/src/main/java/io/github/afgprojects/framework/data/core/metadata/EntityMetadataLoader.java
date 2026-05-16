package io.github.afgprojects.framework.data.core.metadata;

/**
 * 实体元数据加载器
 * <p>
 * 定义加载实体元数据的策略接口。
 * 支持多种加载方式：
 * <ul>
 *   <li>APT 生成：编译时生成，零反射开销</li>
 *   <li>运行时反射：动态解析，作为降级方案</li>
 * </ul>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 实现类
 * public class AptMetadataLoader implements EntityMetadataLoader {
 *     public <T> EntityMetadata<T> load(Class<T> entityClass) {
 *         // 加载 APT 生成的元数据
 *     }
 * }
 *
 * // 使用加载器链
 * List<EntityMetadataLoader> loaders = List.of(
 *     new AptMetadataLoader(),
 *     new ReflectiveMetadataLoader()
 * );
 *
 * for (EntityMetadataLoader loader : loaders) {
 *     if (loader.supports(User.class)) {
 *         EntityMetadata<User> metadata = loader.load(User.class);
 *     }
 * }
 * }
 * </pre>
 *
 * @see EntityMetadata
 * @see EntityMetadataCache
 */
public interface EntityMetadataLoader {

    /**
     * 加载实体元数据
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体元数据，如果无法加载则返回 null
     */
    <T> EntityMetadata<T> load(Class<T> entityClass);

    /**
     * 检查是否支持加载指定实体类
     *
     * @param entityClass 实体类
     * @return 是否支持加载
     */
    boolean supports(Class<?> entityClass);

    /**
     * 获取加载器优先级
     * <p>
     * 数值越小优先级越高。默认优先级为 100。
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 获取加载器名称
     *
     * @return 加载器名称
     */
    String getName();
}
