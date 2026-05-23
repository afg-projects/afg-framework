package io.github.afgprojects.framework.core.model.exception;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.exception.CommonErrorCode} 代替。
 * 此枚举仅作为类型别名，实际值应使用 commons 中的枚举。
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public enum CommonErrorCode implements ErrorCode {
    FAIL,
    PARAM_ERROR,
    PARAM_MISSING,
    PARAM_FORMAT_ERROR,
    NOT_FOUND,
    RESOURCE_EXISTS,
    RESOURCE_LOCKED,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    REQUEST_TIMEOUT,
    PAYLOAD_TOO_LARGE,
    TOO_MANY_REQUESTS,
    RATE_LIMIT_EXCEEDED,
    CIRCUIT_BREAKER_OPEN,
    UNAUTHORIZED,
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    FORBIDDEN,
    PERMISSION_DENIED,
    ACCOUNT_DISABLED,
    ACCOUNT_LOCKED,
    PASSWORD_EXPIRED,
    ENTITY_NOT_FOUND,
    ENTITY_ALREADY_EXISTS,
    FIELD_NOT_FOUND,
    TABLE_NOT_FOUND,
    DDL_ERROR,
    QUERY_ERROR,
    DATA_INTEGRITY_VIOLATION,
    OPTIMISTIC_LOCK_ERROR,
    FILE_NOT_FOUND,
    FILE_UPLOAD_ERROR,
    FILE_DOWNLOAD_ERROR,
    FILE_TYPE_NOT_ALLOWED,
    FILE_SIZE_EXCEEDED,
    STORAGE_FULL,
    JOB_NOT_FOUND,
    JOB_EXECUTION_ERROR,
    JOB_ALREADY_RUNNING,
    JOB_PAUSED,
    JOB_DISABLED,
    CLIENT_REQUEST_FAILED,
    CLIENT_TIMEOUT,
    CLIENT_CONNECT_FAILED,
    CLIENT_RETRY_EXHAUSTED,
    CLIENT_CIRCUIT_OPEN,
    MODULE_NOT_FOUND,
    MODULE_DUPLICATE,
    MODULE_CIRCULAR_DEPENDENCY,
    MODULE_INIT_FAILED,
    CONFIG_NOT_FOUND,
    CONFIG_BINDING_ERROR,
    FEATURE_DISABLED,
    FEATURE_FALLBACK_FAILED,
    SYSTEM_ERROR,
    INTERNAL_ERROR,
    SERVICE_UNAVAILABLE,
    DEPENDENCY_ERROR,
    CONFIG_ERROR;

    private static final io.github.afgprojects.framework.commons.exception.CommonErrorCode[] VALUES = 
        io.github.afgprojects.framework.commons.exception.CommonErrorCode.values();

    @Override
    public int getCode() {
        return VALUES[this.ordinal()].getCode();
    }

    @Override
    public String getMessage() {
        return VALUES[this.ordinal()].getMessage();
    }

    @Override
    public io.github.afgprojects.framework.commons.exception.ErrorCategory getCategory() {
        return VALUES[this.ordinal()].getCategory();
    }
}
