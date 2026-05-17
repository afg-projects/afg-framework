package io.github.afgprojects.framework.ai.core.multiagent.communication;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 消息
 *
 * @param messageId     消息 ID
 * @param fromAgent     发送者 Agent ID
 * @param toAgent       接收者 Agent ID（null 表示广播）
 * @param type          消息类型
 * @param payload       消息负载
 * @param metadata      元数据
 * @param timestamp     时间戳
 * @param correlationId 关联 ID（用于请求-响应模式）
 * @author afg-projects
 * @since 1.0.0
 */
public record AgentMessage(
        @NonNull String messageId,
        @NonNull String fromAgent,
        @Nullable String toAgent,
        @NonNull MessageType type,
        @Nullable Object payload,
        @NonNull Map<String, Object> metadata,
        @NonNull Instant timestamp,
        @Nullable String correlationId
) {
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TASK_REQUEST,    // 请求执行任务
        TASK_RESULT,     // 任务执行结果
        QUERY,           // 查询信息
        RESPONSE,        // 响应查询
        NOTIFICATION,    // 通知消息
        ERROR,           // 错误消息
        HEARTBEAT        // 心跳
    }

    /**
     * 创建任务请求消息
     *
     * @param from 发送者 Agent ID
     * @param to   接收者 Agent ID
     * @param task 任务数据
     * @return 任务请求消息
     */
    public static @NonNull AgentMessage taskRequest(@NonNull String from, @NonNull String to, @Nullable Object task) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, to,
                MessageType.TASK_REQUEST, task, Map.of(), Instant.now(), null
        );
    }

    /**
     * 创建任务结果消息
     *
     * @param from          发送者 Agent ID
     * @param to            接收者 Agent ID
     * @param result        任务结果
     * @param correlationId 关联 ID
     * @return 任务结果消息
     */
    public static @NonNull AgentMessage taskResult(@NonNull String from, @NonNull String to, @Nullable Object result, @NonNull String correlationId) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, to,
                MessageType.TASK_RESULT, result, Map.of(), Instant.now(), correlationId
        );
    }

    /**
     * 创建查询消息
     *
     * @param from  发送者 Agent ID
     * @param to    接收者 Agent ID
     * @param query 查询数据
     * @return 查询消息
     */
    public static @NonNull AgentMessage query(@NonNull String from, @NonNull String to, @Nullable Object query) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, to,
                MessageType.QUERY, query, Map.of(), Instant.now(), null
        );
    }

    /**
     * 创建响应消息
     *
     * @param from          发送者 Agent ID
     * @param to            接收者 Agent ID
     * @param response      响应数据
     * @param correlationId 关联 ID
     * @return 响应消息
     */
    public static @NonNull AgentMessage response(@NonNull String from, @NonNull String to, @Nullable Object response, @NonNull String correlationId) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, to,
                MessageType.RESPONSE, response, Map.of(), Instant.now(), correlationId
        );
    }

    /**
     * 创建通知消息
     *
     * @param from         发送者 Agent ID
     * @param notification 通知数据
     * @return 通知消息
     */
    public static @NonNull AgentMessage notification(@NonNull String from, @Nullable Object notification) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, null,
                MessageType.NOTIFICATION, notification, Map.of(), Instant.now(), null
        );
    }

    /**
     * 创建错误消息
     *
     * @param from          发送者 Agent ID
     * @param to            接收者 Agent ID
     * @param error         错误数据
     * @param correlationId 关联 ID
     * @return 错误消息
     */
    public static @NonNull AgentMessage error(@NonNull String from, @NonNull String to, @Nullable Object error, @Nullable String correlationId) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, to,
                MessageType.ERROR, error, Map.of(), Instant.now(), correlationId
        );
    }

    /**
     * 创建心跳消息
     *
     * @param from 发送者 Agent ID
     * @return 心跳消息
     */
    public static @NonNull AgentMessage heartbeat(@NonNull String from) {
        return new AgentMessage(
                UUID.randomUUID().toString(), from, null,
                MessageType.HEARTBEAT, null, Map.of(), Instant.now(), null
        );
    }
}
