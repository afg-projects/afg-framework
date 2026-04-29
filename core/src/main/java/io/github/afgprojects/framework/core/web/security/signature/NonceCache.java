package io.github.afgprojects.framework.core.web.security.signature;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Nonce 缓存
 * <p>
 * 使用 LRU 策略缓存已使用的 nonce，防止重放攻击。
 * 线程安全，支持并发访问。
 */
public class NonceCache {

    private final int maxSize;
    private final LinkedHashMap<String, Long> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 创建 Nonce 缓存
     *
     * @param maxSize 最大容量
     */
    public NonceCache(int maxSize) {
        this.maxSize = maxSize;
        // 使用访问顺序的 LinkedHashMap 实现 LRU
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > NonceCache.this.maxSize;
            }
        };
    }

    /**
     * 检查并添加 nonce
     * <p>
     * 如果 nonce 已存在返回 false（表示重复），
     * 如果 nonce 不存在则添加并返回 true。
     *
     * @param nonce     随机数
     * @param timestamp 时间戳
     * @return 如果 nonce 未被使用返回 true
     */
    public boolean checkAndAdd(@NonNull String nonce, long timestamp) {
        lock.writeLock().lock();
        try {
            // 检查是否已存在
            if (cache.containsKey(nonce)) {
                return false;
            }
            // 添加到缓存
            cache.put(nonce, timestamp);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查 nonce 是否存在
     *
     * @param nonce 随机数
     * @return 如果存在返回 true
     */
    public boolean contains(@NonNull String nonce) {
        lock.readLock().lock();
        try {
            return cache.containsKey(nonce);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取 nonce 的时间戳
     *
     * @param nonce 随机数
     * @return 时间戳，如果不存在返回 null
     */
    public @Nullable Long getTimestamp(@NonNull String nonce) {
        lock.readLock().lock();
        try {
            return cache.get(nonce);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清理过期的 nonce
     *
     * @param expireTime 过期时间点（毫秒时间戳）
     */
    public void cleanExpired(long expireTime) {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取当前缓存大小
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
