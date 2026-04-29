package io.github.afgprojects.framework.core.audit;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 审计日志记录
 * <p>
 * 存储操作审计的完整信息
 * </p>
 *
 * @param id           日志唯一标识
 * @param userId       操作用户 ID
 * @param username     操作用户名
 * @param tenantId     租户 ID
 * @param operation    操作类型
 * @param module       模块名称
 * @param target       操作对象标识
 * @param args         方法参数（已脱敏）
 * @param oldValue     变更前值（可选）
 * @param newValue     变更后值（已脱敏）
 * @param result       操作结果（SUCCESS/FAILURE）
 * @param errorMessage 错误信息（失败时记录）
 * @param timestamp    操作时间
 * @param durationMs   操作耗时（毫秒）
 * @param traceId      链路追踪 ID
 * @param requestId    请求 ID
 * @param clientIp     客户端 IP
 * @param className    类名
 * @param methodName   方法名
 */
public record AuditLog(
        String id,
        @Nullable Long userId,
        @Nullable String username,
        @Nullable Long tenantId,
        String operation,
        String module,
        @Nullable String target,
        @Nullable String args,
        @Nullable String oldValue,
        @Nullable String newValue,
        Result result,
        @Nullable String errorMessage,
        LocalDateTime timestamp,
        long durationMs,
        @Nullable String traceId,
        @Nullable String requestId,
        @Nullable String clientIp,
        String className,
        String methodName) {

    /**
     * 操作结果枚举
     */
    public enum Result {
        /** 操作成功 */
        SUCCESS,
        /** 操作失败 */
        FAILURE
    }

    /**
     * 创建成功日志构建器
     *
     * @return 构建器实例
     */
    public static Builder successBuilder() {
        return new Builder().result(Result.SUCCESS);
    }

    /**
     * 创建失败日志构建器
     *
     * @return 构建器实例
     */
    public static Builder failureBuilder() {
        return new Builder().result(Result.FAILURE);
    }

    /**
     * 审计日志构建器
     */
    @SuppressWarnings("PMD.TooManyFields")
    public static final class Builder {
        private String id;
        private Long userId;
        private String username;
        private Long tenantId;
        private String operation;
        private String module;
        private String target;
        private String args;
        private String oldValue;
        private String newValue;
        private Result result;
        private String errorMessage;
        private LocalDateTime timestamp;
        private long durationMs;
        private String traceId;
        private String requestId;
        private String clientIp;
        private String className;
        private String methodName;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder args(String args) {
            this.args = args;
            return this;
        }

        public Builder oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public Builder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public Builder result(Result result) {
            this.result = result;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        /**
         * 构建审计日志实例
         *
         * @return 审计日志实例
         */
        public AuditLog build() {
            return new AuditLog(
                    id,
                    userId,
                    username,
                    tenantId,
                    operation,
                    module,
                    target,
                    args,
                    oldValue,
                    newValue,
                    result,
                    errorMessage,
                    timestamp != null ? timestamp : LocalDateTime.now(),
                    durationMs,
                    traceId,
                    requestId,
                    clientIp,
                    className,
                    methodName);
        }
    }
}
