package io.github.afgprojects.framework.ai.core.api.multiagent.communication;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 通信总线接口
 *
 * <p>支持点对点和发布订阅两种模式。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface CommunicationBus {

    // ========== 点对点消息 ==========

    /**
     * 发送消息
     *
     * @param fromAgent 发送者 Agent ID
     * @param toAgent   接收者 Agent ID（null 表示广播）
     * @param message   消息
     */
    void send(@NonNull String fromAgent, @Nullable String toAgent, @NonNull AgentMessage message);

    /**
     * 接收消息（阻塞）
     *
     * @param agentId Agent ID
     * @param timeout 超时时间
     * @return 消息（可能为空）
     */
    @NonNull
    Optional<AgentMessage> receive(@NonNull String agentId, @NonNull Duration timeout);

    /**
     * 获取待处理消息
     *
     * @param agentId Agent ID
     * @return 待处理消息列表
     */
    @NonNull
    List<AgentMessage> getPendingMessages(@NonNull String agentId);

    /**
     * 清空消息队列
     *
     * @param agentId Agent ID
     */
    void clearMessages(@NonNull String agentId);

    // ========== 发布订阅 ==========

    /**
     * 订阅主题
     *
     * @param topic   主题名称
     * @param agentId 订阅者 Agent ID
     * @param handler 消息处理器
     */
    void subscribe(@NonNull String topic, @NonNull String agentId, @NonNull MessageHandler handler);

    /**
     * 取消订阅
     *
     * @param topic   主题名称
     * @param agentId 订阅者 Agent ID
     */
    void unsubscribe(@NonNull String topic, @NonNull String agentId);

    /**
     * 发布消息
     *
     * @param topic   主题名称
     * @param message 消息
     */
    void publish(@NonNull String topic, @NonNull AgentMessage message);

    // ========== 广播 ==========

    /**
     * 广播消息
     *
     * @param message 消息
     */
    void broadcast(@NonNull AgentMessage message);
}
