package io.github.afgprojects.framework.integration.governance.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Governance Client 配置属性
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   governance:
 *     client:
 *       enabled: true
 *       server-addr: "localhost:9090"
 *       service-name: "my-service"
 *       signature:
 *         enabled: true
 *         key-id: "governance-client"
 *         secret: "your-secret-key-at-least-32-characters"
 * </pre>
 *
 * @author afg-projects
 */
@Data
@ConfigurationProperties(prefix = "afg.governance.client")
public class GovernanceClientProperties {

    /**
     * 是否启用客户端
     */
    private boolean enabled = false;

    /**
     * 服务端地址
     */
    private String serverAddr = "localhost:9090";

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 是否启用配置订阅
     */
    private boolean enableConfigSubscribe = true;

    /**
     * 心跳间隔（毫秒）
     */
    private int heartbeatIntervalMs = 10000;

    /**
     * 重试次数
     */
    private int retryTimes = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryIntervalMs = 1000;

    /**
     * 是否启用签名认证
     */
    private boolean signatureEnabled = false;

    /**
     * 签名密钥标识
     */
    private String signatureKeyId = "governance-client";

    /**
     * 签名密钥（至少 32 字符）
     */
    private String signatureSecret;
}