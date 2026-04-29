package io.github.afgprojects.framework.core.cache.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.NonNull;

/**
 * 缓存指标
 * <p>
 * 记录缓存操作的统计数据，用于监控和分析缓存性能
 * </p>
 */
public class CacheMetrics {

    /**
     * 缓存名称
     */
    private final String cacheName;

    /**
     * 缓存类型（local/distributed/multi-level）
     */
    private final String cacheType;

    /**
     * 获取次数
     */
    private final AtomicLong getCount = new AtomicLong(0);

    /**
     * 命中次数
     */
    private final AtomicLong hitCount = new AtomicLong(0);

    /**
     * 未命中次数
     */
    private final AtomicLong missCount = new AtomicLong(0);

    /**
     * 存入次数
     */
    private final AtomicLong putCount = new AtomicLong(0);

    /**
     * 删除次数
     */
    private final AtomicLong evictCount = new AtomicLong(0);

    /**
     * 清空次数
     */
    private final AtomicLong clearCount = new AtomicLong(0);

    /**
     * 加载次数
     */
    private final AtomicLong loadCount = new AtomicLong(0);

    /**
     * 加载失败次数
     */
    private final AtomicLong loadFailureCount = new AtomicLong(0);

    /**
     * 构造缓存指标
     *
     * @param cacheName 缓存名称
     * @param cacheType 缓存类型
     */
    public CacheMetrics(@NonNull String cacheName, @NonNull String cacheType) {
        this.cacheName = cacheName;
        this.cacheType = cacheType;
    }

    /**
     * 记录获取操作
     */
    public void recordGet() {
        getCount.incrementAndGet();
    }

    /**
     * 记录命中
     */
    public void recordHit() {
        hitCount.incrementAndGet();
    }

    /**
     * 记录未命中
     */
    public void recordMiss() {
        missCount.incrementAndGet();
    }

    /**
     * 记录存入操作
     */
    public void recordPut() {
        putCount.incrementAndGet();
    }

    /**
     * 记录删除操作
     */
    public void recordEviction() {
        evictCount.incrementAndGet();
    }

    /**
     * 记录清空操作
     */
    public void recordClear() {
        clearCount.incrementAndGet();
    }

    /**
     * 记录加载操作
     */
    public void recordLoad() {
        loadCount.incrementAndGet();
    }

    /**
     * 记录加载失败
     */
    public void recordLoadFailure() {
        loadFailureCount.incrementAndGet();
    }

    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    @NonNull
    public String getCacheName() {
        return cacheName;
    }

    /**
     * 获取缓存类型
     *
     * @return 缓存类型
     */
    @NonNull
    public String getCacheType() {
        return cacheType;
    }

    /**
     * 获取获取次数
     *
     * @return 获取次数
     */
    public long getGetCount() {
        return getCount.get();
    }

    /**
     * 获取命中次数
     *
     * @return 命中次数
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * 获取未命中次数
     *
     * @return 未命中次数
     */
    public long getMissCount() {
        return missCount.get();
    }

    /**
     * 获取存入次数
     *
     * @return 存入次数
     */
    public long getPutCount() {
        return putCount.get();
    }

    /**
     * 获取删除次数
     *
     * @return 删除次数
     */
    public long getEvictCount() {
        return evictCount.get();
    }

    /**
     * 获取清空次数
     *
     * @return 清空次数
     */
    public long getClearCount() {
        return clearCount.get();
    }

    /**
     * 获取加载次数
     *
     * @return 加载次数
     */
    public long getLoadCount() {
        return loadCount.get();
    }

    /**
     * 获取加载失败次数
     *
     * @return 加载失败次数
     */
    public long getLoadFailureCount() {
        return loadFailureCount.get();
    }

    /**
     * 计算命中率
     *
     * @return 命中率（0.0 ~ 1.0）
     */
    public double getHitRate() {
        long total = getCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / total;
    }

    /**
     * 计算未命中率
     *
     * @return 未命中率（0.0 ~ 1.0）
     */
    public double getMissRate() {
        return 1.0 - getHitRate();
    }

    /**
     * 计算加载成功率
     *
     * @return 加载成功率（0.0 ~ 1.0）
     */
    public double getLoadSuccessRate() {
        long total = loadCount.get() + loadFailureCount.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) loadCount.get() / total;
    }

    /**
     * 重置指标
     */
    public void reset() {
        getCount.set(0);
        hitCount.set(0);
        missCount.set(0);
        putCount.set(0);
        evictCount.set(0);
        clearCount.set(0);
        loadCount.set(0);
        loadFailureCount.set(0);
    }

    @Override
    public String toString() {
        return String.format(
                "CacheMetrics{name='%s', type='%s', gets=%d, hits=%d, misses=%d, hitRate=%.2f%%, "
                        + "puts=%d, evictions=%d, loads=%d, loadFailures=%d}",
                cacheName, cacheType, getCount.get(), hitCount.get(), missCount.get(),
                getHitRate() * 100, putCount.get(), evictCount.get(),
                loadCount.get(), loadFailureCount.get());
    }
}