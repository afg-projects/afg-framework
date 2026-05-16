package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * OAuth2 令牌请求。
 *
 * @param grantType 授权类型（authorization_code / refresh_token / client_credentials）
 * @param code 授权码
 * @param refreshToken 刷新令牌
 * @param redirectUri 重定向 URI
 * @param clientId 客户端 ID
 * @param clientSecret 客户端密钥
 * @param codeVerifier PKCE code_verifier
 * @param scope 请求的权限范围
 * @since 1.0.0
 */
public record TokenRequest(
        @NonNull String grantType,
        @Nullable String code,
        @Nullable String refreshToken,
        @Nullable String redirectUri,
        @NonNull String clientId,
        @Nullable String clientSecret,
        @Nullable String codeVerifier,
        @Nullable Set<String> scope) {

    /**
     * 授权类型常量。
     */
    public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_REFRESH_TOKEN = "refresh_token";
    public static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

    /**
     * 是否是授权码模式。
     *
     * @return 如果是授权码模式返回 true
     */
    public boolean isAuthorizationCodeGrant() {
        return GRANT_AUTHORIZATION_CODE.equals(grantType);
    }

    /**
     * 是否是刷新令牌模式。
     *
     * @return 如果是刷新令牌模式返回 true
     */
    public boolean isRefreshTokenGrant() {
        return GRANT_REFRESH_TOKEN.equals(grantType);
    }

    /**
     * 是否是客户端凭证模式。
     *
     * @return 如果是客户端凭证模式返回 true
     */
    public boolean isClientCredentialsGrant() {
        return GRANT_CLIENT_CREDENTIALS.equals(grantType);
    }

    /**
     * 是否使用 PKCE。
     *
     * @return 如果使用 PKCE 返回 true
     */
    public boolean isPkce() {
        return codeVerifier != null && !codeVerifier.isEmpty();
    }
}
