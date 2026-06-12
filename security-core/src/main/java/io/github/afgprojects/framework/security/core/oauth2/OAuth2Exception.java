package io.github.afgprojects.framework.security.core.oauth2;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 异常。
 *
 * <p>表示 OAuth2 协议层面的错误，包含标准错误码和描述。
 *
 * @since 1.0.0
 */
public class OAuth2Exception extends BusinessException {

    private final String oauth2ErrorCode;
    private final String errorDescription;

    /**
     * 构造函数。
     *
     * @param oauth2ErrorCode OAuth2 错误码
     * @param errorDescription 错误描述
     */
    public OAuth2Exception(@NonNull String oauth2ErrorCode, @Nullable String errorDescription) {
        super(CommonErrorCode.UNAUTHORIZED, oauth2ErrorCode + ": " + errorDescription);
        this.oauth2ErrorCode = oauth2ErrorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * 构造函数。
     *
     * @param oauth2ErrorCode OAuth2 错误码
     * @param errorDescription 错误描述
     * @param cause 原因
     */
    public OAuth2Exception(@NonNull String oauth2ErrorCode, @Nullable String errorDescription, @Nullable Throwable cause) {
        super(CommonErrorCode.UNAUTHORIZED, oauth2ErrorCode + ": " + errorDescription, cause);
        this.oauth2ErrorCode = oauth2ErrorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * 获取 OAuth2 错误码。
     *
     * @return OAuth2 错误码
     */
    @NonNull
    public String getOauth2ErrorCode() {
        return oauth2ErrorCode;
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
