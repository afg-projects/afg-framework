package io.github.afgprojects.framework.governance.client.properties.registry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 服务注册发现客户端属性
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client.registry")
@SuppressWarnings("PMD.TooManyFields")
public class GovernanceRegistryProperties {

    private boolean enabled = true;
    private String serviceName;
    private String groupName = "default";
    private String version = "1.0.0";
    private String host;
    private Integer port;
    private String protocol = "http";
    private boolean autoRegister = true;
    private int heartbeatIntervalMs = 10000;
    private int heartbeatTimeoutMs = 30000;
    private Map<String, String> metadata = new HashMap<>();
    private int weight = 100;
    private int reconnectIntervalMs = 5000;
    private int maxReconnectIntervalMs = 60000;
    private int maxReconnectAttempts = 0;
    private int retryCount = 3;
    private int retryIntervalMs = 1000;
}
