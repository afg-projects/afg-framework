package io.github.afgprojects.framework.core.batch;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 批量操作执行结果
 * <p>
 * 记录批量处理的统计信息和错误详情
 * </p>
 *
 * @param <R> 结果类型
 */
@SuppressWarnings("PMD.UnusedAssignment")
public record BatchResult<R>(
        int total,
        int success,
        int failed,
        @NonNull List<R> results,
        @NonNull List<BatchError> errors,
        @NonNull Duration duration)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // NOPMD - record 紧凑构造器中重新赋值参数是正常行为
    public BatchResult {
        results = results != null ? Collections.unmodifiableList(new ArrayList<>(results)) : Collections.emptyList();
        errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    /**
     * 判断是否全部成功
     *
     * @return 如果所有操作都成功返回 true
     */
    public boolean isAllSuccess() {
        return failed == 0 && total > 0;
    }

    /**
     * 判断是否全部失败
     *
     * @return 如果所有操作都失败返回 true
     */
    public boolean isAllFailed() {
        return success == 0 && total > 0;
    }

    /**
     * 获取成功率
     *
     * @return 成功率（0.0 - 1.0）
     */
    public double getSuccessRate() {
        if (total == 0) {
            return 0.0;
        }
        return (double) success / total;
    }

    /**
     * 创建空结果
     *
     * @param <R> 结果类型
     * @return 空的批量结果
     */
    public static <R> BatchResult<R> empty() {
        return new BatchResult<>(0, 0, 0, Collections.emptyList(), Collections.emptyList(), Duration.ZERO);
    }

    /**
     * 创建构建器
     *
     * @param <R> 结果类型
     * @return 构建器实例
     */
    public static <R> Builder<R> builder() {
        return new Builder<>();
    }

    /**
     * 批量结果构建器
     *
     * @param <R> 结果类型
     */
    public static class Builder<R> {
        private int total;
        private int success;
        private int failed;
        private final List<R> results = new ArrayList<>();
        private final List<BatchError> errors = new ArrayList<>();
        private Duration duration = Duration.ZERO;

        /**
         * 设置总数
         */
        public Builder<R> total(int total) {
            this.total = total;
            return this;
        }

        /**
         * 设置成功数
         */
        public Builder<R> success(int success) {
            this.success = success;
            return this;
        }

        /**
         * 设置失败数
         */
        public Builder<R> failed(int failed) {
            this.failed = failed;
            return this;
        }

        /**
         * 添加成功结果
         */
        public Builder<R> addResult(@Nullable R result) {
            this.results.add(result);
            return this;
        }

        /**
         * 添加所有结果
         */
        public Builder<R> addAllResults(@NonNull List<R> results) {
            this.results.addAll(results);
            return this;
        }

        /**
         * 添加错误
         */
        public Builder<R> addError(@NonNull BatchError error) {
            this.errors.add(error);
            return this;
        }

        /**
         * 添加所有错误
         */
        public Builder<R> addAllErrors(@NonNull List<BatchError> errors) {
            this.errors.addAll(errors);
            return this;
        }

        /**
         * 设置执行耗时
         */
        public Builder<R> duration(@NonNull Duration duration) {
            this.duration = duration;
            return this;
        }

        /**
         * 构建批量结果
         */
        public BatchResult<R> build() {
            return new BatchResult<>(total, success, failed, results, errors, duration);
        }
    }
}
