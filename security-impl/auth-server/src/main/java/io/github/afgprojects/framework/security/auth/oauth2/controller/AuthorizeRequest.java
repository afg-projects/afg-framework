package io.github.afgprojects.framework.security.auth.oauth2.controller;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 授权端点请求参数。
 *
 * @param responseType 响应类型（code）
 * @param clientId 客户端 ID
 * @param redirectUri 重定向 URI
 * @param scope 权限范围
 * @param state 状态参数
 * @param codeChallenge PKCE code_challenge
 * @param codeChallengeMethod PKCE 方法（S256 / plain）
 * @since 1.0.0
 */
public record AuthorizeRequest(
        @NonNull String responseType,
        @NonNull String clientId,
        @NonNull String redirectUri,
        @Nullable String scope,
        @Nullable String state,
        @Nullable String codeChallenge,
        @Nullable String codeChallengeMethod) {

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