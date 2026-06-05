package io.github.afgprojects.framework.governance.client.properties.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Governance 客户端公共配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client")
public class GovernanceCommonProperties {

    private String serverAddr = "localhost:9090";
    private GovernanceSignatureProperties signature = new GovernanceSignatureProperties();
    private int retryTimes = 3;
    private int retryIntervalMs = 1000;

    public boolean isSignatureEnabled() {
        return signature.isEnabled();
    }

    public String getSignatureKeyId() {
        return signature.getKeyId();
    }

    public String getSignatureSecret() {
        return signature.getSecret();
    }
}
