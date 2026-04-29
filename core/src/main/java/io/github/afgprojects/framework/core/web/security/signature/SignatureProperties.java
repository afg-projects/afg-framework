package io.github.afgprojects.framework.core.web.security.signature;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 签名验证配置属性
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   security:
 *     signature:
 *       enabled: true
 *       default-key-id: "default"
 *       keys:
 *         default:
 *           secret: "your-secret-key"
 *         app1:
 *           secret: "app1-secret-key"
 *       timestamp-tolerance: 300
 *       nonce-cache-size: 10000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.security.signature")
public class SignatureProperties {

    /**
     * 是否启用签名验证
     */
    private boolean enabled = true;

    /**
     * 默认密钥标识
     */
    private String defaultKeyId = "default";

    /**
     * 密钥映射
     * <p>
     * key: 密钥标识
     * value: 密钥配置
     */
    private Map<String, KeyConfig> keys = new HashMap<>();

    /**
     * 默认时间戳容忍度（秒）
     */
    private int timestampTolerance = 300;

    /**
     * nonce 缓存最大容量
     * <p>
     * 用于防止重放攻击，缓存已使用的 nonce
     */
    private int nonceCacheSize = 10000;

    /**
     * 是否默认需要 nonce
     */
    private boolean nonceRequired = true;

    /**
     * 默认签名算法
     */
    private SignatureAlgorithm defaultAlgorithm = SignatureAlgorithm.HMAC_SHA256;

    /**
     * 密钥配置
     */
    @Data
    public static class KeyConfig {
        /**
         * 密钥（用于 HMAC 签名）
         */
        private String secret;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 描述
         */
        private @Nullable String description;
    }

    /**
     * 获取指定密钥
     *
     * @param keyId 密钥标识
     * @return 密钥配置，如果不存在返回 null
     */
    public @Nullable KeyConfig getKeyConfig(String keyId) {
        return keys.get(keyId);
    }

    /**
     * 获取默认密钥
     *
     * @return 默认密钥配置，如果不存在返回 null
     */
    public @Nullable KeyConfig getDefaultKeyConfig() {
        return keys.get(defaultKeyId);
    }
}
