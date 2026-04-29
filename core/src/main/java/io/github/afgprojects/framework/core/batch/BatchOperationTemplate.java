package io.github.afgprojects.framework.core.batch;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.batch.BatchResult.Builder;

/**
 * 批量操作模板
 * <p>
 * 提供统一的批量处理抽象，支持：
 * </p>
 * <ul>
 *   <li>顺序执行和并行执行</li>
 *   <li>错误容忍配置</li>
 *   <li>进度回调</li>
 *   <li>重试机制</li>
 *   <li>限流控制</li>
 * </ul>
 *
 * <p>
 * 使用示例：
 * </p>
 * <pre>{@code
 * BatchOperationTemplate template = new BatchOperationTemplate();
 *
 * // 顺序执行
 * BatchResult<String> result = template.execute(items, (item, index) -> process(item));
 *
 * // 并行执行
 * BatchResult<String> result = template.executeParallel(items, operation, 4);
 *
 * // 带重试执行
 * BatchResult<String> result = template.executeWithRetry(items, operation, retryPolicy);
 * }</pre>
 */
@SuppressWarnings({"PMD.GodClass", "PMD.AvoidCatchingGenericException"})
public class BatchOperationTemplate {

    private static final Logger log = LoggerFactory.getLogger(BatchOperationTemplate.class);

    private final BatchProperties properties;

    /**
     * 使用默认配置创建模板
     */
    public BatchOperationTemplate() {
        this(new BatchProperties());
    }

