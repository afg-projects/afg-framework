package io.github.afgprojects.framework.security.core.token;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import org.jspecify.annotations.NonNull;

/**
 * AFG Token 提供者接口。
 *
 * <p>定义 Token 生成和验证的标准接口。
 *
 * @since 1.0.0
 */
public interface AfgTokenProvider {

    /**
     * 生成 Access Token。
     *
     * @param authentication 认证信息，永不为 null
     * @return Access Token，永不为 null
     */
    @NonNull
    AfgToken generateAccessToken(@NonNull AfgAuthentication authentication);

    /**
     * 生成 Refresh Token。
     *
     * @param authentication 认证信息，永不为 null
     * @return Refresh Token，永不为 null
     */
    @NonNull
    AfgToken generateRefreshToken(@NonNull AfgAuthentication authentication);

    /**
     * 验证 Token 并返回认证信息。
     *
     * @param tokenValue Token 值，永不为 null
     * @return 认证信息，如果验证失败则抛出异常
     * @throws io.github.afgprojects.framework.security.core.token.TokenValidationException 如果验证失败
     */
    @NonNull
    AfgAuthentication validateToken(@NonNull String tokenValue);

    /**
     * 撤销 Token。
     *
     * @param tokenValue Token 值，永不为 null
     */
    void invalidateToken(@NonNull String tokenValue);

    /**
     * 判断 Token 是否有效。
     *
     * @param tokenValue Token 值，永不为 null
     * @return 如果有效则返回 true
     */
    boolean isValidToken(@NonNull String tokenValue);
}