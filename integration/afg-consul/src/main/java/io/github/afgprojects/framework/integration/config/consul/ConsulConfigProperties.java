package io.github.afgprojects.framework.integration.config.consul;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Consul 配置属性
 *
 * <p>配置 Consul KV Store 连接参数
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     consul:
 *       enabled: true
 *       host: localhost
 *       port: 8500
 *       prefix: config/afg
 *       token: ${CONSUL_TOKEN:}
 * </pre>
 *
 * @since 1.0.0
 */
@Data
public class ConsulConfigProperties {

    /**
     * 是否启用 Consul 配置客户端
     */
    private boolean enabled = true;

    /**
     * Consul 主机地址
     */
    private String host = "localhost";

    /**
     * Consul 端口
     */
    private int port = 8500;

    /**
     * 配置前缀
     * <p>用于区分不同应用的配置
     */
    private String prefix = "config/afg";

    /**
     * ACL token（可选）
     */
    private @Nullable String token;

    /**
     * 是否启用本地缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 刷新间隔（秒）
     */
    private int refreshInterval = 5;
}