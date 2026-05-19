package io.github.afgprojects.framework.core.web.security.signature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 *         resource-server:
 *           secret: "resource-server-secret-key"
 *           allowed-paths:
 *             - "/internal/permissions/**"
 *           allowed-scopes:
 *             - "permission:read"
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

        /**
         * 允许访问的路径模式（Ant 风格）。
         * <p>
         * 如果为空，则允许访问所有路径。
         * <p>
         * 示例：
         * <ul>
         *   <li>/internal/permissions/** - 允许访问所有权限查询接口</li>
         *   <li>/internal/** - 允许访问所有内部接口</li>
         * </ul>
         */
        private Set<String> allowedPaths = new HashSet<>();

        /**
         * 允许的权限范围。
         * <p>
         * 用于更细粒度的权限控制，可在业务层校验。
         * <p>
         * 示例：
         * <ul>
         *   <li>permission:read - 允许读取权限</li>
         *   <li>user:read - 允许读取用户信息</li>
         * </ul>
         */
        private Set<String> allowedScopes = new HashSet<>();

        /**
         * 是否允许访问指定路径。
         *
         * @param path 请求路径
         * @param pathMatcher 路径匹配器
         * @return 如果允许访问返回 true
         */
        public boolean isPathAllowed(String path, org.springframework.util.AntPathMatcher pathMatcher) {
            if (allowedPaths.isEmpty()) {
                return true;
            }
            return allowedPaths.stream()
                    .anyMatch(pattern -> pathMatcher.match(pattern, path));
        }

        /**
         * 是否具有指定权限范围。
         *
         * @param scope 权限范围
         * @return 如果具有该范围返回 true
         */
        public boolean hasScope(String scope) {
            return allowedScopes.isEmpty() || allowedScopes.contains(scope);
        }
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
