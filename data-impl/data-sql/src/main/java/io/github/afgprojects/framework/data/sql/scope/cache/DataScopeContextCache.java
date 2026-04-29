package io.github.afgprojects.framework.data.sql.scope.cache;

import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据权限上下文缓存
 * <p>
 * 缓存用户的数据权限上下文，避免每次查询都重新计算。
 * 支持过期时间和手动清除。
 * <p>
 * 使用示例：
 * <pre>
 * // 创建缓存，过期时间 5 分钟
 * DataScopeContextCache cache = new DataScopeContextCache(Duration.ofMinutes(5));
 *
 * // 获取或计算用户上下文
 * DataScopeUserContext context = cache.getOrCreate(userId, () -> {
 *     return loadUserContext(userId);
 * });
 *
 * // 清除指定用户的缓存
 * cache.invalidate(userId);
 *
 * // 清除所有缓存
 * cache.invalidateAll();
 * </pre>
 */
public class DataScopeContextCache {

    /**
     * 缓存项
     */
    private static class CacheEntry {
        final DataScopeUserContext context;
        final long expireTime;

        CacheEntry(DataScopeUserContext context, long expireTime) {
            this.context = context;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 用户ID -> 缓存项
     */
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 缓存过期时间
     */
    private final Duration expireDuration;

    /**
     * 清理任务的调度器
     */
    private final @Nullable ScheduledExecutorService scheduler;

    /**
     * 创建数据权限上下文缓存
     *
     * @param expireDuration 缓存过期时间
     */
    public DataScopeContextCache(Duration expireDuration) {
        this(expireDuration, true);
    }

    /**
     * 创建数据权限上下文缓存
     *
     * @param expireDuration   缓存过期时间
     * @param enableAutoCleanup 是否启用自动清理过期项
     */
    public DataScopeContextCache(Duration expireDuration, boolean enableAutoCleanup) {
        this.expireDuration = expireDuration;

        if (enableAutoCleanup) {
            // 启动定期清理任务
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DataScopeContextCache-Cleanup");
                t.setDaemon(true);
                return t;
            });
            // 每分钟清理一次过期项
            scheduler.scheduleAtFixedRate(
                    this::cleanup,
                    expireDuration.toMillis() / 2,
                    Math.max(expireDuration.toMillis() / 2, 60000),
                    TimeUnit.MILLISECONDS
            );
        } else {
            this.scheduler = null;
        }
    }

    /**
     * 获取或创建用户上下文
     *
     * @param userId  用户ID
     * @param loader  加载函数（当缓存不存在或过期时调用）
     * @return 用户上下文
     */
    public DataScopeUserContext getOrCreate(Long userId, ContextLoader loader) {
        CacheEntry entry = cache.get(userId);

        // 如果缓存存在且未过期
        if (entry != null && !entry.isExpired()) {
            return entry.context;
        }

        // 加载新的上下文
        DataScopeUserContext context = loader.load();
        put(userId, context);
        return context;
    }

    /**
     * 获取缓存的用户上下文
     *
     * @param userId 用户ID
     * @return 用户上下文，如果不存在或已过期则返回 null
     */
    public @Nullable DataScopeUserContext get(Long userId) {
        CacheEntry entry = cache.get(userId);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.context;
    }

    /**
     * 缓存用户上下文
     *
     * @param userId  用户ID
     * @param context 用户上下文
     */
    public void put(Long userId, DataScopeUserContext context) {
        long expireTime = System.currentTimeMillis() + expireDuration.toMillis();
        cache.put(userId, new CacheEntry(context, expireTime));
    }

    /**
     * 使指定用户的缓存失效
     *
     * @param userId 用户ID
     */
    public void invalidate(Long userId) {
        cache.remove(userId);
    }

    /**
     * 使所有缓存失效
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的条目数（包括可能过期的条目）
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清理过期条目
     */
    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 关闭缓存，停止后台清理任务
     */
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        cache.clear();
    }

    /**
     * 上下文加载函数
     */
    @FunctionalInterface
    public interface ContextLoader {
        DataScopeUserContext load();
    }
}