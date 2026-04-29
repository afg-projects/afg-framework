package io.github.afgprojects.framework.core.event;

/**
 * 事件处理异常
 *
 * <p>当事件处理失败且重试次数耗尽时抛出此异常
 *
 * @since 1.0.0
 */
public class EventProcessingException extends RuntimeException {

    /**
     * 创建事件处理异常
     *
     * @param message 错误消息
     */
    public EventProcessingException(String message) {
        super(message);
    }

    /**
     * 创建事件处理异常
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建事件处理异常
     *
     * @param cause 原因
     */
    public EventProcessingException(Throwable cause) {
        super(cause);
    }
}