    /**
     * 使用指定配置创建模板
     *
     * @param properties 批量操作配置
     */
    public BatchOperationTemplate(@NonNull BatchProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 顺序执行批量操作
     *
     * @param items     待处理的元素列表
     * @param operation 批量操作
     * @param <T>       元素类型
     * @param <R>       结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> execute(@NonNull List<T> items, @NonNull BatchOperation<T, R> operation) {
        return execute(items, operation, null);
    }

    /**
     * 顺序执行批量操作（带进度回调）
     *
     * @param items     待处理的元素列表
     * @param operation 批量操作
     * @param callback  进度回调（可选）
     * @param <T>       元素类型
     * @param <R>       结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> execute(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            @Nullable BatchProgressCallback<T, R> callback) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        if (items.isEmpty()) {
            return BatchResult.empty();
        }

        Instant start = Instant.now();
        Builder<R> builder = BatchResult.<R>builder().total(items.size());
        Semaphore rateLimiter = createRateLimiter();

        for (int i = 0; i < items.size(); i++) {
            // 检查是否应该停止
            if (shouldStop(builder, items.size())) {
                break;
            }

            T item = items.get(i);
            acquirePermit(rateLimiter);

            try {
                R result = executeWithRetryIfNeeded(operation, item, i);
                builder.addResult(result);
                notifyItemComplete(callback, item, i, result, true);
            } catch (Exception e) {
                handleFailure(builder, i, item, e);
                notifyItemComplete(callback, item, i, null, false);

                if (properties.isStopOnError()) {
                    break;
                }
            }

            notifyProgress(callback, i + 1, items.size());
        }

        BatchResult<R> result = finalizeResult(builder, start);
        notifyComplete(callback, result);
        return result;
    }

    /**
     * 并行执行批量操作
     *
     * @param items       待处理的元素列表
     * @param operation   批量操作
     * @param parallelism 并行度
     * @param <T>         元素类型
     * @param <R>         结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeParallel(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int parallelism) {
        return executeParallel(items, operation, parallelism, null);
    }

    /**
     * 并行执行批量操作（带进度回调）
     *
     * @param items       待处理的元素列表
     * @param operation   批量操作
     * @param parallelism 并行度
     * @param callback    进度回调（可选）
     * @param <T>         元素类型
     * @param <R>         结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeParallel(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int parallelism,
            @Nullable BatchProgressCallback<T, R> callback) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        if (items.isEmpty()) {
            return BatchResult.empty();
        }

        int actualParallelism = parallelism > 0 ? parallelism : properties.getActualParallelism();
        Instant start = Instant.now();
        Builder<R> builder = BatchResult.<R>builder().total(items.size());

        // 使用虚拟线程执行器（Java 25+）
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ItemResult<R>>> futures = new ArrayList<>();
            Semaphore rateLimiter = createRateLimiter();
            AtomicInteger completedCount = new AtomicInteger(0);
            ConcurrentHashMap<Integer, ItemResult<R>> results = new ConcurrentHashMap<>();

            // 提交所有任务
            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                final T item = items.get(i);

                Future<ItemResult<R>> future = executor.submit(() -> {
                    acquirePermit(rateLimiter);
                    try {
                        R result = executeWithRetryIfNeeded(operation, item, index);
                        return new ItemResult<>(index, item, result, null);
                    } catch (Exception e) {
                        return new ItemResult<>(index, item, null, e);
                    }
                });
                futures.add(future);
            }

            // 收集结果
            for (Future<ItemResult<R>> future : futures) {
                try {
                    ItemResult<R> itemResult = future.get();
                    results.put(itemResult.index(), itemResult);
                    int completed = completedCount.incrementAndGet();
                    notifyProgress(callback, completed, items.size());
                } catch (Exception e) {
                    log.warn("Failed to get batch operation result", e);
                }
            }

            // 按顺序处理结果
            for (int i = 0; i < items.size(); i++) {
                ItemResult<R> itemResult = results.get(i);
                if (itemResult == null) {
                    continue;
                }

                if (itemResult.error() != null) {
                    handleFailure(builder, i, items.get(i), itemResult.error());
                    notifyItemComplete(callback, items.get(i), i, null, false);
                } else {
                    builder.addResult(itemResult.result());
                    notifyItemComplete(callback, items.get(i), i, itemResult.result(), true);
                }
            }
        }

        BatchResult<R> result = finalizeResult(builder, start);
        notifyComplete(callback, result);
        return result;
    }

    /**
     * 带重试策略执行批量操作
     *
     * @param items       待处理的元素列表
     * @param operation   批量操作
     * @param maxAttempts 最大重试次数
     * @param <T>         元素类型
     * @param <R>         结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeWithRetry(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int maxAttempts) {
        return executeWithRetry(items, operation, maxAttempts, null);
    }

    /**
     * 带重试策略执行批量操作（带进度回调）
     *
     * @param items       待处理的元素列表
     * @param operation   批量操作
     * @param maxAttempts 最大重试次数
     * @param callback    进度回调（可选）
     * @param <T>         元素类型
     * @param <R>         结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeWithRetry(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int maxAttempts,
            @Nullable BatchProgressCallback<T, R> callback) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        if (items.isEmpty()) {
            return BatchResult.empty();
        }

        int actualMaxAttempts = maxAttempts > 0 ? maxAttempts : properties.getRetry().getMaxAttempts();
        long initialInterval = properties.getRetry().getInitialInterval();
        double multiplier = properties.getRetry().getMultiplier();
        long maxInterval = properties.getRetry().getMaxInterval();

        Instant start = Instant.now();
        Builder<R> builder = BatchResult.<R>builder().total(items.size());
        Semaphore rateLimiter = createRateLimiter();

        for (int i = 0; i < items.size(); i++) {
            if (shouldStop(builder, items.size())) {
                break;
            }

            T item = items.get(i);
            acquirePermit(rateLimiter);

            R result = null;
            Exception lastException = null;
            boolean success = false;

            for (int attempt = 1; attempt <= actualMaxAttempts; attempt++) {
                try {
                    result = operation.execute(item, i);
                    success = true;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < actualMaxAttempts) {
                        long waitTime = calculateWaitTime(attempt, initialInterval, multiplier, maxInterval);
                        log.debug("Retry attempt {}/{} for item at index {} after {}ms",
                                attempt, actualMaxAttempts, i, waitTime);
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (success) {
                builder.addResult(result);
                notifyItemComplete(callback, item, i, result, true);
            } else {
                handleFailure(builder, i, item, lastException);
                notifyItemComplete(callback, item, i, null, false);

                if (properties.isStopOnError()) {
                    break;
                }
            }

            notifyProgress(callback, i + 1, items.size());
        }

        BatchResult<R> finalResult = finalizeResult(builder, start);
        notifyComplete(callback, finalResult);
        return finalResult;
    }

    /**
     * 分批执行批量操作
     * <p>
     * 将大量数据分批次处理，避免内存溢出
     * </p>
     *
     * @param items     待处理的元素列表
     * @param operation 批量操作
     * @param batchSize 批次大小
     * @param <T>       元素类型
     * @param <R>       结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeInBatches(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int batchSize) {
        return executeInBatches(items, operation, batchSize, null);
    }

    /**
     * 分批执行批量操作（带进度回调）
     *
     * @param items     待处理的元素列表
     * @param operation 批量操作
     * @param batchSize 批次大小
     * @param callback  进度回调（可选）
     * @param <T>       元素类型
     * @param <R>       结果类型
     * @return 批量处理结果
     */
    public <T, R> BatchResult<R> executeInBatches(
            @NonNull List<T> items,
            @NonNull BatchOperation<T, R> operation,
            int batchSize,
            @Nullable BatchProgressCallback<T, R> callback) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        if (items.isEmpty()) {
            return BatchResult.empty();
        }

        int actualBatchSize = batchSize > 0 ? batchSize : properties.getDefaultBatchSize();
        Instant start = Instant.now();
        Builder<R> builder = BatchResult.<R>builder().total(items.size());

        int totalBatches = (items.size() + actualBatchSize - 1) / actualBatchSize;
        int processedCount = 0;

        for (int batch = 0; batch < totalBatches; batch++) {
            int fromIndex = batch * actualBatchSize;
            int toIndex = Math.min(fromIndex + actualBatchSize, items.size());
            List<T> batchItems = items.subList(fromIndex, toIndex);

            log.debug("Processing batch {}/{}, items {}-{}", batch + 1, totalBatches, fromIndex, toIndex - 1);

            for (int i = 0; i < batchItems.size(); i++) {
                int globalIndex = fromIndex + i;
                T item = batchItems.get(i);

                if (shouldStop(builder, items.size())) {
                    break;
                }

                try {
                    R result = operation.execute(item, globalIndex);
                    builder.addResult(result);
                    notifyItemComplete(callback, item, globalIndex, result, true);
                } catch (Exception e) {
                    handleFailure(builder, globalIndex, item, e);
                    notifyItemComplete(callback, item, globalIndex, null, false);

                    if (properties.isStopOnError()) {
                        break;
                    }
                }

                processedCount++;
                notifyProgress(callback, processedCount, items.size());
            }

            if (shouldStop(builder, items.size()) || properties.isStopOnError() && builder.build().failed() > 0) {
                break;
            }
        }

        BatchResult<R> result = finalizeResult(builder, start);
        notifyComplete(callback, result);
        return result;
    }

