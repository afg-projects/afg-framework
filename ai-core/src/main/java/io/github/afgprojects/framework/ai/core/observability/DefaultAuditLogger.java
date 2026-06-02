package io.github.afgprojects.framework.ai.core.observability;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 默认审计日志记录器实现
 *
 * <p>基于内存的简单审计日志记录器，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>不需要持久化的场景</li>
 *   <li>小规模审计日志</li>
 * </ul>
 *
 * <p>生产环境建议使用数据库或专业审计系统实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultAuditLogger implements AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuditLogger.class);

    private final Deque<AuditEntry> auditLogs = new ConcurrentLinkedDeque<>();
    private final int maxEntries;

    /**
     * 创建默认审计日志记录器
     *
     * @param maxEntries 最大条目数（超过后删除最旧的）
     */
    public DefaultAuditLogger(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * 创建默认审计日志记录器（默认最大 10000 条）
     */
    public DefaultAuditLogger() {
        this(10000);
    }

    @Override
    public void log(@NonNull AuditEntry entry) {
        auditLogs.addFirst(entry);

        // 超过最大条目数时删除最旧的
        while (auditLogs.size() > maxEntries) {
            auditLogs.removeLast();
        }

        log.debug("Audit log recorded: {} (user={}, status={})",
                entry.getOperationType(), entry.getUserId(), entry.getStatus());
    }

    @Override
    public void log(
            @Nullable String userId,
            @NonNull String operationType,
            @NonNull String modelName,
            @Nullable String request,
            @Nullable String response,
            @NonNull AuditStatus status
    ) {
        DefaultAuditEntry entry = new DefaultAuditEntry(
                generateId(),
                Instant.now(),
                userId,
                null,
                operationType,
                modelName,
                request,
                response,
                status,
                null,
                null,
                null,
                null,
                null,
                Map.of()
        );

        log(entry);
    }

    @Override
    public void logSensitiveDataAccess(
            @Nullable String userId,
            @NonNull String dataType,
            @NonNull String dataId,
            @NonNull AccessType accessType,
            @NonNull String description
    ) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("dataType", dataType);
        attributes.put("dataId", dataId);
        attributes.put("accessType", accessType.name());

        DefaultAuditEntry entry = new DefaultAuditEntry(
                generateId(),
                Instant.now(),
                userId,
                null,
                "SENSITIVE_DATA_ACCESS",
                null,
                null,
                null,
                AuditStatus.SUCCESS,
                null,
                null,
                null,
                null,
                null,
                attributes
        );

        log(entry);

        log.info("Sensitive data access: user={}, type={}, id={}, access={}",
                userId, dataType, dataId, accessType);
    }

    @Override
    public void logSecurityEvent(
            @NonNull SecurityEventType eventType,
            @NonNull Severity severity,
            @NonNull String description,
            @NonNull Map<String, String> details
    ) {
        Map<String, String> attributes = new HashMap<>(details);
        attributes.put("severity", severity.name());

        DefaultAuditEntry entry = new DefaultAuditEntry(
                generateId(),
                Instant.now(),
                null,
                null,
                "SECURITY_EVENT:" + eventType.name(),
                null,
                null,
                description,
                AuditStatus.SUCCESS,
                null,
                null,
                null,
                null,
                null,
                attributes
        );

        log(entry);

        if (severity == Severity.HIGH || severity == Severity.CRITICAL) {
            log.warn("Security event: type={}, severity={}, description={}",
                    eventType, severity, description);
        } else {
            log.info("Security event: type={}, severity={}, description={}",
                    eventType, severity, description);
        }
    }

    @Override
    @NonNull
    public AuditLogResult query(@NonNull AuditQuery query) {
        List<AuditEntry> filtered = auditLogs.stream()
                .filter(entry -> matchesQuery(entry, query))
                .skip((long) query.getPage() * query.getSize())
                .limit(query.getSize() + 1)
                .collect(Collectors.toList());

        boolean hasNext = filtered.size() > query.getSize();
        List<AuditEntry> entries = hasNext ? filtered.subList(0, query.getSize()) : filtered;

        long total = auditLogs.stream()
                .filter(entry -> matchesQuery(entry, query))
                .count();

        return new DefaultAuditLogResult(entries, total, query.getPage(), query.getSize(), hasNext);
    }

    private boolean matchesQuery(AuditEntry entry, AuditQuery query) {
        if (query.getUserId() != null && !query.getUserId().equals(entry.getUserId())) {
            return false;
        }
        if (query.getTenantId() != null && !query.getTenantId().equals(entry.getTenantId())) {
            return false;
        }
        if (query.getOperationType() != null && !query.getOperationType().equals(entry.getOperationType())) {
            return false;
        }
        if (query.getModelName() != null && !query.getModelName().equals(entry.getModelName())) {
            return false;
        }
        if (query.getStatus() != null && query.getStatus() != entry.getStatus()) {
            return false;
        }
        if (query.getStartTime() != null && entry.getTimestamp().isBefore(query.getStartTime())) {
            return false;
        }
        if (query.getEndTime() != null && entry.getTimestamp().isAfter(query.getEndTime())) {
            return false;
        }
        if (query.getTraceId() != null && !query.getTraceId().equals(entry.getTraceId())) {
            return false;
        }
        return true;
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 默认审计日志条目实现
     */
    @SuppressWarnings("PMD.ExcessiveParameterList")
    private static class DefaultAuditEntry implements AuditEntry {

        private final String id;
        private final Instant timestamp;
        private final String userId;
        private final String tenantId;
        private final String operationType;
        private final String modelName;
        private final String requestContent;
        private final String responseContent;
        private final AuditStatus status;
        private final String errorMessage;
        private final TokenUsage tokenUsage;
        private final Double cost;
        private final Long responseTimeMs;
        private final String traceId;
        private final Map<String, String> attributes;

        DefaultAuditEntry(String id, Instant timestamp, String userId, String tenantId,
                          String operationType, String modelName, String requestContent,
                          String responseContent, AuditStatus status, String errorMessage,
                          TokenUsage tokenUsage, Double cost, Long responseTimeMs,
                          String traceId, Map<String, String> attributes) {
            this.id = id;
            this.timestamp = timestamp;
            this.userId = userId;
            this.tenantId = tenantId;
            this.operationType = operationType;
            this.modelName = modelName;
            this.requestContent = requestContent;
            this.responseContent = responseContent;
            this.status = status;
            this.errorMessage = errorMessage;
            this.tokenUsage = tokenUsage;
            this.cost = cost;
            this.responseTimeMs = responseTimeMs;
            this.traceId = traceId;
            this.attributes = attributes;
        }

        @Override
        @NonNull
        public String getId() {
            return id;
        }

        @Override
        @NonNull
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        @Nullable
        public String getUserId() {
            return userId;
        }

        @Override
        @Nullable
        public String getTenantId() {
            return tenantId;
        }

        @Override
        @NonNull
        public String getOperationType() {
            return operationType;
        }

        @Override
        @Nullable
        public String getModelName() {
            return modelName;
        }

        @Override
        @Nullable
        public String getRequestContent() {
            return requestContent;
        }

        @Override
        @Nullable
        public String getResponseContent() {
            return responseContent;
        }

        @Override
        @NonNull
        public AuditStatus getStatus() {
            return status;
        }

        @Override
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        @Nullable
        public TokenUsage getTokenUsage() {
            return tokenUsage;
        }

        @Override
        @Nullable
        public Double getCost() {
            return cost;
        }

        @Override
        @Nullable
        public Long getResponseTimeMs() {
            return responseTimeMs;
        }

        @Override
        @Nullable
        public String getTraceId() {
            return traceId;
        }

        @Override
        @NonNull
        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    /**
     * 默认审计日志查询结果实现
     */
    private static class DefaultAuditLogResult implements AuditLogResult {

        private final List<AuditEntry> entries;
        private final long total;
        private final int page;
        private final int size;
        private final boolean hasNext;

        DefaultAuditLogResult(List<AuditEntry> entries, long total, int page, int size, boolean hasNext) {
            this.entries = entries;
            this.total = total;
            this.page = page;
            this.size = size;
            this.hasNext = hasNext;
        }

        @Override
        @NonNull
        public List<AuditEntry> getEntries() {
            return entries;
        }

        @Override
        public long getTotal() {
            return total;
        }

        @Override
        public int getPage() {
            return page;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }
    }
}
