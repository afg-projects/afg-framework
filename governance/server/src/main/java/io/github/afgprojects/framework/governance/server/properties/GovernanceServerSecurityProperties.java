package io.github.afgprojects.framework.governance.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Governance 模块安全配置属性
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   governance:
 *     server:
 *       security:
 *         enabled: true
 *         signature:
 *           enabled: true
 *           key-id: "governance-server"
 *           secret: "your-secret-key-at-least-32-characters"
 *           timestamp-tolerance: 300
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.server.security")
public class GovernanceServerSecurityProperties {

    /**
     * 是否启用安全认证
     */
    private boolean enabled = true;

    /**
     * 签名认证配置
     */
    private SignatureConfig signature = new SignatureConfig();

    @Data
    public static class SignatureConfig {
        /**
         * 是否启用签名认证
         */
        private boolean enabled = true;

        /**
         * 密钥标识
         */
        private String keyId = "governance-server";

        /**
         * 签名密钥（至少 32 字符）
         */
        private String secret = "governance-default-secret-key-32ch";

        /**
         * 时间戳容忍度（秒）
         */
        private int timestampTolerance = 300;

        /**
         * 签名算法
         */
        private String algorithm = "HMAC_SHA256";
    }
}
