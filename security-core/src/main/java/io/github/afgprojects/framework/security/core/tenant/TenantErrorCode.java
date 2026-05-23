package io.github.afgprojects.framework.security.core.tenant;

import io.github.afgprojects.framework.commons.exception.ErrorCategory;
import io.github.afgprojects.framework.commons.exception.ErrorCode;

/**
 * 租户错误码枚举。
 *
 * <p>错误码范围：20000-20999
 *
 * @since 1.0.0
 */
public enum TenantErrorCode implements ErrorCode {

    // ==================== 租户错误 (20000-20099) ====================

    /**
     * 租户不存在
     */
    TENANT_NOT_FOUND(20000, "租户不存在: {0}", ErrorCategory.BUSINESS),

    /**
     * 租户已禁用
     */
    TENANT_DISABLED(20001, "租户已禁用: {0}", ErrorCategory.BUSINESS),

    /**
     * 租户已过期
     */
    TENANT_EXPIRED(20002, "租户已过期: {0}", ErrorCategory.BUSINESS),

    /**
     * 租户已暂停
     */
    TENANT_SUSPENDED(20010, "租户已暂停: {0}", ErrorCategory.BUSINESS),

    /**
     * 无法解析租户
     */
    TENANT_UNRESOLVED(20003, "无法解析租户信息", ErrorCategory.BUSINESS),

    /**
     * 租户访问被拒绝
     */
    TENANT_ACCESS_DENIED(20004, "无权访问租户: {0}", ErrorCategory.SECURITY),

    /**
     * 租户切换失败
     */
    TENANT_SWITCH_FAILED(20005, "切换租户失败: {0}", ErrorCategory.BUSINESS),

    /**
     * 租户参数无效
     */
    TENANT_INVALID_PARAM(20006, "租户参数无效", ErrorCategory.BUSINESS),

    /**
     * 租户配置错误
     */
    TENANT_CONFIG_ERROR(20007, "租户配置错误", ErrorCategory.SYSTEM),

    /**
     * 租户数据源不可用
     */
    TENANT_DATASOURCE_UNAVAILABLE(20008, "租户数据源不可用", ErrorCategory.SYSTEM),

    /**
     * 租户初始化失败
     */
    TENANT_INIT_FAILED(20009, "租户初始化失败", ErrorCategory.SYSTEM);

    private final int code;
    private final String message;
    private final ErrorCategory category;

    TenantErrorCode(int code, String message, ErrorCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCategory getCategory() {
        return category;
    }
}
