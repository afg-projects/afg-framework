package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行上下文。
 *
 * <p>封装工具执行所需的安全上下文信息，包括：
 * <ul>
 *   <li>用户信息 - 当前执行用户</li>
 *   <li>租户信息 - 多租户隔离</li>
 *   <li>会话信息 - AI 会话追踪</li>
 *   <li>扩展属性 - 自定义上下文数据</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 从用户详情创建
 * ToolContext context = ToolContext.of(userDetails);
 *
 * // 使用 Builder 创建
 * ToolContext context = ToolContext.builder()
 *     .userId("user-001")
 *     .tenantId("tenant-001")
 *     .sessionId("session-001")
 *     .attribute("customKey", "customValue")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ToolContext {

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID，未认证时返回 null
     */
    @Nullable
    String getUserId();

    /**
     * 获取用户详情。
     *
     * <p>包含用户的完整信息，如角色、权限等。
     *
     * @return 用户详情，未认证时返回 null
     */
    @Nullable
    Object getUserDetails();

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID，单租户场景返回 null
     */
    @Nullable
    String getTenantId();

    /**
     * 获取组织 ID。
     *
     * @return 组织 ID，如无组织则返回 null
     */
    @Nullable
    default String getOrganizationId() {
        return null;
    }

    /**
     * 获取会话 ID。
     *
     * <p>用于追踪 AI 会话。
     *
     * @return 会话 ID
     */
    @Nullable
    String getSessionId();

    /**
     * 获取请求 ID。
     *
     * <p>用于分布式追踪。
     *
     * @return 请求 ID
     */
    @Nullable
    String getRequestId();

    /**
     * 获取客户端 IP。
     *
     * @return 客户端 IP 地址
     */
    @Nullable
    String getClientIp();

    /**
     * 获取扩展属性。
     *
     * @return 扩展属性 Map，永不为 null
     */
    @NonNull
    Map<String, Object> getAttributes();

    /**
     * 获取指定属性。
     *
     * @param key 属性名
     * @param <T> 属性类型
     * @return 属性值，不存在时返回 null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(@NonNull String key) {
        return (T) getAttributes().get(key);
    }

    /**
     * 判断是否已认证。
     *
     * @return 如果用户已认证则返回 true
     */
    default boolean isAuthenticated() {
        return getUserId() != null;
    }

    /**
     * 判断是否为管理员。
     *
     * @return 如果是管理员则返回 true
     */
    default boolean isAdmin() {
        return false;
    }

    /**
     * 获取用户角色集合。
     *
     * @return 角色集合，永不为 null
     */
    default java.util.Set<String> getRoles() {
        return java.util.Set.of();
    }

    /**
     * 获取原始 HTTP 请求头。
     *
     * <p>用于透传认证请求头到远程服务。
     *
     * @return 原始请求头 Map，永不为 null
     */
    default @NonNull Map<String, String> getOriginalHeaders() {
        return Map.of();
    }

    /**
     * 创建空上下文。
     *
     * @return 空上下文
     */
    static @NonNull ToolContext empty() {
        return DefaultToolContext.EMPTY;
    }

    /**
     * 从用户 ID 创建上下文。
     *
     * @param userId 用户 ID
     * @return 工具上下文
     */
    static @NonNull ToolContext of(@Nullable String userId) {
        if (userId == null) {
            return empty();
        }
        return new DefaultToolContext(userId, null, null, null, null, null, null, java.util.Set.of(), Map.of(), Map.of());
    }

    /**
     * 从用户 ID 和租户 ID 创建上下文。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 工具上下文
     */
    static @NonNull ToolContext of(@Nullable String userId, @Nullable String tenantId) {
        if (userId == null && tenantId == null) {
            return empty();
        }
        return new DefaultToolContext(userId, null, tenantId, null, null, null, null, java.util.Set.of(), Map.of(), Map.of());
    }

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * ToolContext Builder。
     */
    class Builder {
        private String userId;
        private Object userDetails;
        private String tenantId;
        private String organizationId;
        private String sessionId;
        private String requestId;
        private String clientIp;
        private java.util.Set<String> roles = java.util.Set.of();
        private Map<String, String> originalHeaders = Map.of();
        private Map<String, Object> attributes = new HashMap<>();

        /**
         * 设置用户 ID。
         *
         * @param userId 用户 ID
         * @return this
         */
        public @NonNull Builder userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置用户详情。
         *
         * @param userDetails 用户详情
         * @return this
         */
        public @NonNull Builder userDetails(@Nullable Object userDetails) {
            this.userDetails = userDetails;
            return this;
        }

        /**
         * 设置租户 ID。
         *
         * @param tenantId 租户 ID
         * @return this
         */
        public @NonNull Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 设置组织 ID。
         *
         * @param organizationId 组织 ID
         * @return this
         */
        public @NonNull Builder organizationId(@Nullable String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        /**
         * 设置会话 ID。
         *
         * @param sessionId 会话 ID
         * @return this
         */
        public @NonNull Builder sessionId(@Nullable String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * 设置请求 ID。
         *
         * @param requestId 请求 ID
         * @return this
         */
        public @NonNull Builder requestId(@Nullable String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 设置客户端 IP。
         *
         * @param clientIp 客户端 IP
         * @return this
         */
        public @NonNull Builder clientIp(@Nullable String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        /**
         * 设置用户角色。
         *
         * @param roles 用户角色集合
         * @return this
         */
        public @NonNull Builder roles(java.util.Set<String> roles) {
            this.roles = roles != null ? new java.util.HashSet<>(roles) : java.util.Set.of();
            return this;
        }

        /**
         * 设置原始 HTTP 请求头。
         *
         * <p>用于透传认证请求头到远程服务。
         *
         * @param originalHeaders 原始请求头
         * @return this
         */
        public @NonNull Builder originalHeaders(@Nullable Map<String, String> originalHeaders) {
            this.originalHeaders = originalHeaders != null ? new HashMap<>(originalHeaders) : Map.of();
            return this;
        }

        /**
         * 设置扩展属性。
         *
         * @param attributes 扩展属性
         * @return this
         */
        public @NonNull Builder attributes(@Nullable Map<String, Object> attributes) {
            this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
            return this;
        }

        /**
         * 添加单个扩展属性。
         *
         * @param key   属性名
         * @param value 属性值
         * @return this
         */
        public @NonNull Builder attribute(@NonNull String key, @Nullable Object value) {
            this.attributes.put(key, value);
            return this;
        }

        /**
         * 构建 ToolContext。
         *
         * @return ToolContext 实例
         */
        public @NonNull ToolContext build() {
            return new DefaultToolContext(
                userId,
                userDetails,
                tenantId,
                organizationId,
                sessionId,
                requestId,
                clientIp,
                roles,
                originalHeaders,
                Collections.unmodifiableMap(attributes)
            );
        }
    }
}
