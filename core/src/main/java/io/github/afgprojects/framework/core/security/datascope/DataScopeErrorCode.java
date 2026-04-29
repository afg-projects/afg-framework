package io.github.afgprojects.framework.core.security.datascope;

import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 数据权限错误码
 * <p>
 * 错误码范围：18000-18999
 */
public enum DataScopeErrorCode implements ErrorCode {

    /**
     * 数据权限配置错误
     */
    CONFIG_ERROR(18000, "数据权限配置错误", ErrorCategory.SYSTEM),

    /**
     * 数据权限上下文不存在
     */
    CONTEXT_NOT_FOUND(18001, "数据权限上下文不存在", ErrorCategory.SYSTEM),

    /**
     * 数据权限解析失败
     */
    PARSE_ERROR(18002, "数据权限解析失败", ErrorCategory.SYSTEM),

    /**
     * SQL 注入失败
     */
    SQL_INJECT_ERROR(18003, "SQL权限条件注入失败", ErrorCategory.SYSTEM),

    /**
     * 部门层级查询失败
     */
    DEPT_HIERARCHY_ERROR(18004, "部门层级查询失败", ErrorCategory.SYSTEM),

    /**
     * 无权访问数据
     */
    ACCESS_DENIED(18005, "无权访问该数据", ErrorCategory.SECURITY),

    /**
     * 自定义条件解析失败
     */
    CUSTOM_CONDITION_ERROR(18006, "自定义条件解析失败", ErrorCategory.SYSTEM);

    private final int code;
    private final String message;
    private final ErrorCategory category;

    DataScopeErrorCode(int code, String message, ErrorCategory category) {
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