package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ETL 执行结果。
 *
 * @param totalDocuments       总文档数
 * @param successCount         成功数
 * @param failureCount         失败数
 * @param successfulDocuments  成功文档列表
 * @param failures             失败记录列表
 * @param duration             执行时长
 * @param metadata             额外元数据
 * @author AFG Projects
 * @since 1.0.0
 */
public record EtlResult(
    int totalDocuments,
    int successCount,
    int failureCount,
    @NonNull List<Document> successfulDocuments,
    @NonNull List<DocumentFailure> failures,
    @NonNull Duration duration,
    @NonNull Map<String, Object> metadata
) {

    /**
     * 是否全部成功。
     */
    public boolean isSuccess() {
        return failureCount == 0;
    }

    /**
     * 获取成功率。
     */
    public double getSuccessRate() {
        return totalDocuments == 0 ? 0.0 : (double) successCount / totalDocuments;
    }

    /**
     * 创建空结果。
     */
    @NonNull
    public static EtlResult empty(@NonNull Duration duration) {
        return new EtlResult(0, 0, 0, List.of(), List.of(), duration, Map.of());
    }

    /**
     * 创建构建器。
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器。
     */
    public static class Builder {
        private int totalDocuments;
        private int successCount;
        private int failureCount;
        private List<Document> successfulDocuments = List.of();
        private List<DocumentFailure> failures = List.of();
        private Duration duration = Duration.ZERO;
        private Map<String, Object> metadata = Map.of();

        public Builder totalDocuments(int totalDocuments) {
            this.totalDocuments = totalDocuments;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder successfulDocuments(@NonNull List<Document> documents) {
            this.successfulDocuments = documents;
            return this;
        }

        public Builder failures(@NonNull List<DocumentFailure> failures) {
            this.failures = failures;
            return this;
        }

        public Builder duration(@NonNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder metadata(@NonNull Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        @NonNull
        public EtlResult build() {
            return new EtlResult(
                totalDocuments,
                successCount,
                failureCount,
                Collections.unmodifiableList(successfulDocuments),
                Collections.unmodifiableList(failures),
                duration,
                Collections.unmodifiableMap(metadata)
            );
        }
    }
}