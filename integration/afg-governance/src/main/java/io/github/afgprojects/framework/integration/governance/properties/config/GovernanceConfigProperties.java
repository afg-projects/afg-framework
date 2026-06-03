package io.github.afgprojects.framework.integration.governance.properties.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 配置中心客户端属性
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client.config")
public class GovernanceConfigProperties {

    private boolean enabled = true;
    private String serviceName;
    private String environment = "dev";
    private boolean enableSubscribe = true;
    private boolean autoRegister = false;
    private List<String> autoRegisterPrefixes = new ArrayList<>();
    private boolean autoRegisterOverwrite = true;
    private Map<String, String> displayNames = new HashMap<>();
    private int reconnectIntervalMs = 5000;
    private int maxReconnectIntervalMs = 60000;
    private int maxReconnectAttempts = 0;
    private int retryCount = 3;
    private int retryIntervalMs = 1000;

    public List<String> getEffectiveAutoRegisterPrefixes() {
        if (!autoRegisterPrefixes.isEmpty()) {
            return autoRegisterPrefixes;
        }
        return List.of();
    }
}
