package io.github.afgprojects.framework.core.model.exception;

/**
 * 错误分类
 */
public enum ErrorCategory {

    /**
     * 业务错误 - 业务逻辑校验失败
     */
    BUSINESS("B"),

    /**
     * 系统错误 - 系统内部异常
     */
    SYSTEM("S"),

    /**
     * 网络错误 - 网络通信异常
     */
    NETWORK("N"),

    /**
     * 安全错误 - 认证授权异常
     */
    SECURITY("A");

    private final String prefix;

    ErrorCategory(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
