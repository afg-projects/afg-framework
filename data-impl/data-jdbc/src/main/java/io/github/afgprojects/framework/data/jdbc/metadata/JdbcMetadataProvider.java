package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.MetadataProvider;

/**
 * JDBC 模块的 MetadataProvider 实现
 * <p>
 * 通过 {@link ReflectiveEntityMetadata} 创建基于反射的实体元数据。
 * 作为 data-core 模块 {@link MetadataProvider} SPI 的实现，
 * 解除 data-core 对 data-jdbc 模块的直接类依赖。
 *
 * <p>通过 SPI 机制自动注册（META-INF/services）。
 *
 * @see MetadataProvider
 * @see ReflectiveEntityMetadata
 */
public class JdbcMetadataProvider implements MetadataProvider {

    @Override
    public <T> EntityMetadata<T> createMetadata(Class<T> entityClass) {
        return ReflectiveEntityMetadata.create(entityClass);
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        return true;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getName() {
        return "JDBC-Reflective";
    }
}
