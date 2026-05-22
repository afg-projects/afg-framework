package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI Observation 适配器
 *
 * <p>将 Spring AI 的 Micrometer Observation 转换为框架的 Tracer/MetricsCollector 接口。
 * 实现与 Spring AI 内置观测能力的无缝集成。
 *
 * <p>功能特性：
 * <ul>
 *   <li>Span 追踪：通过 ObservationRegistry 创建和管理 Span</li>
 *   <li>指标收集：通过 MeterRegistry 记录 AI 操作指标</li>
 *   <li>上下文传播：支持跨服务的追踪上下文传递</li>
 *   <li>Spring AI 集成：与 Spring AI 的 Observation 自动配置协同工作</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiObservationAdapter implements Tracer, MetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(SpringAiObservationAdapter.class);

    /**
     * AI 操作的观测约定名称前缀
     */
    private static final String AI_OBSERVATION_PREFIX = "afg.ai";

    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    private final AtomicReference<Span> currentSpan = new AtomicReference<>();

    /**
     * 创建适配器
     *
     * @param observationRegistry Observation 注册表
     * @param meterRegistry       Meter 注册表
     */
    public SpringAiObservationAdapter(ObservationRegistry observationRegistry,
                                      @Nullable MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
        log.info("Spring AI Observation adapter initialized");
    }

    // ==================== Tracer 接口实现 ====================

    @Override
    @NonNull
    public Span startSpan(@NonNull String operationName) {
        return startSpan(operationName, (Span) null);
    }

    @Override
    @NonNull
    public Span startSpan(@NonNull String operationName, @Nullable Span parentSpan) {
        Observation parentObservation = null;
        String parentTraceId = null;
        String parentSpanId = null;

        if (parentSpan instanceof ObservationSpan observationSpan) {
            parentObservation = observationSpan.getObservation();
            parentTraceId = observationSpan.getTraceId();
            parentSpanId = observationSpan.getSpanId();
        }

        String observationName = AI_OBSERVATION_PREFIX + "." + operationName;
        Observation observation = Observation.createNotStarted(observationName, observationRegistry);

        if (parentObservation != null) {
            observation = observation.parentObservation(parentObservation);
        }

        observation.start();

        // 创建带有父 Span 上下文的 Span
        ObservationSpan span;
        if (parentTraceId != null && parentSpanId != null) {
            span = new ObservationSpan(operationName, observation, parentTraceId, parentSpanId);
        } else {
            span = new ObservationSpan(operationName, observation);
        }

        // 设置父 Span 的上下文
        if (parentSpan instanceof ObservationSpan observationParentSpan) {
            observationParentSpan.getContext().getAllBaggage().forEach(
                    (k, v) -> span.getContext().setBaggage(k, v)
            );
        }

        currentSpan.set(span);
        log.debug("Started observation span: {} (observationName={})", operationName, observationName);

        return span;
    }

    @Override
    @NonNull
    public Span startSpanWithContext(@NonNull String operationName, @NonNull TraceContext context) {
        String observationName = AI_OBSERVATION_PREFIX + "." + operationName;
        Observation observation = Observation.createNotStarted(observationName, observationRegistry);

        observation.start();

        ObservationSpan span = new ObservationSpan(operationName, observation, context);
        context.getAllBaggage().forEach((k, v) -> span.getContext().setBaggage(k, v));

        currentSpan.set(span);
        log.debug("Started observation span with context: {} (traceId={})", operationName, context.getTraceId());

        return span;
    }

    @Override
    @Nullable
    public Span getCurrentSpan() {
        return currentSpan.get();
    }

    @Override
    @NonNull
    public TraceContext extractContext(@NonNull Map<String, String> headers) {
        String traceId = headers.getOrDefault("X-Trace-Id", generateTraceId());
        String spanId = headers.getOrDefault("X-Span-Id", generateSpanId());
        String parentSpanId = headers.get("X-Parent-Span-Id");

        ObservationTraceContext context = new ObservationTraceContext(traceId, spanId, parentSpanId);

        // 提取 baggage
        headers.entrySet().stream()
                .filter(e -> e.getKey().startsWith("X-Baggage-"))
                .forEach(e -> context.setBaggage(
                        e.getKey().substring("X-Baggage-".length()),
                        e.getValue()
                ));

        return context;
    }

    @Override
    @NonNull
    public Map<String, String> injectContext(@NonNull TraceContext context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Trace-Id", context.getTraceId());
        headers.put("X-Span-Id", context.getSpanId());

        if (context.getParentSpanId() != null) {
            headers.put("X-Parent-Span-Id", context.getParentSpanId());
        }

        context.getAllBaggage().forEach((k, v) -> headers.put("X-Baggage-" + k, v));

        return headers;
    }

    // ==================== MetricsCollector 接口实现 ====================

    @Override
    @NonNull
    public Timer startTimer(@NonNull String operationType, @NonNull String modelName, @NonNull Map<String, String> tags) {
        return new ObservationTimer(operationType, modelName, tags, this);
    }

    @Override
    public void recordCount(@NonNull String operationType, @NonNull String modelName,
                            @NonNull String status, @NonNull Map<String, String> tags) {
        if (meterRegistry == null) {
            return;
        }

        String meterName = AI_OBSERVATION_PREFIX + "." + operationType + ".requests";
        Tags meterTags = buildTags(modelName, status, tags);

        meterRegistry.counter(meterName, meterTags).increment();
        log.debug("Recorded count: {} (model={}, status={})", meterName, modelName, status);
    }

    @Override
    public void recordTokenUsage(@NonNull String modelName, long inputTokens, long outputTokens,
                                 @NonNull Map<String, String> tags) {
        if (meterRegistry == null) {
            return;
        }

        String inputMeterName = AI_OBSERVATION_PREFIX + ".tokens.input";
        String outputMeterName = AI_OBSERVATION_PREFIX + ".tokens.output";

        Tags meterTags = buildTags(modelName, null, tags);

        meterRegistry.counter(inputMeterName, meterTags).increment(inputTokens);
        meterRegistry.counter(outputMeterName, meterTags).increment(outputTokens);

        log.debug("Recorded token usage: model={}, input={}, output={}", modelName, inputTokens, outputTokens);
    }

    @Override
    public void recordCost(@NonNull String modelName, double cost, @NonNull Map<String, String> tags) {
        if (meterRegistry == null) {
            return;
        }

        String meterName = AI_OBSERVATION_PREFIX + ".cost";
        Tags meterTags = buildTags(modelName, null, tags);

        meterRegistry.counter(meterName, meterTags).increment(cost);

        log.debug("Recorded cost: model={}, cost={}", modelName, cost);
    }

    @Override
    public void recordResponseSize(@NonNull String operationType, @NonNull String modelName,
                                   long sizeBytes, @NonNull Map<String, String> tags) {
        if (meterRegistry == null) {
            return;
        }

        String meterName = AI_OBSERVATION_PREFIX + "." + operationType + ".response.size";
        Tags meterTags = buildTags(modelName, null, tags);

        meterRegistry.summary(meterName, meterTags).record(sizeBytes);

        log.debug("Recorded response size: {} bytes", sizeBytes);
    }

    @Override
    @NonNull
    public MetricsSummary getSummary() {
        return new ObservationMetricsSummary();
    }

    /**
     * 构建 Micrometer Tags
     */
    private Tags buildTags(String modelName, @Nullable String status, Map<String, String> additionalTags) {
        List<Tag> tagList = new ArrayList<>();
        tagList.add(Tag.of("model", modelName));
        if (status != null) {
            tagList.add(Tag.of("status", status));
        }
        additionalTags.forEach((k, v) -> tagList.add(Tag.of(k, v)));
        return Tags.of(tagList);
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String generateSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 基于 Observation 的 Span 实现
     */
    private class ObservationSpan implements Span {

        private final String operationName;
        private final Observation observation;
        private final ObservationTraceContext context;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final long startTimeMs;

        private volatile SpanStatus status = SpanStatus.UNSET;
        private volatile boolean ended = false;

        ObservationSpan(String operationName, Observation observation) {
            this(operationName, observation, null, null);
        }

        ObservationSpan(String operationName, Observation observation, @Nullable TraceContext parentContext) {
            this(operationName, observation,
                    parentContext != null ? parentContext.getTraceId() : null,
                    parentContext != null ? parentContext.getSpanId() : null);
        }

        ObservationSpan(String operationName, Observation observation, @Nullable String parentTraceId, @Nullable String parentSpanId) {
            this.operationName = operationName;
            this.observation = observation;
            if (parentTraceId != null && parentSpanId != null) {
                this.context = new ObservationTraceContext(parentTraceId, generateSpanId(), parentSpanId);
            } else {
                this.context = new ObservationTraceContext(generateTraceId(), generateSpanId(), null);
            }
            this.startTimeMs = System.currentTimeMillis();
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, @NonNull String value) {
            attributes.put(key, value);
            observation.lowCardinalityKeyValue(key, value);
            return this;
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, long value) {
            attributes.put(key, value);
            observation.lowCardinalityKeyValue(key, String.valueOf(value));
            return this;
        }

        @Override
        @NonNull
        public Span setAttribute(@NonNull String key, boolean value) {
            attributes.put(key, value);
            observation.lowCardinalityKeyValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public void recordEvent(@NonNull String name) {
            recordEvent(name, Map.of());
        }

        @Override
        public void recordEvent(@NonNull String name, @NonNull Map<String, String> eventAttributes) {
            observation.event(Observation.Event.of(name));
            eventAttributes.forEach((k, v) -> observation.highCardinalityKeyValue(k, v));
            log.debug("Event recorded on span {}: {}", operationName, name);
        }

        @Override
        public void recordException(@NonNull Exception exception) {
            setAttribute("error.type", exception.getClass().getName());
            setAttribute("error.message", exception.getMessage());
            setStatus(SpanStatus.ERROR);
            observation.error(exception);
            log.debug("Exception recorded on span {}: {}", operationName, exception.getMessage());
        }

        @Override
        public void setStatus(@NonNull SpanStatus spanStatus) {
            this.status = spanStatus;
            if (spanStatus == SpanStatus.ERROR) {
                observation.error(new RuntimeException("Span marked as error"));
            }
        }

        @Override
        public void end() {
            end(status);
        }

        @Override
        public void end(@NonNull SpanStatus endStatus) {
            if (ended) {
                return;
            }
            ended = true;
            this.status = endStatus;

            long durationMs = System.currentTimeMillis() - startTimeMs;
            observation.lowCardinalityKeyValue("duration_ms", String.valueOf(durationMs));
            observation.lowCardinalityKeyValue("status", endStatus.name().toLowerCase());

            observation.stop();

            log.debug("Ended observation span: {} (status={}, duration={}ms)",
                    operationName, endStatus, durationMs);

            // 清除当前 span
            currentSpan.compareAndSet(this, null);
        }

        @Override
        @NonNull
        public String getSpanId() {
            return context.getSpanId();
        }

        @Override
        @NonNull
        public String getTraceId() {
            return context.getTraceId();
        }

        @Override
        @NonNull
        public String getOperationName() {
            return operationName;
        }

        @Override
        @NonNull
        public TraceContext getContext() {
            return context;
        }

        @Override
        public boolean isEnded() {
            return ended;
        }

        Observation getObservation() {
            return observation;
        }
    }

    /**
     * 追踪上下文实现
     */
    private static class ObservationTraceContext implements TraceContext {

        private final String traceId;
        private final String spanId;
        private final String parentSpanId;
        private final Map<String, String> baggage = new ConcurrentHashMap<>();

        ObservationTraceContext(String traceId, String spanId, @Nullable String parentSpanId) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
        }

        @Override
        @NonNull
        public String getTraceId() {
            return traceId;
        }

        @Override
        @NonNull
        public String getSpanId() {
            return spanId;
        }

        @Override
        @Nullable
        public String getParentSpanId() {
            return parentSpanId;
        }

        @Override
        @Nullable
        public String getBaggage(@NonNull String key) {
            return baggage.get(key);
        }

        @Override
        public void setBaggage(@NonNull String key, @NonNull String value) {
            baggage.put(key, value);
        }

        @Override
        @NonNull
        public Map<String, String> getAllBaggage() {
            return new HashMap<>(baggage);
        }
    }

    /**
     * 基于 Observation 的计时器实现
     */
    private class ObservationTimer implements Timer {

        private final String operationType;
        private final String modelName;
        private final Map<String, String> tags;
        private final SpringAiObservationAdapter collector;
        private final long startTimeMs;
        private volatile boolean stopped = false;

        ObservationTimer(String operationType, String modelName, Map<String, String> tags,
                         SpringAiObservationAdapter collector) {
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

            // 记录响应时间
            if (meterRegistry != null) {
                String timerName = AI_OBSERVATION_PREFIX + "." + operationType + ".duration";

                // 合并所有标签
                Map<String, String> allTags = new HashMap<>(tags);
                allTags.putAll(additionalTags);
                Tags meterTags = buildTags(modelName, status, allTags);

                meterRegistry.timer(timerName, meterTags).record(java.time.Duration.ofMillis(responseTimeMs));
            }

            // 记录计数
            Map<String, String> allTags = new HashMap<>(tags);
            allTags.putAll(additionalTags);
            collector.recordCount(operationType, modelName, status, allTags);
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
     * 指标摘要实现
     *
     * <p>注意：此实现返回空数据，因为 Micrometer 不直接提供聚合数据。
     * 实际指标应通过 Prometheus 或其他监控系统查询。
     */
    private static class ObservationMetricsSummary implements MetricsSummary {

        @Override
        public long getTotalRequests() {
            return 0;
        }

        @Override
        public long getSuccessRequests() {
            return 0;
        }

        @Override
        public long getFailedRequests() {
            return 0;
        }

        @Override
        @NonNull
        public Duration getAverageResponseTime() {
            return Duration.ZERO;
        }

        @Override
        public long getTotalTokens() {
            return 0;
        }

        @Override
        public double getTotalCost() {
            return 0;
        }

        @Override
        @NonNull
        public Map<String, ModelStats> getModelStats() {
            return Map.of();
        }
    }
}