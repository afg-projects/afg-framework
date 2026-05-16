package io.github.afgprojects.framework.security.auth.oauth2.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/**
 * OAuth2 配置属性。
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.auth.oauth2")
public class OAuth2Properties {

    /**
     * 是否启用 OAuth2 授权服务器
     */
    private boolean enabled = true;

    /**
     * 授权码有效期（默认 5 分钟）
     */
    private Duration authorizationCodeTtl = Duration.ofMinutes(5);

    /**
     * 访问令牌有效期（默认 1 小时）
     */
    private Duration accessTokenTtl = Duration.ofHours(1);

    /**
     * 刷新令牌有效期（默认 7 天）
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 令牌签发者
     */
    private String issuer = "afg-auth-server";

    /**
     * 预配置的客户端列表
     */
    private Set<ClientConfig> clients = Set.of();

    /**
     * 客户端配置。
     */
    public record ClientConfig(
            @NonNull String clientId,
            String clientSecret,
            @NonNull String clientName,
            @NonNull Set<String> redirectUris,
            @NonNull Set<String> scopes,
            @NonNull Set<String> grantTypes,
            boolean requirePkce
    ) {}

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getAuthorizationCodeTtl() {
        return authorizationCodeTtl;
    }

    public void setAuthorizationCodeTtl(Duration authorizationCodeTtl) {
        this.authorizationCodeTtl = authorizationCodeTtl;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Set<ClientConfig> getClients() {
        return clients;
    }

    public void setClients(Set<ClientConfig> clients) {
        this.clients = clients;
    }
}