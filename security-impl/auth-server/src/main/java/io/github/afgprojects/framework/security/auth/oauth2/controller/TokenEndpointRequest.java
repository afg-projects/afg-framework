package io.github.afgprojects.framework.security.auth.oauth2.controller;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 令牌端点请求参数。
 *
 * @param grantType 授权类型
 * @param code 授权码
 * @param refreshToken 刷新令牌
 * @param redirectUri 重定向 URI
 * @param clientId 客户端 ID
 * @param clientSecret 客户端密钥
 * @param codeVerifier PKCE code_verifier
 * @param scope 权限范围
 * @since 1.0.0
 */
public record TokenEndpointRequest(
        @NonNull String grantType,
        @Nullable String code,
        @Nullable String refreshToken,
        @Nullable String redirectUri,
        @NonNull String clientId,
        @Nullable String clientSecret,
        @Nullable String codeVerifier,
        @Nullable String scope) {

    /**
     * 解析 scope 字符串为 Set。
     *
     * @return scope 集合
     */
    public Set<String> parseScope() {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Set.of(scope.split("\\s+"));
    }
}