package io.github.afgprojects.framework.core.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 基于日志的审计日志存储
 * <p>
 * 将审计日志输出到日志文件，适用于轻量级场景
 * 或作为 Redis 不可用时的降级方案
 * </p>
 */
public class LogAuditLogStorage implements AuditLogStorage {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOG");

    @Override
    public void save(AuditLog auditLog) {
        // 使用 JSON 格式输出，便于日志分析系统解析
        AUDIT_LOG.info("{}", JacksonUtils.toJson(auditLog));
    }
}
