package io.github.afgprojects.framework.security.resource.introspection;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Token 远程验证（Introspection）配置属性。
 *
 * <p>配置通过 OAuth2 Introspection 端点验证 Token 的参数。
 *
 * <p>配置示例：
 * <pre>
 * afg.security.resource.introspection.enabled=true
 * afg.security.resource.introspection.introspection-uri=https://auth.example.com/oauth2/introspect
 * afg.security.resource.introspection.client-id=resource-server
 * afg.security.resource.introspection.client-secret=${INTROSPECTION_CLIENT_SECRET}
 * </pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.security.resource.introspection")
public class IntrospectionProperties {

    /**
     * 是否启用远程验证。
     * 默认禁用，通常使用 JWT 本地验证。
     */
    private boolean enabled = false;

    /**
     * Introspection 端点 URI。
     * 例如：https://auth.example.com/oauth2/introspect
     */
    private String introspectionUri;

    /**
     * 客户端 ID。
     * 用于调用 Introspection 端点的身份验证。
     */
    private String clientId;

    /**
     * 客户端密钥。
     * 用于调用 Introspection 端点的身份验证。
     */
    private String clientSecret;

    /**
     * Introspection 结果缓存 TTL。
     * 默认 1 分钟，避免频繁调用远程端点。
     */
    private Duration cacheTtl = Duration.ofMinutes(1);

    /**
     * 连接超时时间。
     * 默认 5 秒。
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 读取超时时间。
     * 默认 10 秒。
     */
    private Duration readTimeout = Duration.ofSeconds(10);

    /**
     * 是否验证 Token 是否已激活。
     * 默认 true。
     */
    private boolean verifyActive = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    public String getIntrospectionUri() {
        return introspectionUri;
    }

    public void setIntrospectionUri(@NonNull String introspectionUri) {
        this.introspectionUri = introspectionUri;
    }

    @NonNull
    public String getClientId() {
        return clientId;
    }

    public void setClientId(@NonNull String clientId) {
        this.clientId = clientId;
    }

    @NonNull
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(@NonNull String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @NonNull
    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(@NonNull Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    @NonNull
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(@NonNull Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @NonNull
    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(@NonNull Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isVerifyActive() {
        return verifyActive;
    }

    public void setVerifyActive(boolean verifyActive) {
        this.verifyActive = verifyActive;
    }
}