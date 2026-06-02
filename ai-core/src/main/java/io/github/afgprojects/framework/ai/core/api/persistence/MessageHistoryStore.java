package io.github.afgprojects.framework.ai.core.api.persistence;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * 消息历史存储接口
 *
 * <p>用于管理 AI 对话消息历史：
 * <ul>
 *   <li>消息存储和检索</li>
 *   <li>消息分页查询</li>
 *   <li>消息搜索</li>
 *   <li>消息统计</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface MessageHistoryStore {

    /**
     * 添加消息
     *
     * @param sessionId 会话 ID
     * @param message   消息
     * @return 添加的消息
     */
    @NonNull
    Message addMessage(@NonNull String sessionId, @NonNull Message message);

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @NonNull
    List<Message> getMessages(@NonNull String sessionId);

    /**
     * 获取会话的消息（分页）
     *
     * @param sessionId 会话 ID
     * @param offset    偏移量
     * @param limit     最大数量
     * @return 消息列表
     */
    @NonNull
    List<Message> getMessages(@NonNull String sessionId, int offset, int limit);

    /**
     * 获取最近的 N 条消息
     *
     * @param sessionId 会话 ID
     * @param limit     最大数量
     * @return 消息列表（按时间倒序）
     */
    @NonNull
    List<Message> getRecentMessages(@NonNull String sessionId, int limit);

    /**
     * 获取指定消息
     *
     * @param messageId 消息 ID
     * @return 消息，如果不存在返回 null
     */
    @Nullable
    Message getMessage(@NonNull String messageId);

    /**
     * 更新消息
     *
     * @param message 消息
     */
    void updateMessage(@NonNull Message message);

    /**
     * 删除消息
     *
     * @param messageId 消息 ID
     */
    void deleteMessage(@NonNull String messageId);

    /**
     * 删除会话的所有消息
     *
     * @param sessionId 会话 ID
     */
    void deleteSessionMessages(@NonNull String sessionId);

    /**
     * 搜索消息
     *
     * @param sessionId 会话 ID
     * @param query     搜索关键词
     * @param limit     最大数量
     * @return 匹配的消息列表
     */
    @NonNull
    List<Message> searchMessages(@NonNull String sessionId, @NonNull String query, int limit);

    /**
     * 获取消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    long getMessageCount(@NonNull String sessionId);

    /**
     * 获取会话的 Token 统计
     *
     * @param sessionId 会话 ID
     * @return Token 统计
     */
    @NonNull
    TokenStats getTokenStats(@NonNull String sessionId);

    /**
     * 消息接口
     */
    interface Message {

        /**
         * 获取消息 ID
         *
         * @return 消息 ID
         */
        @NonNull
        String getMessageId();

        /**
         * 获取会话 ID
         *
         * @return 会话 ID
         */
        @NonNull
        String getSessionId();

        /**
         * 获取消息角色
         *
         * @return 消息角色
         */
        @NonNull
        MessageRole getRole();

        /**
         * 获取消息内容
         *
         * @return 消息内容
         */
        @NonNull
        String getContent();

        /**
         * 设置消息内容
         *
         * @param content 消息内容
         */
        void setContent(@NonNull String content);

        /**
         * 获取创建时间
         *
         * @return 创建时间
         */
        @NonNull
        Instant getCreatedAt();

        /**
         * 获取 Token 使用量
         *
         * @return Token 使用量
         */
        @Nullable
        TokenUsage getTokenUsage();

        /**
         * 设置 Token 使用量
         *
         * @param tokenUsage Token 使用量
         */
        void setTokenUsage(@Nullable TokenUsage tokenUsage);

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取元数据
         *
         * @return 元数据
         */
        java.util.@NonNull Map<String, String> getMetadata();

        /**
         * 获取父消息 ID（用于引用回复）
         *
         * @return 父消息 ID
         */
        @Nullable
        String getParentMessageId();

        /**
         * 获取消息状态
         *
         * @return 消息状态
         */
        @NonNull
        MessageStatus getStatus();

        /**
         * 设置消息状态
         *
         * @param status 消息状态
         */
        void setStatus(@NonNull MessageStatus status);
    }

    /**
     * 消息角色
     */
    enum MessageRole {
        /**
         * 系统
         */
        SYSTEM,
        /**
         * 用户
         */
        USER,
        /**
         * 助手
         */
        ASSISTANT,
        /**
         * 工具
         */
        TOOL
    }

    /**
     * 消息状态
     */
    enum MessageStatus {
        /**
         * 正常
         */
        NORMAL,
        /**
         * 已编辑
         */
        EDITED,
        /**
         * 已删除
         */
        DELETED,
        /**
         * 已隐藏
         */
        HIDDEN
    }

    /**
     * Token 使用量接口
     */
    interface TokenUsage {

        /**
         * 获取输入 Token 数
         *
         * @return 输入 Token 数
         */
        long getInputTokens();

        /**
         * 获取输出 Token 数
         *
         * @return 输出 Token 数
         */
        long getOutputTokens();

        /**
         * 获取总 Token 数
         *
         * @return 总 Token 数
         */
        long getTotalTokens();
    }

    /**
     * Token 统计接口
     */
    interface TokenStats {

        /**
         * 获取总输入 Token
         *
         * @return 总输入 Token
         */
        long getTotalInputTokens();

        /**
         * 获取总输出 Token
         *
         * @return 总输出 Token
         */
        long getTotalOutputTokens();

        /**
         * 获取总 Token
         *
         * @return 总 Token
         */
        long getTotalTokens();

        /**
         * 获取用户消息 Token
         *
         * @return 用户消息 Token
         */
        long getUserMessageTokens();

        /**
         * 获取助手消息 Token
         *
         * @return 助手消息 Token
         */
        long getAssistantMessageTokens();

        /**
         * 获取平均每条消息 Token
         *
         * @return 平均每条消息 Token
         */
        double getAverageTokensPerMessage();
    }
}