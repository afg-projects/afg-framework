package io.github.afgprojects.framework.core.event;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 死信事件
 *
 * <p>封装失败事件的信息，用于发送到死信队列。
 * 包含原始事件、失败原因、重试次数等元数据。
 *
 * @param <T> 原始事件载荷类型
 * @since 1.0.0
 */
public record DeadLetterEvent<T>(
        /**
         * 死信事件 ID
         */
        @NonNull String eventId,

        /**
         * 原始事件类型
         */
        @NonNull String originalEventType,

        /**
         * 原始事件载荷（JSON 字符串）
         */
        @NonNull String originalEventJson,

        /**
         * 失败原因
         */
        @NonNull String failureReason,

        /**
         * 异常堆栈（可选）
         */
        @Nullable String stackTrace,

        /**
         * 重试次数
         */
        int retryCount,

        /**
         * 最大重试次数
         */
        int maxRetryCount,

        /**
         * 原始主题
         */
        @NonNull String originalTopic,

        /**
         * 失败时间
         */
        @NonNull Instant failedAt,

        /**
         * 下次重试时间（可选）
         */
        @Nullable Instant nextRetryTime,

        /**
         * 扩展属性
         */
        @NonNull Map<String, Object> metadata) implements DomainEvent<T> {

    /**
     * 创建死信事件
     */
    public DeadLetterEvent {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * 判断是否可以重试
     *
     * @return 是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }

    /**
     * 获取剩余重试次数
     *
     * @return 剩余重试次数
     */
    public int remainingRetries() {
        return Math.max(0, maxRetryCount - retryCount);
    }

    // === DomainEvent 接口实现 ===

    @Override
    @NonNull
    public String getEventId() {
        return eventId;
    }

    @Override
    @NonNull
    public String getEventType() {
        return "dead-letter." + originalEventType;
    }

    @Override
    @NonNull
    public Instant getTimestamp() {
        return failedAt;
    }

    @Override
    @Nullable
    public String getAggregateId() {
        return null;
    }

    @Override
    @Nullable
    public T getPayload() {
        return null;
    }

    /**
     * 创建构建器
     *
     * @param <T> 事件载荷类型
     * @return 构建器
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 死信事件构建器
     */
    public static class Builder<T> {
        private String eventId;
        private String originalEventType;
        private String originalEventJson;
        private String failureReason;
        private String stackTrace;
        private int retryCount;
        private int maxRetryCount = 3;
        private String originalTopic;
        private Instant failedAt;
        private Instant nextRetryTime;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder<T> eventId(@NonNull String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder<T> originalEventType(@NonNull String originalEventType) {
            this.originalEventType = originalEventType;
            return this;
        }

        public Builder<T> originalEventJson(@NonNull String originalEventJson) {
            this.originalEventJson = originalEventJson;
            return this;
        }

        public Builder<T> failureReason(@NonNull String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder<T> stackTrace(@Nullable String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder<T> retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder<T> maxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            return this;
        }

        public Builder<T> originalTopic(@NonNull String originalTopic) {
            this.originalTopic = originalTopic;
            return this;
        }

        public Builder<T> failedAt(@NonNull Instant failedAt) {
            this.failedAt = failedAt;
            return this;
        }

        public Builder<T> nextRetryTime(@Nullable Instant nextRetryTime) {
            this.nextRetryTime = nextRetryTime;
            return this;
        }

        public Builder<T> metadata(@NonNull String key, @NonNull Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder<T> metadata(@NonNull Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public DeadLetterEvent<T> build() {
            if (failedAt == null) {
                failedAt = Instant.now();
            }
            return new DeadLetterEvent<>(
                    eventId,
                    originalEventType,
                    originalEventJson,
                    failureReason,
                    stackTrace,
                    retryCount,
                    maxRetryCount,
                    originalTopic,
                    failedAt,
                    nextRetryTime,
                    metadata);
        }
    }
}
