package io.github.afgprojects.framework.ai.langchain4j.internal;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage as Lc4jAiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.chat.AiRole;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AFG AiMessage 与 LangChain4j ChatMessage 双向转换器
 *
 * <p>此转换器位于 internal 包，供 chat、memory 等模块共享使用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class Lc4jMessageConverter {

    private Lc4jMessageConverter() {}

    /**
     * AFG AiMessage → LangChain4j ChatMessage
     */
    public static ChatMessage toLc4j(@NonNull AiMessage msg) {
        return switch (msg.role()) {
            case SYSTEM -> SystemMessage.from(msg.content());
            case USER -> {
                if (msg.hasMedia()) {
                    // LangChain4j UserMessage 支持多媒体内容
                    // 简化处理：仅使用文本内容
                    yield UserMessage.from(msg.content());
                }
                yield UserMessage.from(msg.content());
            }
            case ASSISTANT -> Lc4jAiMessage.from(msg.content());
            case TOOL -> ToolExecutionResultMessage.from(
                // Tool 消息需要 toolExecutionRequestId，从 metadata 中获取
                (String) msg.metadata().getOrDefault("toolExecutionRequestId", "unknown"),
                (String) msg.metadata().getOrDefault("toolName", "unknown"),
                msg.content()
            );
        };
    }

    /**
     * LangChain4j ChatMessage → AFG AiMessage
     */
    public static AiMessage fromLc4j(@NonNull ChatMessage msg) {
        if (msg instanceof SystemMessage systemMsg) {
            return AiMessage.system(systemMsg.text());
        }
        if (msg instanceof UserMessage userMsg) {
            return AiMessage.user(userMsg.singleText());
        }
        if (msg instanceof Lc4jAiMessage aiMsg) {
            if (aiMsg.hasToolExecutionRequests()) {
                // AI 消息包含工具调用请求
                return AiMessage.assistant(aiMsg.text(), Map.of(
                    "toolExecutionRequests", aiMsg.toolExecutionRequests()
                ));
            }
            return AiMessage.assistant(aiMsg.text());
        }
        if (msg instanceof ToolExecutionResultMessage toolMsg) {
            return AiMessage.tool(toolMsg.text(), Map.of(
                "toolName", toolMsg.toolName(),
                "toolExecutionRequestId", toolMsg.id()
            ));
        }
        // 降级处理
        return AiMessage.assistant(msg.text());
    }

    /**
     * 批量转换 AFG AiMessage 列表 → LangChain4j ChatMessage 列表
     */
    public static List<ChatMessage> toLc4jMessages(@NonNull List<AiMessage> messages) {
        var result = new ArrayList<ChatMessage>(messages.size());
        for (AiMessage msg : messages) {
            result.add(toLc4j(msg));
        }
        return result;
    }

    /**
     * 批量转换 LangChain4j ChatMessage 列表 → AFG AiMessage 列表
     */
    public static List<AiMessage> fromLc4jMessages(@NonNull List<ChatMessage> messages) {
        var result = new ArrayList<AiMessage>(messages.size());
        for (ChatMessage msg : messages) {
            result.add(fromLc4j(msg));
        }
        return result;
    }
}
