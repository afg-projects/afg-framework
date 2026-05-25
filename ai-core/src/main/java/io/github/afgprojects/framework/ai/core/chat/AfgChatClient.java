package io.github.afgprojects.framework.ai.core.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AFG 对话核心接口 - 与具体 AI 框架解耦的统一对话入口
 * <p>
 * 应用层面向此接口编程，实现层由 ai-chat 模块提供（基于 Spring AI ChatClient）。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AfgChatClient {

    // ---- 同步对话 ----

    /**
     * 简单同步对话
     *
     * @param userMessage 用户消息
     * @return AI 响应
     */
    @NonNull
    AiChatResponse chat(@NonNull String userMessage);

    /**
     * 带会话 ID 的同步对话（启用对话记忆）
     *
     * @param conversationId 会话 ID
     * @param userMessage    用户消息
     * @return AI 响应
     */
    @NonNull
    AiChatResponse chat(@NonNull String conversationId, @NonNull String userMessage);

    /**
     * 单条消息对话
     *
     * @param message AI 消息
     * @return AI 响应
     */
    @NonNull
    AiChatResponse chat(@NonNull AiMessage message);

    /**
     * 多轮对话
     *
     * @param messages 消息列表
     * @return AI 响应
     */
    @NonNull
    AiChatResponse chat(@NonNull List<AiMessage> messages);

    // ---- 流式对话 ----

    /**
     * 简单流式对话
     *
     * @param userMessage 用户消息
     * @return 流式响应内容
     */
    @NonNull
    Flux<String> chatStream(@NonNull String userMessage);

    /**
     * 带会话 ID 的流式对话（启用对话记忆）
     *
     * @param conversationId 会话 ID
     * @param userMessage    用户消息
     * @return 流式响应内容
     */
    @NonNull
    Flux<String> chatStream(@NonNull String conversationId, @NonNull String userMessage);

    // ---- 结构化输出 ----

    /**
     * 结构化输出 - AI 响应自动解析为指定类型
     *
     * @param userMessage 用户消息
     * @param responseType 目标类型
     * @return 解析后的对象
     */
    @NonNull
    <T> T chat(@NonNull String userMessage, @NonNull Class<T> responseType);

    // ---- 构建器式调用（高级） ----

    /**
     * 创建请求构建器
     *
     * @param userMessage 用户消息
     * @return 请求构建器
     */
    @NonNull
    ChatRequestSpec prompt(@NonNull String userMessage);

    // ---- 配置切换（返回新实例） ----

    /**
     * 切换系统提示词
     *
     * @param systemPrompt 系统提示词
     * @return 新的 ChatClient 实例
     */
    @NonNull
    AfgChatClient withSystemPrompt(@Nullable String systemPrompt);

    /**
     * 切换模型
     *
     * @param modelName 模型名称
     * @return 新的 ChatClient 实例
     */
    @NonNull
    AfgChatClient withModel(@Nullable String modelName);

    /**
     * 切换会话 ID
     *
     * @param conversationId 会话 ID
     * @return 新的 ChatClient 实例
     */
    @NonNull
    AfgChatClient withConversationId(@Nullable String conversationId);

    /**
     * 请求构建器接口 - 支持链式配置
     */
    interface ChatRequestSpec {

        ChatRequestSpec systemPrompt(@Nullable String systemPrompt);

        ChatRequestSpec conversationId(@Nullable String conversationId);

        ChatRequestSpec options(@NonNull Map<String, Object> options);

        @NonNull
        AiChatResponse call();

        @NonNull
        Flux<String> stream();

        @NonNull
        <T> T entity(@NonNull Class<T> type);
    }
}