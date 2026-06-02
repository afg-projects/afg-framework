package io.github.afgprojects.framework.ai.core.api.observability;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * 审计日志记录器接口
 *
 * <p>用于记录 AI 操作的审计信息：
 * <ul>
 *   <li>用户操作记录</li>
 *   <li>敏感数据访问</li>
 *   <li>权限变更</li>
 *   <li>安全事件</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AuditLogger {

    /**
     * 记录 AI 请求审计日志
     *
     * @param entry 审计日志条目
     */
    void log(@NonNull AuditEntry entry);

    /**
     * 记录 AI 请求审计日志（简化）
     *
     * @param userId        用户 ID
     * @param operationType 操作类型
     * @param modelName     模型名称
     * @param request       请求内容
     * @param response      响应内容
     * @param status        状态
     */
    void log(
            @Nullable String userId,
            @NonNull String operationType,
            @NonNull String modelName,
            @Nullable String request,
            @Nullable String response,
            @NonNull AuditStatus status
    );

    /**
     * 记录敏感数据访问
     *
     * @param userId      用户 ID
     * @param dataType    数据类型
     * @param dataId      数据 ID
     * @param accessType  访问类型
     * @param description 描述
     */
    void logSensitiveDataAccess(
            @Nullable String userId,
            @NonNull String dataType,
            @NonNull String dataId,
            @NonNull AccessType accessType,
            @NonNull String description
    );

    /**
     * 记录安全事件
     *
     * @param eventType   事件类型
     * @param severity    严重程度
     * @param description 描述
     * @param details     详细信息
     */
    void logSecurityEvent(
            @NonNull SecurityEventType eventType,
            @NonNull Severity severity,
            @NonNull String description,
            @NonNull Map<String, String> details
    );

    /**
     * 查询审计日志
     *
     * @param query 查询条件
     * @return 审计日志列表
     */
    @NonNull
    AuditLogResult query(@NonNull AuditQuery query);

    /**
     * 审计日志条目接口
     */
    interface AuditEntry {

        /**
         * 获取审计 ID
         *
         * @return 审计 ID
         */
        @NonNull
        String getId();

        /**
         * 获取时间戳
         *
         * @return 时间戳
         */
        @NonNull
        Instant getTimestamp();

        /**
         * 获取用户 ID
         *
         * @return 用户 ID，可能为 null（系统操作）
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
         * 获取操作类型
         *
         * @return 操作类型
         */
        @NonNull
        String getOperationType();

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取请求内容
         *
         * @return 请求内容（可能已脱敏）
         */
        @Nullable
        String getRequestContent();

        /**
         * 获取响应内容
         *
         * @return 响应内容（可能已脱敏）
         */
        @Nullable
        String getResponseContent();

        /**
         * 获取状态
         *
         * @return 状态
         */
        @NonNull
        AuditStatus getStatus();

        /**
         * 获取错误消息
         *
         * @return 错误消息，如果成功则为 null
         */
        @Nullable
        String getErrorMessage();

        /**
         * 获取 Token 使用量
         *
         * @return Token 使用量
         */
        @Nullable
        TokenUsage getTokenUsage();

        /**
         * 获取成本
         *
         * @return 成本（美元）
         */
        @Nullable
        Double getCost();

        /**
         * 获取响应时间
         *
         * @return 响应时间（毫秒）
         */
        @Nullable
        Long getResponseTimeMs();

        /**
         * 获取追踪 ID
         *
         * @return 追踪 ID
         */
        @Nullable
        String getTraceId();

        /**
         * 获取额外属性
         *
         * @return 额外属性
         */
        @NonNull
        Map<String, String> getAttributes();
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
     * 审计状态
     */
    enum AuditStatus {
        /**
         * 成功
         */
        SUCCESS,
        /**
         * 失败
         */
        FAILURE,
        /**
         * 超时
         */
        TIMEOUT,
        /**
         * 拒绝（权限或熔断）
         */
        REJECTED,
        /**
         * 降级
         */
        DEGRADED
    }

    /**
     * 访问类型
     */
    enum AccessType {
        /**
         * 读取
         */
        READ,
        /**
         * 写入
         */
        WRITE,
        /**
         * 删除
         */
        DELETE,
        /**
         * 导出
         */
        EXPORT
    }

    /**
     * 安全事件类型
     */
    enum SecurityEventType {
        /**
         * 认证失败
         */
        AUTH_FAILURE,
        /**
         * 权限拒绝
         */
        PERMISSION_DENIED,
        /**
         * 数据泄露风险
         */
        DATA_LEAK_RISK,
        /**
         * 异常访问模式
         */
        ABNORMAL_ACCESS,
        /**
         * 速率限制触发
         */
        RATE_LIMIT_TRIGGERED,
        /**
         * 熔断器打开
         */
        CIRCUIT_BREAKER_OPEN
    }

    /**
     * 严重程度
     */
    enum Severity {
        /**
         * 低
         */
        LOW,
        /**
         * 中
         */
        MEDIUM,
        /**
         * 高
         */
        HIGH,
        /**
         * 严重
         */
        CRITICAL
    }

    /**
     * 审计查询接口
     */
    interface AuditQuery {

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
         * 获取操作类型
         *
         * @return 操作类型
         */
        @Nullable
        String getOperationType();

        /**
         * 获取模型名称
         *
         * @return 模型名称
         */
        @Nullable
        String getModelName();

        /**
         * 获取状态
         *
         * @return 状态
         */
        @Nullable
        AuditStatus getStatus();

        /**
         * 获取开始时间
         *
         * @return 开始时间
         */
        @Nullable
        Instant getStartTime();

        /**
         * 获取结束时间
         *
         * @return 结束时间
         */
        @Nullable
        Instant getEndTime();

        /**
         * 获取追踪 ID
         *
         * @return 追踪 ID
         */
        @Nullable
        String getTraceId();

        /**
         * 获取页码
         *
         * @return 页码（从 0 开始）
         */
        int getPage();

        /**
         * 获取每页大小
         *
         * @return 每页大小
         */
        int getSize();
    }

    /**
     * 审计日志查询结果接口
     */
    interface AuditLogResult {

        /**
         * 获取审计日志列表
         *
         * @return 审计日志列表
         */
        java.util.List<AuditEntry> getEntries();

        /**
         * 获取总数
         *
         * @return 总数
         */
        long getTotal();

        /**
         * 获取页码
         *
         * @return 页码
         */
        int getPage();

        /**
         * 获取每页大小
         *
         * @return 每页大小
         */
        int getSize();

        /**
         * 是否有下一页
         *
         * @return 是否有下一页
         */
        boolean hasNext();
    }
}