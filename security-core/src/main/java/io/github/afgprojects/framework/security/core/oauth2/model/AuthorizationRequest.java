package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * OAuth2 授权请求。
 *
 * @param responseType 响应类型（code / token）
 * @param clientId 客户端 ID
 * @param redirectUri 重定向 URI
 * @param scope 请求的权限范围
 * @param state 状态参数
 * @param codeChallenge PKCE code_challenge
 * @param codeChallengeMethod PKCE code_challenge_method（S256 / plain）
 * @since 1.0.0
 */
public record AuthorizationRequest(
        @NonNull String responseType,
        @NonNull String clientId,
        @NonNull String redirectUri,
        @Nullable Set<String> scope,
        @Nullable String state,
        @Nullable String codeChallenge,
        @Nullable String codeChallengeMethod) {

    /**
     * 是否使用 PKCE。
     *
     * @return 如果使用 PKCE 返回 true
     */
    public boolean isPkce() {
        return codeChallenge != null && !codeChallenge.isEmpty();
    }
}
