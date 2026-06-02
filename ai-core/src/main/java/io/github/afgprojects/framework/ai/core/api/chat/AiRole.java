package io.github.afgprojects.framework.ai.core.api.chat;

/**
 * AI 角色枚举
 *
 * @author afg-projects
 * @since 1.0.0
 */
public enum AiRole {

    /**
     * 系统消息 - 设置 AI 的行为和上下文
     */
    SYSTEM,

    /**
     * 用户消息 - 来自人类的输入
     */
    USER,

    /**
     * AI 助手消息 - AI 的响应
     */
    ASSISTANT,

    /**
     * 工具消息 - 工具执行的结果
     */
    TOOL
}