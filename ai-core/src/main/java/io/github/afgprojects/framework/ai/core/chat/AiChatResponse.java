package io.github.afgprojects.framework.ai.core.chat;

import org.jspecify.annotations.Nullable;

/**
 * AI 对话响应
 *
 * @param content  响应文本内容
 * @param metadata 响应元数据
 * @author afg-projects
 * @since 1.0.0
 */
public record AiChatResponse(
    @Nullable String content,
    @Nullable AiChatMetadata metadata
) {

    public static AiChatResponse of(@Nullable String content) {
        return new AiChatResponse(content, null);
    }

    public static AiChatResponse of(@Nullable String content, @Nullable AiChatMetadata metadata) {
        return new AiChatResponse(content, metadata);
    }

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }
}