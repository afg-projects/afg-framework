package io.github.afgprojects.framework.core.api.duplicatesubmit;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 本地内存重复提交检查器实现
 * <p>
 * 使用 Caffeine 缓存实现内存去重，适用于单机部署或降级场景。
 * 基于 expireAfterWrite 过期策略，key 写入后在 intervalMs 后自动过期。
 * </p>
 * <p>
 * 注意：此实现仅对单实例有效，多实例部署时需要使用 Redis 分布式实现。
 * </p>
 *
 * @since 1.0.0
 */
public class LocalDuplicateSubmitChecker implements DuplicateSubmitChecker {

    private static final int DEFAULT_CACHE_SIZE = 10000;
    private static final int DEFAULT_EXPIRE_SECONDS = 3600;

    private final Cache<String, Long> submitCache;

    /**
     * 默认构造函数
     * <p>
     * 使用默认缓存大小（10000）和过期时间（3600秒）
     * </p>
     */
    public LocalDuplicateSubmitChecker() {
        this(DEFAULT_CACHE_SIZE, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 构造函数
     *
     * @param cacheSize         缓存最大容量
     * @param expireAfterSeconds 缓存默认过期时间（秒），用于 Caffeine 的 maximumSize 和 expireAfterWrite
     */
    public LocalDuplicateSubmitChecker(int cacheSize, int expireAfterSeconds) {
        this.submitCache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(Duration.ofSeconds(expireAfterSeconds))
                .build();
    }

    @Override
    public boolean tryAcquire(String key, long intervalMs) {
        long now = System.currentTimeMillis();
        Long existingTimestamp = submitCache.getIfPresent(key);

        if (existingTimestamp != null && (now - existingTimestamp) < intervalMs) {
            // key 存在且未过期，表示重复请求
            return false;
        }

        // key 不存在或已过期，标记为首次请求
        submitCache.put(key, now);
        return true;
    }

    @Override
    public void release(String key) {
        submitCache.invalidate(key);
    }
}
