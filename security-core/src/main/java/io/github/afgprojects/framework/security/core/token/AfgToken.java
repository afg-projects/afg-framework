package io.github.afgprojects.framework.security.core.token;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Map;

/**
 * AFG Token 接口。
 *
 * <p>表示一个 OAuth2 Token，包括 Access Token 和 Refresh Token。
 *
 * @since 1.0.0
 */
public interface AfgToken {

    /**
     * 获取 Token 值。
     *
     * @return Token 字符串，永不为 null
     */
    @NonNull
    String getTokenValue();

    /**
     * 获取 Token 签发时间。
     *
     * @return 签发时间
     */
    Instant getIssuedAt();

    /**
     * 获取 Token 过期时间。
     *
     * @return 过期时间
     */
    Instant getExpiresAt();

    /**
     * 获取 Token 类型。
     *
     * @return Token 类型，如 "access"、"refresh"
     */
    @NonNull
    default String getTokenType() {
        return "access";
    }

    /**
     * 判断 Token 是否已过期。
     *
     * @return 如果已过期则返回 true
     */
    default boolean isExpired() {
        return getExpiresAt() != null && getExpiresAt().isBefore(Instant.now());
    }

    /**
     * 获取 Token 声明（Claims）。
     *
     * @return 声明 Map
     */
    default Map<String, Object> getClaims() {
        return Map.of();
    }

    /**
     * 获取指定声明。
     *
     * @param name 声明名称
     * @return 声明值
     */
    default Object getClaim(String name) {
        return getClaims().get(name);
    }
}