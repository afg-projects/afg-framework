package io.github.afgprojects.framework.ai.core.api.chat;

import org.jspecify.annotations.Nullable;

/**
 * AI 对话响应元数据
 *
 * @param promptTokens     输入 token 数量
 * @param completionTokens 输出 token 数量
 * @param totalTokens      总 token 数量
 * @param finishReason     结束原因（stop、tool_calls、length 等）
 * @param model            实际使用的模型名称
 * @author afg-projects
 * @since 1.0.0
 */
public record AiChatMetadata(
    @Nullable Long promptTokens,
    @Nullable Long completionTokens,
    @Nullable Long totalTokens,
    @Nullable String finishReason,
    @Nullable String model
) {
}