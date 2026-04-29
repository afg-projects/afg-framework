package io.github.afgprojects.framework.core.security.datascope;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 数据权限异常
 */
public class DataScopeException extends BusinessException {

    public DataScopeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DataScopeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DataScopeException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause.getMessage(), cause);
    }

    public DataScopeException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 抛出数据权限配置错误异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static DataScopeException configError(String message) {
        return new DataScopeException(DataScopeErrorCode.CONFIG_ERROR, message);
    }

    /**
     * 抛出上下文不存在异常
     *
     * @return 异常实例
     */
    public static DataScopeException contextNotFound() {
        return new DataScopeException(DataScopeErrorCode.CONTEXT_NOT_FOUND);
    }

    /**
     * 抛出解析失败异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static DataScopeException parseError(String message) {
        return new DataScopeException(DataScopeErrorCode.PARSE_ERROR, message);
    }

    /**
     * 抛出 SQL 注入失败异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static DataScopeException sqlInjectError(String message) {
        return new DataScopeException(DataScopeErrorCode.SQL_INJECT_ERROR, message);
    }

    /**
     * 抛出访问拒绝异常
     *
     * @return 异常实例
     */
    public static DataScopeException accessDenied() {
        return new DataScopeException(DataScopeErrorCode.ACCESS_DENIED);
    }
}