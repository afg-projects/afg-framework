package io.github.afgprojects.framework.ai.core.api.memory;

import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 对话记忆接口 - 与具体 AI 框架解耦
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ConversationMemory {

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话 ID
     * @param message   AI 消息
     */
    void addMessage(@NonNull String sessionId, @NonNull AiMessage message);

    /**
     * 获取会话历史消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @NonNull
    List<AiMessage> getHistory(@NonNull String sessionId);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话 ID
     */
    void clear(@NonNull String sessionId);

    /**
     * 获取最近 N 条消息
     *
     * @param sessionId 会话 ID
     * @param n         消息数量
     * @return 最近 N 条消息
     */
    @NonNull
    List<AiMessage> getRecentMessages(@NonNull String sessionId, int n);
}