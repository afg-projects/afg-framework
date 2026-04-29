package io.github.afgprojects.framework.integration.config.apollo;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Apollo 配置属性
 *
 * <p>配置 Apollo 配置中心连接参数
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     apollo:
 *       enabled: true
 *       namespace: application
 * </pre>
 *
 * @since 1.0.0
 */
@Data
public class ApolloConfigProperties {

    /**
     * 是否启用 Apollo 配置客户端
     */
    private boolean enabled = true;

    /**
     * 默认命名空间
     * <p>Apollo 默认使用 application 命名空间
     */
    private String namespace = "application";

    /**
     * 是否启用本地缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 本地缓存目录
     */
    private @Nullable String cacheDir;

    /**
     * 集群名称
     */
    private @Nullable String cluster;
}
