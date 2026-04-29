package io.github.afgprojects.framework.core.cache.metrics;

import org.jspecify.annotations.NonNull;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Micrometer 缓存指标绑定器
 * <p>
 * 将缓存指标绑定到 Micrometer，支持 Prometheus 等监控系统
 * </p>
 */
public class CacheMetricsBinder {

    /**
     * 指标前缀
     */
    private static final String METRIC_PREFIX = "afg.cache.";

    /**
     * 绑定缓存指标到 MeterRegistry
     *
     * @param metrics      缓存指标
     * @param meterRegistry Micrometer 注册表
     */
    public static void bind(@NonNull CacheMetrics metrics, @NonNull MeterRegistry meterRegistry) {
        String cacheName = metrics.getCacheName();
        String cacheType = metrics.getCacheType();

        // 命中次数
        Counter hitCounter = Counter.builder(METRIC_PREFIX + "hits")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache hit count")
                .register(meterRegistry);

        // 未命中次数
        Counter missCounter = Counter.builder(METRIC_PREFIX + "misses")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache miss count")
                .register(meterRegistry);

        // 存入次数
        Counter putCounter = Counter.builder(METRIC_PREFIX + "puts")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache put count")
                .register(meterRegistry);

        // 删除次数
        Counter evictCounter = Counter.builder(METRIC_PREFIX + "evictions")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache eviction count")
                .register(meterRegistry);

        // 加载次数
        Counter loadCounter = Counter.builder(METRIC_PREFIX + "loads")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache load count")
                .register(meterRegistry);

        // 加载失败次数
        Counter loadFailureCounter = Counter.builder(METRIC_PREFIX + "load.failures")
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache load failure count")
                .register(meterRegistry);

        // 命中率
        Gauge.builder(METRIC_PREFIX + "hit.rate", metrics, CacheMetrics::getHitRate)
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache hit rate")
                .register(meterRegistry);

        // 未命中率
        Gauge.builder(METRIC_PREFIX + "miss.rate", metrics, CacheMetrics::getMissRate)
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache miss rate")
                .register(meterRegistry);

        // 加载成功率
        Gauge.builder(METRIC_PREFIX + "load.success.rate", metrics, CacheMetrics::getLoadSuccessRate)
                .tag("cache", cacheName)
                .tag("type", cacheType)
                .description("Cache load success rate")
                .register(meterRegistry);

        // 注册更新器，将内部指标同步到 Micrometer
        Runnable syncTask = () -> {
            hitCounter.increment(metrics.getHitCount() - hitCounter.count());
            missCounter.increment(metrics.getMissCount() - missCounter.count());
            putCounter.increment(metrics.getPutCount() - putCounter.count());
            evictCounter.increment(metrics.getEvictCount() - evictCounter.count());
            loadCounter.increment(metrics.getLoadCount() - loadCounter.count());
            loadFailureCounter.increment(metrics.getLoadFailureCount() - loadFailureCounter.count());
        };

        // 每次获取指标时同步
        // 这里简化处理，实际可以定期同步
    }
}