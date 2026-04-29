package io.github.afgprojects.framework.integration.jdbc.audit;

import java.time.LocalDateTime;

import io.github.afgprojects.framework.core.audit.AuditLog;
import org.jspecify.annotations.Nullable;

/**
 * 审计日志数据库实体
 * <p>
 * 用于将审计日志持久化到数据库
 * </p>
 *
 * @param id            日志唯一标识
 * @param traceId       链路追踪 ID
 * @param requestId     请求 ID
 * @param userId        操作用户 ID
 * @param username      操作用户名
 * @param tenantId      租户 ID
 * @param module        模块名称
 * @param operation     操作类型
 * @param target        操作对象标识
 * @param className     类名
 * @param methodName    方法名
 * @param args          方法参数（已脱敏）
 * @param oldValue      变更前值（可选）
 * @param newValue      变更后值（已脱敏）
 * @param result        操作结果（SUCCESS/FAILURE）
 * @param errorMessage  错误信息（失败时记录）
 * @param clientIp      客户端 IP
 * @param timestamp     操作时间
 * @param durationMs    操作耗时（毫秒）
 */
public record AuditLogRecord(
        String id,
        @Nullable String traceId,
        @Nullable String requestId,
        @Nullable Long userId,
        @Nullable String username,
        @Nullable Long tenantId,
        String module,
        String operation,
        @Nullable String target,
        String className,
        String methodName,
        @Nullable String args,
        @Nullable String oldValue,
        @Nullable String newValue,
        String result,
        @Nullable String errorMessage,
        @Nullable String clientIp,
        LocalDateTime timestamp,
        long durationMs) {

    /**
     * 从 AuditLog 创建 AuditLogRecord
     *
     * @param auditLog 审计日志
     * @return 审计日志数据库实体
     */
    public static AuditLogRecord from(AuditLog auditLog) {
        return new AuditLogRecord(
                auditLog.id(),
                auditLog.traceId(),
                auditLog.requestId(),
                auditLog.userId(),
                auditLog.username(),
                auditLog.tenantId(),
                auditLog.module(),
                auditLog.operation(),
                auditLog.target(),
                auditLog.className(),
                auditLog.methodName(),
                auditLog.args(),
                auditLog.oldValue(),
                auditLog.newValue(),
                auditLog.result().name(),
                auditLog.errorMessage(),
                auditLog.clientIp(),
                auditLog.timestamp(),
                auditLog.durationMs());
    }
}
