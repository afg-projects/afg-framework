package io.github.afgprojects.framework.data.core.metadata;

/**
 * 元数据提供者接口
 * <p>
 * 用于创建 EntityMetadata 实例的 SPI 接口。
 * data-core 模块通过此接口解耦对 data-jdbc 模块的直接依赖。
 *
 * <p>实现类应通过 SPI 机制注册（META-INF/services）。
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 在 data-jdbc 模块中实现
 * public class JdbcMetadataProvider implements MetadataProvider {
 *     public <T> EntityMetadata<T> createMetadata(Class<T> entityClass) {
 *         return ReflectiveEntityMetadata.create(entityClass);
 *     }
 * }
 *
 * // SPI 注册文件
 * // META-INF/services/io.github.afgprojects.framework.data.core.metadata.MetadataProvider
 * io.github.afgprojects.framework.data.jdbc.metadata.JdbcMetadataProvider
 * }
 * </pre>
 *
 * @see EntityMetadata
 * @see ReflectiveMetadataLoader
 */
public interface MetadataProvider {

    /**
     * 创建实体元数据
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 实体元数据，如果无法创建则返回 null
     */
    <T> EntityMetadata<T> createMetadata(Class<T> entityClass);

    /**
     * 检查是否支持创建指定实体类的元数据
     *
     * @param entityClass 实体类
     * @return 是否支持创建
     */
    default boolean supports(Class<?> entityClass) {
        return true;
    }

    /**
     * 获取提供者优先级
     * <p>
     * 数值越小优先级越高。默认优先级为 100。
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 获取提供者名称
     *
     * @return 提供者名称
     */
    default String getName() {
        return "Default";
    }
}
