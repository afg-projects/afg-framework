package io.github.afgprojects.framework.integration.governance.client.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Governance 客户端公共配置属性
 *
 * @author afg-projects
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client")
public class GovernanceCommonProperties {

    /**
     * 服务端地址
     */
    private String serverAddr = "localhost:9090";

    /**
     * 签名认证配置
     */
    private SignatureConfig signature = new SignatureConfig();

    /**
     * 重试次数
     */
    private int retryTimes = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryIntervalMs = 1000;

    @Data
    public static class SignatureConfig {
        /**
         * 是否启用签名认证
         */
        private boolean enabled = false;

        /**
         * 签名密钥标识
         */
        private String keyId = "governance-client";

        /**
         * 筠名密钥（至少 32 字符）
         */
        private String secret;
    }

    public boolean isSignatureEnabled() {
        return signature.enabled;
    }

    public String getSignatureKeyId() {
        return signature.keyId;
    }

    public String getSignatureSecret() {
        return signature.secret;
    }
}