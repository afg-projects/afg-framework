package io.github.afgprojects.framework.security.core.oauth2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 异常。
 *
 * <p>表示 OAuth2 协议层面的错误，包含标准错误码和描述。
 *
 * @since 1.0.0
 */
public class OAuth2Exception extends RuntimeException {

    private final String errorCode;
    private final String errorDescription;

    /**
     * 构造函数。
     *
     * @param errorCode 错误码
     * @param errorDescription 错误描述
     */
    public OAuth2Exception(@NonNull String errorCode, @Nullable String errorDescription) {
        super(errorCode + ": " + errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * 构造函数。
     *
     * @param errorCode 错误码
     * @param errorDescription 错误描述
     * @param cause 原因
     */
    public OAuth2Exception(@NonNull String errorCode, @Nullable String errorDescription, @Nullable Throwable cause) {
        super(errorCode + ": " + errorDescription, cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    @NonNull
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误描述。
     *
     * @return 错误描述
     */
    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    // ==================== 预定义错误 ====================

    /**
     * 创建 invalid_client 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception invalidClient(@Nullable String description) {
        return new OAuth2Exception("invalid_client", description);
    }

    /**
     * 创建 invalid_grant 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception invalidGrant(@Nullable String description) {
        return new OAuth2Exception("invalid_grant", description);
    }

    /**
     * 创建 invalid_request 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception invalidRequest(@Nullable String description) {
        return new OAuth2Exception("invalid_request", description);
    }

    /**
     * 创建 unsupported_grant_type 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception unsupportedGrantType(@Nullable String description) {
        return new OAuth2Exception("unsupported_grant_type", description);
    }

    /**
     * 创建 unsupported_response_type 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception unsupportedResponseType(@Nullable String description) {
        return new OAuth2Exception("unsupported_response_type", description);
    }

    /**
     * 创建 unauthorized_client 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception unauthorizedClient(@Nullable String description) {
        return new OAuth2Exception("unauthorized_client", description);
    }

    /**
     * 创建 access_denied 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception accessDenied(@Nullable String description) {
        return new OAuth2Exception("access_denied", description);
    }

    /**
     * 创建 server_error 错误。
     *
     * @param description 错误描述
     * @return OAuth2Exception
     */
    public static OAuth2Exception serverError(@Nullable String description) {
        return new OAuth2Exception("server_error", description);
    }
}