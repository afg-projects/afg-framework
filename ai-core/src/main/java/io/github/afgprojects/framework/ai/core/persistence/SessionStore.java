package io.github.afgprojects.framework.ai.core.persistence;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 会话存储接口
 *
 * <p>用于管理 AI 对话会话：
 * <ul>
 *   <li>会话创建和检索</li>
 *   <li>会话状态管理</li>
 *   <li>会话元数据</li>
 *   <li>会话过期处理</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface SessionStore {

    /**
     * 创建新会话
     *
     * @param context 会话上下文
     * @return 新创建的会话
     */
    @NonNull
    Session createSession(@NonNull SessionContext context);

    /**
     * 创建新会话（指定 ID）
     *
     * @param sessionId 会话 ID
     * @param context   会话上下文
     * @return 新创建的会话
     */
    @NonNull
    Session createSession(@NonNull String sessionId, @NonNull SessionContext context);

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话，如果不存在返回 null
     */
    @Nullable
    Session getSession(@NonNull String sessionId);

    /**
     * 更新会话
     *
     * @param session 会话
     */
    void updateSession(@NonNull Session session);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(@NonNull String sessionId);

    /**
     * 获取用户的活跃会话
     *
     * @param userId 用户 ID
     * @param limit  最大数量
     * @return 会话列表
     */
    @NonNull
    List<Session> getActiveSessions(@NonNull String userId, int limit);

    /**
     * 获取会话历史
     *
     * @param userId 用户 ID
     * @param start  开始时间
     * @param end    结束时间
     * @param limit  最大数量
     * @return 会话列表
     */
    @NonNull
    List<Session> getSessionHistory(@NonNull String userId, @Nullable Instant start, @Nullable Instant end, int limit);

    /**
     * 清理过期会话
     *
     * @return 清理的会话数量
     */
    int cleanupExpiredSessions();

    /**
     * 会话接口
     */
    interface Session {

        /**
         * 获取会话 ID
         *
         * @return 会话 ID
         */
        @NonNull
        String getSessionId();

        /**
         * 获取用户 ID
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取会话状态
         *
         * @return 会话状态
         */
        @NonNull
        SessionState getState();

        /**
         * 设置会话状态
         *
         * @param state 会话状态
         */
        void setState(@NonNull SessionState state);

        /**
         * 获取创建时间
         *
         * @return 创建时间
         */
        @NonNull
        Instant getCreatedAt();

        /**
         * 获取最后活动时间
         *
         * @return 最后活动时间
         */
        @NonNull
        Instant getLastActiveAt();

        /**
         * 更新最后活动时间
         */
        void updateLastActiveAt();

        /**
         * 获取过期时间
         *
         * @return 过期时间，如果永不过期返回 null
         */
        @Nullable
        Instant getExpiresAt();

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取系统提示
         *
         * @return 系统提示
         */
        @Nullable
        String getSystemPrompt();

        /**
         * 获取元数据
         *
         * @return 元数据
         */
        @NonNull
        Map<String, String> getMetadata();

        /**
         * 设置元数据
         *
         * @param key   键
         * @param value 值
         */
        void setMetadata(@NonNull String key, @NonNull String value);

        /**
         * 获取消息数量
         *
         * @return 消息数量
         */
        int getMessageCount();

        /**
         * 获取总 Token 使用量
         *
         * @return 总 Token 使用量
         */
        long getTotalTokens();

        /**
         * 是否已过期
         *
         * @return 是否已过期
         */
        boolean isExpired();
    }

    /**
     * 会话状态
     */
    enum SessionState {
        /**
         * 活跃
         */
        ACTIVE,
        /**
         * 已暂停
         */
        PAUSED,
        /**
         * 已结束
         */
        ENDED,
        /**
         * 已过期
         */
        EXPIRED
    }

    /**
     * 会话上下文接口
     */
    interface SessionContext {

        /**
         * 获取用户 ID
         *
         * @return 用户 ID
         */
        @Nullable
        String getUserId();

        /**
         * 获取租户 ID
         *
         * @return 租户 ID
         */
        @Nullable
        String getTenantId();

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取系统提示
         *
         * @return 系统提示
         */
        @Nullable
        String getSystemPrompt();

        /**
         * 获取过期时间（秒）
         *
         * @return 过期时间，如果永不过期返回 null
         */
        @Nullable
        Long getExpiresInSeconds();

        /**
         * 获取初始元数据
         *
         * @return 初始元数据
         */
        @NonNull
        Map<String, String> getMetadata();
    }
}