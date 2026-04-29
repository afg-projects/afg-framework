package io.github.afgprojects.framework.core.trace;

/**
 * 异常日志级别枚举
 * <p>
 * 控制异常信息的记录详细程度
 * </p>
 */
public enum ExceptionLogLevel {

    /**
     * 不记录异常
     */
    NONE,

    /**
     * 只记录异常消息
     */
    MESSAGE,

    /**
     * 记录异常消息和堆栈信息
     */
    STACK_TRACE
}
