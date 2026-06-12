package io.github.afgprojects.framework.security.core.token;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

/**
 * Token 验证异常类。
 *
 * <p>用于表示 Token 验证失败。
 *
 * @since 1.0.0
 */
public class TokenValidationException extends BusinessException {

    /**
     * 构造 Token 验证异常。
     *
     * @param message 错误消息
     */
    public TokenValidationException(String message) {
        super(CommonErrorCode.TOKEN_INVALID, message);
    }

    /**
     * 构造 Token 验证异常。
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public TokenValidationException(String message, Throwable cause) {
        super(CommonErrorCode.TOKEN_INVALID, message, cause);
    }

    /**
     * 创建 Token 已过期异常。
     *
     * @return Token 验证异常
     */
    public static TokenValidationException expired() {
        return new TokenValidationException("Token has expired");
    }

    /**
     * 创建 Token 无效异常。
     *
     * @return Token 验证异常
     */
    public static TokenValidationException invalid() {
        return new TokenValidationException("Token is invalid");
    }

    /**
     * 创建 Token 签名无效异常。
     *
     * @return Token 验证异常
     */
    public static TokenValidationException invalidSignature() {
        return new TokenValidationException("Token signature is invalid");
    }

    /**
     * 创建 Token 已撤销异常。
     *
     * @return Token 验证异常
     */
    public static TokenValidationException revoked() {
        return new TokenValidationException("Token has been revoked");
    }
}
