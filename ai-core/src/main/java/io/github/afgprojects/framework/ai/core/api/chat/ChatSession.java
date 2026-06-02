package io.github.afgprojects.framework.ai.core.api.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 对话会话元数据
 *
 * @param conversationId 会话 ID
 * @param model          使用的模型
 * @param createdAt      创建时间
 * @param lastMessageAt  最后消息时间
 * @param messageCount   消息数量
 * @author afg-projects
 * @since 1.0.0
 */
public record ChatSession(
    @NonNull String conversationId,
    @NonNull String model,
    @NonNull Instant createdAt,
    @Nullable Instant lastMessageAt,
    int messageCount
) {
}