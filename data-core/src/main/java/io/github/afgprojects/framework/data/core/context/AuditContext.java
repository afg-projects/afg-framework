package io.github.afgprojects.framework.data.core.context;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 审计上下文
 */
public interface AuditContext {

    /**
     * 获取当前用户ID
     */
    @Nullable String getCurrentUserId();

    /**
     * 获取当前用户名
     */
    @Nullable String getCurrentUsername();

    /**
     * 获取当前时间
     */
    Instant getCurrentTime();

    /**
     * 获取当前租户ID
     */
    @Nullable String getTenantId();
}
