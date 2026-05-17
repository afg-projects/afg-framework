package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 默认工具上下文实现。
 *
 * <p>使用 record 实现，不可变且线程安全。
 *
 * @param userId          用户 ID
 * @param userDetails     用户详情
 * @param tenantId        租户 ID
 * @param organizationId  组织 ID
 * @param sessionId       会话 ID
 * @param requestId       请求 ID
 * @param clientIp        客户端 IP
 * @param roles           用户角色
 * @param originalHeaders 原始 HTTP 请求头（用于透传认证）
 * @param attributes      扩展属性
 * @since 1.0.0
 */
public record DefaultToolContext(
    @Nullable String userId,
    @Nullable Object userDetails,
    @Nullable String tenantId,
    @Nullable String organizationId,
    @Nullable String sessionId,
    @Nullable String requestId,
    @Nullable String clientIp,
    @NonNull Set<String> roles,
    @NonNull Map<String, String> originalHeaders,
    @NonNull Map<String, Object> attributes
) implements ToolContext {

    /**
     * 空上下文单例。
     */
    public static final DefaultToolContext EMPTY = new DefaultToolContext(
        null, null, null, null, null, null, null, Set.of(), Map.of(), Map.of()
    );

    /**
     * 构造函数，确保集合不可变。
     */
    public DefaultToolContext {
        roles = Collections.unmodifiableSet(roles);
        originalHeaders = Collections.unmodifiableMap(originalHeaders);
        attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public @Nullable String getUserId() {
        return userId;
    }

    @Override
    public @Nullable Object getUserDetails() {
        return userDetails;
    }

    @Override
    public @Nullable String getTenantId() {
        return tenantId;
    }

    @Override
    public @Nullable String getOrganizationId() {
        return organizationId;
    }

    @Override
    public @Nullable String getSessionId() {
        return sessionId;
    }

    @Override
    public @Nullable String getRequestId() {
        return requestId;
    }

    @Override
    public @Nullable String getClientIp() {
        return clientIp;
    }

    @Override
    public @NonNull Set<String> getRoles() {
        return roles;
    }

    @Override
    public @NonNull Map<String, String> getOriginalHeaders() {
        return originalHeaders;
    }

    @Override
    public @NonNull Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isAdmin() {
        // 如果 userDetails 实现了 isAdmin 方法，通过反射调用
        if (userDetails != null) {
            try {
                var method = userDetails.getClass().getMethod("isAdmin");
                return (boolean) method.invoke(userDetails);
            } catch (Exception e) {
                // 忽略异常
            }
        }
        return false;
    }
}
