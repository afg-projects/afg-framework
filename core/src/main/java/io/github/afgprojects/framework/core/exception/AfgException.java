package io.github.afgprojects.framework.core.exception;

import java.io.Serial;

import lombok.Getter;

/**
 * AFG 平台异常基类
 * 所有业务异常的父类
 */
@Getter
public abstract class AfgException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    protected AfgException(int code, String message) {
        super(message);
        this.code = code;
    }

    protected AfgException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取格式化的错误码
     * @return 格式为 "E{code}"
     */
    public String formatCode() {
        return "E" + code;
    }
}
