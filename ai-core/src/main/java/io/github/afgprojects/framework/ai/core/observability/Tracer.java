package io.github.afgprojects.framework.ai.core.observability;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 分布式追踪器接口
 *
 * <p>用于追踪 AI 操作的完整调用链：
 * <ul>
 *   <li>Span 创建和嵌套</li>
 *   <li>上下文传播</li>
 *   <li>事件记录</li>
 *   <li>错误追踪</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface Tracer {

    /**
     * 创建新的 Span
     *
     * @param operationName 操作名称
     * @return Span
     */
    @NonNull
    Span startSpan(@NonNull String operationName);

    /**
     * 创建新的 Span（带父 Span）
     *
     * @param operationName 操作名称
     * @param parentSpan    父 Span
     * @return Span
     */
    @NonNull
    Span startSpan(@NonNull String operationName, @Nullable Span parentSpan);

    /**
     * 创建新的 Span（带上下文）
     *
     * @param operationName 操作名称
     * @param context       追踪上下文
     * @return Span
     */
    @NonNull
    Span startSpanWithContext(@NonNull String operationName, @NonNull TraceContext context);

    /**
     * 获取当前活跃的 Span
     *
     * @return 当前 Span，如果没有则返回 null
     */
    @Nullable
    Span getCurrentSpan();

    /**
     * 从请求头提取追踪上下文
     *
     * @param headers 请求头
     * @return 追踪上下文
     */
    @NonNull
    TraceContext extractContext(@NonNull Map<String, String> headers);

    /**
     * 将追踪上下文注入到请求头
     *
     * @param context 追踪上下文
     * @return 请求头
     */
    @NonNull
    Map<String, String> injectContext(@NonNull TraceContext context);

    /**
     * Span 接口
     */
    interface Span {

        /**
         * 设置属性
         *
         * @param key   属性名
         * @param value 属性值
         * @return this
         */
        @NonNull
        Span setAttribute(@NonNull String key, @NonNull String value);

        /**
         * 设置属性（数值）
         *
         * @param key   属性名
         * @param value 属性值
         * @return this
         */
        @NonNull
        Span setAttribute(@NonNull String key, long value);

        /**
         * 设置属性（布尔）
         *
         * @param key   属性名
         * @param value 属性值
         * @return this
         */
        @NonNull
        Span setAttribute(@NonNull String key, boolean value);

        /**
         * 记录事件
         *
         * @param name 事件名称
         */
        void recordEvent(@NonNull String name);

        /**
         * 记录事件（带属性）
         *
         * @param name       事件名称
         * @param attributes 属性
         */
        void recordEvent(@NonNull String name, @NonNull Map<String, String> attributes);

        /**
         * 记录异常
         *
         * @param exception 异常
         */
        void recordException(@NonNull Exception exception);

        /**
         * 设置状态
         *
         * @param status 状态
         */
        void setStatus(@NonNull SpanStatus status);

        /**
         * 结束 Span
         */
        void end();

        /**
         * 结束 Span（带状态）
         *
         * @param status 状态
         */
        void end(@NonNull SpanStatus status);

        /**
         * 获取 Span ID
         *
         * @return Span ID
         */
        @NonNull
        String getSpanId();

        /**
         * 获取追踪 ID
         *
         * @return 追踪 ID
         */
        @NonNull
        String getTraceId();

        /**
         * 获取操作名称
         *
         * @return 操作名称
         */
        @NonNull
        String getOperationName();

        /**
         * 获取追踪上下文
         *
         * @return 追踪上下文
         */
        @NonNull
        TraceContext getContext();

        /**
         * 检查是否已结束
         *
         * @return 是否已结束
         */
        boolean isEnded();
    }

    /**
     * 追踪上下文接口
     */
    interface TraceContext {

        /**
         * 获取追踪 ID
         *
         * @return 追踪 ID
         */
        @NonNull
        String getTraceId();

        /**
         * 获取 Span ID
         *
         * @return Span ID
         */
        @NonNull
        String getSpanId();

        /**
         * 获取父 Span ID
         *
         * @return 父 Span ID，如果没有则返回 null
         */
        @Nullable
        String getParentSpanId();

        /**
         * 获取 Baggage（跨服务传递的数据）
         *
         * @param key 键
         * @return 值
         */
        @Nullable
        String getBaggage(@NonNull String key);

        /**
         * 设置 Baggage
         *
         * @param key   键
         * @param value 值
         */
        void setBaggage(@NonNull String key, @NonNull String value);

        /**
         * 获取所有 Baggage
         *
         * @return Baggage
         */
        @NonNull
        Map<String, String> getAllBaggage();
    }

    /**
     * Span 状态
     */
    enum SpanStatus {
        /**
         * 未设置
         */
        UNSET,
        /**
         * 成功
         */
        OK,
        /**
         * 错误
         */
        ERROR
    }
}