    /**
     * 创建限流器
     */
    private Semaphore createRateLimiter() {
        if (!properties.getRateLimit().isEnabled()) {
            return null;
        }
        return new Semaphore(properties.getRateLimit().getPermitsPerSecond());
    }

    /**
     * 获取许可
     */
    private void acquirePermit(@Nullable Semaphore rateLimiter) {
        if (rateLimiter == null) {
            return;
        }

        long maxWait = properties.getRateLimit().getMaxWaitMillis();
        try {
            // maxWait < 0 视为无限等待，但设置上限为 30 秒防止永久阻塞
            if (maxWait < 0) {
                if (!rateLimiter.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new BatchOperationException("Rate limit exceeded, max wait time reached (30s)");
                }
            } else if (maxWait == 0) {
                if (!rateLimiter.tryAcquire()) {
                    throw new BatchOperationException("Rate limit exceeded");
                }
            } else {
                if (!rateLimiter.tryAcquire(maxWait, TimeUnit.MILLISECONDS)) {
                    throw new BatchOperationException("Rate limit exceeded, wait timeout");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BatchOperationException("Interrupted while acquiring rate limit permit", e);
        }
    }

    /**
     * 检查是否应该停止处理
     */
    private <R> boolean shouldStop(Builder<R> builder, int total) {
        if (total == 0) {
            return false;
        }

        double tolerance = properties.getErrorTolerance();
        if (tolerance >= 1.0) {
            return false;
        }

        BatchResult<R> current = builder.build();
        if (current.total() == 0) {
            return false;
        }

        double currentFailureRate = (double) current.errors().size() / current.total();
        return currentFailureRate > tolerance;
    }

    /**
     * 根据配置决定是否重试
     */
    private <T, R> R executeWithRetryIfNeeded(BatchOperation<T, R> operation, T item, int index) throws Exception {
        if (!properties.getRetry().isEnabled()) {
            return operation.execute(item, index);
        }

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long initialInterval = properties.getRetry().getInitialInterval();
        double multiplier = properties.getRetry().getMultiplier();
        long maxInterval = properties.getRetry().getMaxInterval();

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute(item, index);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    long waitTime = calculateWaitTime(attempt, initialInterval, multiplier, maxInterval);
                    Thread.sleep(waitTime);
                }
            }
        }

