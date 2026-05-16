package io.github.afgprojects.framework.ai.core.multiagent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Agent 通信总线接口
 *
 * <p>提供 Agent 之间的消息传递机制，支持发布-订阅和点对点通信。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface CommunicationBus {

    /**
     * 发布消息到指定主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    void publish(@NonNull String topic, @NonNull AgentMessage message);

    /**
     * 订阅主题
     *
     * @param topic    主题名称
     * @param agentId  订阅者 Agent ID
     * @param handler 消息处理器
     */
    void subscribe(@NonNull String topic, @NonNull String agentId, @NonNull Consumer<AgentMessage> handler);

    /**
     * 取消订阅
     *
     * @param topic   主题名称
     * @param agentId 订阅者 Agent ID
     */
    void unsubscribe(@NonNull String topic, @NonNull String agentId);

    /**
     * 发送消息给指定 Agent
     *
     * @param targetAgentId 目标 Agent ID
     * @param message       消息内容
     */
    void send(@NonNull String targetAgentId, @NonNull AgentMessage message);

    /**
     * 广播消息给所有 Agent
     *
     * @param message 消息内容
     */
    void broadcast(@NonNull AgentMessage message);

    /**
     * 获取 Agent 的待处理消息
     *
     * @param agentId Agent ID
     * @return 待处理消息列表
     */
    @NonNull List<AgentMessage> getPendingMessages(@NonNull String agentId);

    /**
     * 清除 Agent 的待处理消息
     *
     * @param agentId Agent ID
     */
    void clearPendingMessages(@NonNull String agentId);

    /**
     * Agent 消息
     *
     * @param messageId   消息 ID
     * @param senderId    发送者 Agent ID
     * @param receiverId  接收者 Agent ID（null 表示广播）
     * @param topic       主题
     * @param content     消息内容
     * @param timestamp   时间戳
     * @param metadata    元数据
     */
    record AgentMessage(
            @NonNull String messageId,
            @NonNull String senderId,
            @Nullable String receiverId,
            @Nullable String topic,
            @NonNull Object content,
            long timestamp,
            @Nullable Map<String, Object> metadata
    ) {
        /**
         * 创建点对点消息
         */
        public static @NonNull AgentMessage direct(
                @NonNull String senderId,
                @NonNull String receiverId,
                @NonNull Object content
        ) {
            return new AgentMessage(
                    UUID.randomUUID().toString(),
                    senderId,
                    receiverId,
                    null,
                    content,
                    System.currentTimeMillis(),
                    null
            );
        }

        /**
         * 创建主题消息
         */
        public static @NonNull AgentMessage toTopic(
                @NonNull String senderId,
                @NonNull String topic,
                @NonNull Object content
        ) {
            return new AgentMessage(
                    UUID.randomUUID().toString(),
                    senderId,
                    null,
                    topic,
                    content,
                    System.currentTimeMillis(),
                    null
            );
        }

        /**
         * 创建广播消息
         */
        public static @NonNull AgentMessage broadcast(
                @NonNull String senderId,
                @NonNull Object content
        ) {
            return new AgentMessage(
                    UUID.randomUUID().toString(),
                    senderId,
                    null,
                    null,
                    content,
                    System.currentTimeMillis(),
                    null
            );
        }
    }
}
