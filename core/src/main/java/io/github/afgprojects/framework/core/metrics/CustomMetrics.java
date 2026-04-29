package io.github.afgprojects.framework.core.metrics;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.autoconfigure.MetricsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * 自定义指标注册器
 *
 * <p>提供便捷的自定义指标注册方法
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 注册计数器
 * Counter counter = customMetrics.counter("orders.created", "type", "online");
 * counter.increment();
 *
 * // 注册 Timer
 * Timer.Sample sample = customMetrics.startTimer();
 * // ... 执行业务逻辑
 * customMetrics.stopTimer(sample, "api.latency", "endpoint", "/users");
 *
 * // 注册 Gauge
 * customMetrics.gauge("queue.size", queue, Queue::size);
 * </pre>
 *
 * @since 1.0.0
 */
public class CustomMetrics {

    private final MeterRegistry meterRegistry;
    private final MetricsProperties properties;

    public CustomMetrics(MeterRegistry meterRegistry, MetricsProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        registerConfiguredMetrics();
    }

    /**
     * 注册配置文件中定义的指标
     */
    private void registerConfiguredMetrics() {
        if (!properties.getCustom().isEnabled()) {
            return;
        }

        for (MetricsProperties.CounterConfig config : properties.getCustom().getCounters()) {
            counter(config.getName(), config.getDescription(), config.getTags());
        }
    }

    /**
     * 创建或获取计数器
     *
     * @param name 指标名称
     * @return 计数器
     */
    @NonNull
    public Counter counter(@NonNull String name) {
        return Counter.builder(name).register(meterRegistry);
    }

    /**
     * 创建或获取带标签的计数器
     *
     * @param name  指标名称
     * @param tags  标签键值对（key1, value1, key2, value2...）
     * @return 计数器
     */
    @NonNull
    public Counter counter(@NonNull String name, @NonNull String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }

    /**
     * 创建或获取带描述和标签的计数器
     *
     * @param name        指标名称
     * @param description 描述
     * @param tags        标签映射
     * @return 计数器
     */
    @NonNull
    public Counter counter(@NonNull String name, @Nullable String description, @Nullable Map<String, String> tags) {
        Counter.Builder builder = Counter.builder(name);
        if (description != null) {
            builder.description(description);
        }
        if (tags != null && !tags.isEmpty()) {
            builder.tags(Tags.of(tags.entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .toList()));
        }
        return builder.register(meterRegistry);
    }

    /**
     * 创建 Timer
     *
     * @param name 指标名称
     * @return Timer
     */
    @NonNull
    public Timer timer(@NonNull String name) {
        return Timer.builder(name)
                .publishPercentiles(properties.getHistogram().getPercentiles())
                .publishPercentileHistogram(properties.getHistogram().isPercentileHistogram())
                .register(meterRegistry);
    }

    /**
     * 创建带标签的 Timer
     *
     * @param name 指标名称
     * @param tags 标签键值对
     * @return Timer
     */
    @NonNull
    public Timer timer(@NonNull String name, @NonNull String... tags) {
        return Timer.builder(name)
                .tags(tags)
                .publishPercentiles(properties.getHistogram().getPercentiles())
                .publishPercentileHistogram(properties.getHistogram().isPercentileHistogram())
                .register(meterRegistry);
    }

    /**
     * 开始计时
     *
     * @return Timer.Sample
     */
    public Timer.@NonNull Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止计时并记录
     *
     * @param sample Timer.Sample
     * @param name   指标名称
     * @param tags   标签键值对
     */
    public void stopTimer(Timer.@NonNull Sample sample, @NonNull String name, @NonNull String... tags) {
        sample.stop(timer(name, tags));
    }

    /**
     * 注册 Gauge
     *
     * @param name   指标名称
     * @param obj    被观察对象
     * @param value  值函数
     * @param <T>    对象类型
     * @return Gauge
     */
    @NonNull
    public <T> Gauge gauge(@NonNull String name, @NonNull T obj, java.util.function.@NonNull ToDoubleFunction<T> value) {
        return Gauge.builder(name, obj, value).register(meterRegistry);
    }

    /**
     * 注册带标签的 Gauge
     *
     * @param name   指标名称
     * @param obj    被观察对象
     * @param value  值函数
     * @param tags   标签键值对
     * @param <T>    对象类型
     * @return Gauge
     */
    @NonNull
    public <T> Gauge gauge(@NonNull String name, @NonNull T obj,
                           java.util.function.@NonNull ToDoubleFunction<T> value,
                           @NonNull String... tags) {
        return Gauge.builder(name, obj, value).tags(tags).register(meterRegistry);
    }

    /**
     * 增加计数器值
     *
     * @param name  指标名称
     * @param delta 增量
     */
    public void increment(@NonNull String name, double delta) {
        counter(name).increment(delta);
    }

    /**
     * 增加计数器值（带标签）
     *
     * @param name  指标名称
     * @param delta 增量
     * @param tags  标签键值对
     */
    public void increment(@NonNull String name, double delta, @NonNull String... tags) {
        counter(name, tags).increment(delta);
    }

    /**
     * 记录执行时间
     *
     * @param name     指标名称
     * @param duration 执行时间
     * @param tags     标签键值对
     */
    public void record(@NonNull String name, java.time.@NonNull Duration duration, @NonNull String... tags) {
        timer(name, tags).record(duration);
    }

    /**
     * 记录 Runnable 执行时间
     *
     * @param name     指标名称
     * @param runnable 要执行的任务
     * @param tags     标签键值对
     */
    public void record(@NonNull String name, @NonNull Runnable runnable, @NonNull String... tags) {
        timer(name, tags).record(runnable);
    }

    /**
     * 记录 Callable 执行时间并返回结果
     *
     * @param name     指标名称
     * @param callable 要执行的任务
     * @param tags     标签键值对
     * @param <T>      返回类型
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public <T> T record(@NonNull String name, java.util.concurrent.@NonNull Callable<T> callable,
                        @NonNull String... tags) throws Exception {
        return timer(name, tags).recordCallable(callable);
    }

    /**
     * 获取 MeterRegistry
     *
     * @return MeterRegistry
     */
    @NonNull
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}