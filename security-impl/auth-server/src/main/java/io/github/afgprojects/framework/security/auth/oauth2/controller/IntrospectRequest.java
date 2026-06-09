package io.github.afgprojects.framework.security.auth.oauth2.controller;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 令牌内省请求参数（RFC 7662）。
 *
 * @param token 要内省的令牌
 * @param tokenTypeHint 令牌类型提示（access_token / refresh_token）
 * @since 1.0.0
 */
public record IntrospectRequest(
        @NonNull String token,
        @Nullable String tokenTypeHint) {
}
