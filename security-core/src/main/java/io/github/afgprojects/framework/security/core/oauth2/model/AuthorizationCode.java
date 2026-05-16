package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

/**
 * OAuth2 授权码。
 *
 * <p>表示一个已颁发的授权码，包含授权码的所有相关信息。
 *
 * @param code 授权码
 * @param clientId 客户端 ID
 * @param userId 用户 ID
 * @param redirectUri 重定向 URI
 * @param scopes 权限范围
 * @param codeChallenge PKCE code_challenge
 * @param codeChallengeMethod PKCE code_challenge_method
 * @param expiresAt 过期时间
 * @param createdAt 创建时间
 * @since 1.0.0
 */
public record AuthorizationCode(
        @NonNull String code,
        @NonNull String clientId,
        @NonNull String userId,
        @NonNull String redirectUri,
        @Nullable Set<String> scopes,
        @Nullable String codeChallenge,
        @Nullable String codeChallengeMethod,
        @NonNull Instant expiresAt,
        @NonNull Instant createdAt) {

    /**
     * 是否使用 PKCE。
     *
     * @return 如果使用 PKCE 返回 true
     */
    public boolean isPkce() {
        return codeChallenge != null && !codeChallenge.isEmpty();
    }

    /**
     * 检查授权码是否已过期。
     *
     * @return 如果已过期返回 true
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * 检查授权码是否有效。
     *
     * @return 如果有效返回 true
     */
    public boolean isValid() {
        return !isExpired();
    }
}