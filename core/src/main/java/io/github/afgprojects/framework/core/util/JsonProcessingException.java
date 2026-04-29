package io.github.afgprojects.framework.core.util;

import java.io.Serial;

/**
 * JSON 处理异常
 * 用于包装 JSON 序列化/反序列化过程中的异常
 */
public class JsonProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public JsonProcessingException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
