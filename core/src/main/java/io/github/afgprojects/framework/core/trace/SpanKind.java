package io.github.afgprojects.framework.core.trace;

/**
 * Span 类型枚举
 * <p>
 * 定义 Span 的类型，用于区分不同的调用场景
 * </p>
 */
public enum SpanKind {

    /**
     * 服务端 Span
     * <p>
     * 表示服务端处理请求，如 Controller 方法、Service 入口等
     * </p>
     */
    SERVER,

    /**
     * 客户端 Span
     * <p>
     * 表示客户端发起请求，如 HTTP 调用、RPC 调用、数据库访问等
     * </p>
     */
    CLIENT,

    /**
     * 生产者 Span
     * <p>
     * 表示消息生产者，如发送消息到消息队列
     * </p>
     */
    PRODUCER,

    /**
     * 消费者 Span
     * <p>
     * 表示消息消费者，如从消息队列接收消息
     * </p>
     */
    CONSUMER,

    /**
     * 内部 Span
     * <p>
     * 表示内部方法调用，不涉及跨服务通信
     * </p>
     */
    INTERNAL
}
