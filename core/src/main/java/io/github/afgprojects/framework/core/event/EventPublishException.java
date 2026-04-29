package io.github.afgprojects.framework.core.event;

/**
 * 事件发布异常
 *
 * <p>当事件发布失败时抛出此异常
 *
 * @since 1.0.0
 */
public class EventPublishException extends RuntimeException {

    /**
     * 创建事件发布异常
     *
     * @param message 错误消息
     */
    public EventPublishException(String message) {
        super(message);
    }

    /**
     * 创建事件发布异常
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建事件发布异常
     *
     * @param cause 原因
     */
    public EventPublishException(Throwable cause) {
        super(cause);
    }
}
