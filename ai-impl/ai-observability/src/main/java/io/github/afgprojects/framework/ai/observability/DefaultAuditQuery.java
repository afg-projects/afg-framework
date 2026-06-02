package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 默认审计查询实现
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultAuditQuery implements AuditLogger.AuditQuery {

    private String userId;
    private String tenantId;
    private String operationType;
    private String modelName;
    private AuditLogger.AuditStatus status;
    private Instant startTime;
    private Instant endTime;
    private String traceId;
    private int page = 0;
    private int size = 20;

    @Override
    @Nullable
    public String getUserId() {
        return userId;
    }

    public DefaultAuditQuery userId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    @Nullable
    public String getTenantId() {
        return tenantId;
    }

    public DefaultAuditQuery tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    @Nullable
    public String getOperationType() {
        return operationType;
    }

    public DefaultAuditQuery operationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    @Override
    @Nullable
    public String getModelName() {
        return modelName;
    }

    public DefaultAuditQuery modelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    @Override
    public AuditLogger.@Nullable AuditStatus getStatus() {
        return status;
    }

    public DefaultAuditQuery status(AuditLogger.AuditStatus status) {
        this.status = status;
        return this;
    }

    @Override
    @Nullable
    public Instant getStartTime() {
        return startTime;
    }

    public DefaultAuditQuery startTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    @Override
    @Nullable
    public Instant getEndTime() {
        return endTime;
    }

    public DefaultAuditQuery endTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    @Override
    @Nullable
    public String getTraceId() {
        return traceId;
    }

    public DefaultAuditQuery traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    @Override
    public int getPage() {
        return page;
    }

    public DefaultAuditQuery page(int page) {
        this.page = page;
        return this;
    }

    @Override
    public int getSize() {
        return size;
    }

    public DefaultAuditQuery size(int size) {
        this.size = size;
        return this;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     */
    public static class Builder {

        private final DefaultAuditQuery query = new DefaultAuditQuery();

        public Builder userId(String userId) {
            query.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            query.tenantId = tenantId;
            return this;
        }

        public Builder operationType(String operationType) {
            query.operationType = operationType;
            return this;
        }

        public Builder modelName(String modelName) {
            query.modelName = modelName;
            return this;
        }

        public Builder status(AuditLogger.AuditStatus status) {
            query.status = status;
            return this;
        }

        public Builder startTime(Instant startTime) {
            query.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            query.endTime = endTime;
            return this;
        }

        public Builder traceId(String traceId) {
            query.traceId = traceId;
            return this;
        }

        public Builder page(int page) {
            query.page = page;
            return this;
        }

        public Builder size(int size) {
            query.size = size;
            return this;
        }

        public DefaultAuditQuery build() {
            return query;
        }
    }
}