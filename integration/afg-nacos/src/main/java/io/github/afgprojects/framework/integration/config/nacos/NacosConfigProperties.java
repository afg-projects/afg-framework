package io.github.afgprojects.framework.integration.config.nacos;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Nacos 配置属性
 *
 * <p>配置 Nacos 配置中心连接参数
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     nacos:
 *       enabled: true
 *       server-addr: localhost:8848
 *       namespace: ""
 *       group: DEFAULT_GROUP
 *       username: nacos
 *       password: nacos
 * </pre>
 *
 * @since 1.0.0
 */
@Data
public class NacosConfigProperties {

    /**
     * 是否启用 Nacos 配置客户端
     */
    private boolean enabled = true;

    /**
     * Nacos 服务器地址
     * <p>格式：host1:port1,host2:port2,...
     */
    private @Nullable String serverAddr = "localhost:8848";

    /**
     * 命名空间 ID
     * <p>用于隔离不同环境的配置，如 dev、test、prod
     */
    private @Nullable String namespace;

    /**
     * 配置分组
     * <p>默认为 DEFAULT_GROUP
     */
    private String group = "DEFAULT_GROUP";

    /**
     * 认证用户名
     */
    private @Nullable String username;

    /**
     * 认证密码
     */
    private @Nullable String password;

    /**
     * 访问令牌（可选，用于服务间认证）
     */
    private @Nullable String accessToken;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 10000;

    /**
     * 配置长轮询超时时间（毫秒）
     */
    private int pollTimeout = 30000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryInterval = 1000;

    /**
     * 是否启用本地缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 本地缓存目录
     */
    private @Nullable String cacheDir;

    /**
     * Context path for Nacos server
     */
    private @Nullable String contextPath;

    /**
     * Endpoint for Nacos server
     */
    private @Nullable String endpoint;

    /**
     * 是否启用 HTTPS
     */
    private boolean secure = false;
}
