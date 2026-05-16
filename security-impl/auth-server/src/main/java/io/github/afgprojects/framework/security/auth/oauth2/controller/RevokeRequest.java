package io.github.afgprojects.framework.security.auth.oauth2.controller;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 令牌撤销请求参数。
 *
 * @param token 要撤销的令牌
 * @param tokenTypeHint 令牌类型提示（access_token / refresh_token）
 * @since 1.0.0
 */
public record RevokeRequest(
        @NonNull String token,
        @Nullable String tokenTypeHint) {
}