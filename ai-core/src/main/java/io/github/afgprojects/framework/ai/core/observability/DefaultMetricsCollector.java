package io.github.afgprojects.framework.ai.core.observability;

import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 默认指标收集器实现
 *
 * <p>基于内存的简单指标收集器，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>不需要持久化的场景</li>
 *   <li>轻量级监控</li>
 * </ul>
 *
 * <p>生产环境建议使用 Micrometer 或 Prometheus 实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final LongAdder totalResponseTimeMs = new LongAdder();
    private final LongAdder totalInputTokens = new LongAdder();
    private final LongAdder totalOutputTokens = new LongAdder();
    private final LongAdder totalCost = new LongAdder();

    private final ConcurrentHashMap<String, ModelMetrics> modelMetrics = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Timer startTimer(@NonNull String operationType, @NonNull String modelName, @NonNull Map<String, String> tags) {
        return new DefaultTimer(operationType, modelName, tags, this);
    }

    @Override
    public void recordCount(
            @NonNull String operationType,
            @NonNull String modelName,
            @NonNull String status,
            @NonNull Map<String, String> tags
    ) {
        totalRequests.incrementAndGet();

        if ("success".equals(status)) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }

        modelMetrics.computeIfAbsent(modelName, ModelMetrics::new)
                .recordRequest(status);
    }

    @Override
    public void recordTokenUsage(
            @NonNull String modelName,
            long inputTokens,
            long outputTokens,
            @NonNull Map<String, String> tags
    ) {
        totalInputTokens.add(inputTokens);
        totalOutputTokens.add(outputTokens);

        modelMetrics.computeIfAbsent(modelName, ModelMetrics::new)
                .recordTokens(inputTokens, outputTokens);
    }

    @Override
    public void recordCost(@NonNull String modelName, double cost, @NonNull Map<String, String> tags) {
        totalCost.add((long) (cost * 1000000)); // 使用微美元避免精度问题

        modelMetrics.computeIfAbsent(modelName, ModelMetrics::new)
                .recordCost(cost);
    }

    @Override
    public void recordResponseSize(
            @NonNull String operationType,
            @NonNull String modelName,
            long sizeBytes,
            @NonNull Map<String, String> tags
    ) {
        // 简化实现，暂不记录响应大小
    }

    @Override
    @NonNull
    public MetricsSummary getSummary() {
        return new DefaultMetricsSummary();
    }

    /**
     * 记录响应时间
     */
    void recordResponseTime(String modelName, long responseTimeMs) {
        totalResponseTimeMs.add(responseTimeMs);

        modelMetrics.computeIfAbsent(modelName, ModelMetrics::new)
                .recordResponseTime(responseTimeMs);
    }

    /**
     * 默认计时器实现
     */
    private static class DefaultTimer implements Timer {

        private final String operationType;
        private final String modelName;
        private final Map<String, String> tags;
        private final DefaultMetricsCollector collector;
        private final long startTimeMs;
        private volatile boolean stopped = false;

        DefaultTimer(String operationType, String modelName, Map<String, String> tags, DefaultMetricsCollector collector) {
            this.operationType = operationType;
            this.modelName = modelName;
            this.tags = tags;
            this.collector = collector;
            this.startTimeMs = System.currentTimeMillis();
        }

        @Override
        public void stop(@NonNull String status) {
            stop(status, Map.of());
        }

        @Override
        public void stop(@NonNull String status, @NonNull Map<String, String> additionalTags) {
            if (stopped) {
                return;
            }
            stopped = true;

            long responseTimeMs = System.currentTimeMillis() - startTimeMs;
            collector.recordResponseTime(modelName, responseTimeMs);
            collector.recordCount(operationType, modelName, status, tags);
        }

        @Override
        @NonNull
        public Duration getElapsed() {
            return Duration.ofMillis(System.currentTimeMillis() - startTimeMs);
        }

        @Override
        public long getStartTimeMs() {
            return startTimeMs;
        }
    }

    /**
     * 默认指标摘要实现
     */
    private class DefaultMetricsSummary implements MetricsSummary {

        @Override
        public long getTotalRequests() {
            return totalRequests.get();
        }

        @Override
        public long getSuccessRequests() {
            return successRequests.get();
        }

        @Override
        public long getFailedRequests() {
            return failedRequests.get();
        }

        @Override
        @NonNull
        public Duration getAverageResponseTime() {
            long total = totalRequests.get();
            if (total == 0) {
                return Duration.ZERO;
            }
            return Duration.ofMillis(totalResponseTimeMs.sum() / total);
        }

        @Override
        public long getTotalTokens() {
            return totalInputTokens.sum() + totalOutputTokens.sum();
        }

        @Override
        public double getTotalCost() {
            return totalCost.sum() / 1000000.0;
        }

        @Override
        @NonNull
        public Map<String, ModelStats> getModelStats() {
            Map<String, ModelStats> result = new ConcurrentHashMap<>();
            modelMetrics.forEach((name, metrics) -> result.put(name, metrics.toStats()));
            return result;
        }
    }

    /**
     * 模型指标
     */
    private static class ModelMetrics {

        private final String modelName;
        private final LongAdder requestCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder responseTimeMs = new LongAdder();
        private final LongAdder inputTokens = new LongAdder();
        private final LongAdder outputTokens = new LongAdder();
        private final LongAdder cost = new LongAdder();

        ModelMetrics(String modelName) {
            this.modelName = modelName;
        }

        void recordRequest(String status) {
            requestCount.increment();
            if ("success".equals(status)) {
                successCount.increment();
            } else {
                failureCount.increment();
            }
        }

        void recordResponseTime(long timeMs) {
            responseTimeMs.add(timeMs);
        }

        void recordTokens(long input, long output) {
            inputTokens.add(input);
            outputTokens.add(output);
        }

        void recordCost(double c) {
            cost.add((long) (c * 1000000));
        }

        ModelStats toStats() {
            return new DefaultModelStats(modelName, requestCount.sum(), responseTimeMs.sum(),
                    inputTokens.sum(), outputTokens.sum(), cost.sum() / 1000000.0);
        }
    }

    /**
     * 默认模型统计实现
     */
    private static class DefaultModelStats implements ModelStats {

        private final String modelName;
        private final long requestCount;
        private final long responseTimeMs;
        private final long inputTokens;
        private final long outputTokens;
        private final double cost;

        DefaultModelStats(String modelName, long requestCount, long responseTimeMs,
                          long inputTokens, long outputTokens, double cost) {
            this.modelName = modelName;
            this.requestCount = requestCount;
            this.responseTimeMs = responseTimeMs;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.cost = cost;
        }

        @Override
        @NonNull
        public String getModelName() {
            return modelName;
        }

        @Override
        public long getRequestCount() {
            return requestCount;
        }

        @Override
        @NonNull
        public Duration getAverageResponseTime() {
            if (requestCount == 0) {
                return Duration.ZERO;
            }
            return Duration.ofMillis(responseTimeMs / requestCount);
        }

        @Override
        public long getTotalInputTokens() {
            return inputTokens;
        }

        @Override
        public long getTotalOutputTokens() {
            return outputTokens;
        }

        @Override
        public double getTotalCost() {
            return cost;
        }
    }
}
