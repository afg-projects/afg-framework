package io.github.afgprojects.framework.integration.governance.client.registry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务注册发现客户端属性
 *
 * @author afg-projects
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client.registry")
public class GovernanceRegistryProperties {

    /**
     * 是否启用服务注册
     */
    private boolean enabled = true;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务分组
     */
    private String groupName = "default";

    /**
     * 服务版本
     */
    private String version = "1.0.0";

    /**
     * 服务地址（不设置则自动获取）
     */
    private String host;

    /**
     * 服务端口（不设置则自动获取）
     */
    private Integer port;

    /**
     * 协议：http/grpc
     */
    private String protocol = "http";

    /**
     * 是否自动注册
     */
    private boolean autoRegister = true;

    /**
     * 心跳间隔（毫秒）
     */
    private int heartbeatIntervalMs = 10000;

    /**
     * 心跳超时（毫秒）
     */
    private int heartbeatTimeoutMs = 30000;

    /**
     * 实例元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 实例权重
     */
    private int weight = 100;

    // ==================== 重连配置 ====================

    /**
     * 重连间隔（毫秒）
     */
    private int reconnectIntervalMs = 5000;

    /**
     * 最大重连间隔（毫秒）
     */
    private int maxReconnectIntervalMs = 60000;

    /**
     * 最大重连次数（0表示无限）
     */
    private int maxReconnectAttempts = 0;

    /**
     * 请求重试次数
     */
    private int retryCount = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryIntervalMs = 1000;
}