        throw lastException;
    }

    /**
     * 计算重试等待时间（指数退避）
     */
    private long calculateWaitTime(int attempt, long initialInterval, double multiplier, long maxInterval) {
        long interval = (long) (initialInterval * Math.pow(multiplier, attempt - 1));
        return Math.min(interval, maxInterval);
    }

    /**
     * 处理失败情况
     */
    private <R> void handleFailure(Builder<R> builder, int index, Object item, @Nullable Exception e) {
        String itemStr = item != null ? item.toString() : null;
        String error = e != null ? e.getMessage() : "Unknown error";
        builder.addError(BatchError.of(index, itemStr, error, e));
        log.debug("Batch operation failed at index {}: {}", index, error, e);
    }

    /**
     * 通知单个元素处理完成
     */
    private <T, R> void notifyItemComplete(
            @Nullable BatchProgressCallback<T, R> callback,
            T item, int index, @Nullable R result, boolean success) {
        if (callback != null) {
            try {
                callback.onItemComplete(item, index, result, success);
            } catch (Exception e) {
                log.warn("Progress callback onItemComplete failed", e);
            }
        }
    }

    /**
     * 通知进度更新
     */
    private <T, R> void notifyProgress(
            @Nullable BatchProgressCallback<T, R> callback,
            int completed, int total) {
        if (callback != null) {
            try {
                callback.onBatchProgress(completed, total);
            } catch (Exception e) {
                log.warn("Progress callback onBatchProgress failed", e);
            }
        }
    }

    /**
     * 通知处理完成
     */
    private <T, R> void notifyComplete(@Nullable BatchProgressCallback<T, R> callback, BatchResult<R> result) {
        if (callback != null) {
            try {
                callback.onComplete(result);
            } catch (Exception e) {
                log.warn("Progress callback onComplete failed", e);
            }
        }
    }

    /**
     * 完成结果构建
     */
    private <R> BatchResult<R> finalizeResult(Builder<R> builder, Instant start) {
        int success = builder.build().results().size();
        int errors = builder.build().errors().size();
        return builder
                .success(success)
                .failed(errors)
                .duration(Duration.between(start, Instant.now()))
                .build();
    }

    /**
     * 单个元素执行结果（内部使用）
     */
    private record ItemResult<R>(int index, Object item, @Nullable R result, @Nullable Exception error) {}
}
