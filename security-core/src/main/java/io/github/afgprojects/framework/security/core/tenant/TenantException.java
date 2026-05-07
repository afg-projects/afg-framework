package io.github.afgprojects.framework.security.core.tenant;

import java.io.Serial;

import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 租户异常类。
 *
 * <p>用于表示租户相关的错误，如租户不存在、租户已禁用、无法解析租户等。
 *
 * <p>继承 {@link BusinessException}，支持错误码和国际化消息。
 *
 * @since 1.0.0
 */
public class TenantException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用错误码构造租户异常。
     *
     * @param errorCode 错误码，永不为 null
     */
    public TenantException(@NonNull ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用错误码和自定义消息构造租户异常。
     *
     * @param errorCode 错误码，永不为 null
     * @param message 自定义消息，永不为 null
     */
    public TenantException(@NonNull ErrorCode errorCode, @NonNull String message) {
        super(errorCode, message);
    }

    /**
     * 使用错误码、消息和原因构造租户异常。
     *
     * @param errorCode 错误码，永不为 null
     * @param message 自定义消息，永不为 null
     * @param cause 原因，可为 null
     */
    public TenantException(@NonNull ErrorCode errorCode, @NonNull String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 使用错误码和参数构造租户异常。
     *
     * @param errorCode 错误码，永不为 null
     * @param args 消息参数，可为 null
     */
    public TenantException(@NonNull ErrorCode errorCode, Object[] args) {
        super(errorCode, args);
    }

    /**
     * 创建租户不存在异常。
     *
     * @param tenantId 租户 ID，永不为 null
     * @return 租户异常
     */
    @NonNull
    public static TenantException notFound(@NonNull String tenantId) {
        return new TenantException(TenantErrorCode.TENANT_NOT_FOUND, "租户不存在: " + tenantId);
    }

    /**
     * 创建租户已禁用异常。
     *
     * @param tenantId 租户 ID，永不为 null
     * @return 租户异常
     */
    @NonNull
    public static TenantException disabled(@NonNull String tenantId) {
        return new TenantException(TenantErrorCode.TENANT_DISABLED, "租户已禁用: " + tenantId);
    }

    /**
     * 创建无法解析租户异常。
     *
     * @return 租户异常
     */
    @NonNull
    public static TenantException unresolved() {
        return new TenantException(TenantErrorCode.TENANT_UNRESOLVED);
    }

    /**
     * 创建租户已过期异常。
     *
     * @param tenantId 租户 ID，永不为 null
     * @return 租户异常
     */
    @NonNull
    public static TenantException expired(@NonNull String tenantId) {
        return new TenantException(TenantErrorCode.TENANT_EXPIRED, "租户已过期: " + tenantId);
    }

    /**
     * 创建租户访问被拒绝异常。
     *
     * @param tenantId 租户 ID，永不为 null
     * @return 租户异常
     */
    @NonNull
    public static TenantException accessDenied(@NonNull String tenantId) {
        return new TenantException(TenantErrorCode.TENANT_ACCESS_DENIED, "无权访问租户: " + tenantId);
    }

    /**
     * 创建租户切换失败异常。
     *
     * @param tenantId 目标租户 ID，永不为 null
     * @return 租户异常
     */
    @NonNull
    public static TenantException switchFailed(@NonNull String tenantId) {
        return new TenantException(TenantErrorCode.TENANT_SWITCH_FAILED, "切换租户失败: " + tenantId);
    }
}