package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

/**
 * OAuth2 客户端详情。
 *
 * @param clientId 客户端 ID
 * @param clientSecret 客户端密钥（机密客户端）
 * @param clientName 客户端名称
 * @param redirectUris 允许的重定向 URI
 * @param scopes 允许的权限范围
 * @param grantTypes 允许的授权类型
 * @param requirePkce 是否需要 PKCE
 * @param accessTokenTtl 访问令牌有效期
 * @param refreshTokenTtl 刷新令牌有效期
 * @since 1.0.0
 */
public record ClientDetails(
        @NonNull String clientId,
        @Nullable String clientSecret,
        @NonNull String clientName,
        @NonNull Set<String> redirectUris,
        @NonNull Set<String> scopes,
        @NonNull Set<String> grantTypes,
        boolean requirePkce,
        @NonNull Duration accessTokenTtl,
        @NonNull Duration refreshTokenTtl) {

    /**
     * 客户端类型。
     */
    public enum ClientType {
        /**
         * 机密客户端（有密钥）
         */
        CONFIDENTIAL,
        /**
         * 公共客户端（无密钥，如 SPA、移动应用）
         */
        PUBLIC
    }

    /**
     * 获取客户端类型。
     *
     * @return 客户端类型
     */
    public ClientType getClientType() {
        return clientSecret == null || clientSecret.isEmpty()
                ? ClientType.PUBLIC
                : ClientType.CONFIDENTIAL;
    }

    /**
     * 检查重定向 URI 是否允许。
     *
     * @param redirectUri 重定向 URI
     * @return 如果允许返回 true
     */
    public boolean isRedirectUriAllowed(@NonNull String redirectUri) {
        return redirectUris.contains(redirectUri);
    }

    /**
     * 检查授权类型是否允许。
     *
     * @param grantType 授权类型
     * @return 如果允许返回 true
     */
    public boolean isGrantTypeAllowed(@NonNull String grantType) {
        return grantTypes.contains(grantType);
    }

    /**
     * 检查权限范围是否允许。
     *
     * @param requestedScopes 请求的权限范围
     * @return 如果全部允许返回 true
     */
    public boolean isScopeAllowed(@NonNull Set<String> requestedScopes) {
        return scopes.containsAll(requestedScopes);
    }
}
