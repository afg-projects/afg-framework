package io.github.afgprojects.framework.core.web.security.signature;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Nonce 缓存
 * <p>
 * 使用 ConcurrentHashMap 配合自定义 LRU 逻辑实现线程安全的 nonce 缓存，
 * 防止重放攻击。
 * <p>
 * 使用 ConcurrentLinkedDeque 维护访问顺序，实现 LRU 淘汰策略。
 */
public class NonceCache {

    private final int maxSize;
    private final ConcurrentHashMap<String, Long> cache;
    private final ConcurrentLinkedDeque<String> accessOrder;
    private final AtomicInteger size;

    /**
     * 创建 Nonce 缓存
     *
     * @param maxSize 最大容量
     */
    public NonceCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(16);
        this.accessOrder = new ConcurrentLinkedDeque<>();
        this.size = new AtomicInteger(0);
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
        // 使用 putIfAbsent 保证原子性
        Long existing = cache.putIfAbsent(nonce, timestamp);
        if (existing != null) {
            return false; // 已存在
        }

        // 添加到访问顺序队列
        accessOrder.addLast(nonce);
        int currentSize = size.incrementAndGet();

        // 如果超过最大容量，移除最旧的条目
        if (currentSize > maxSize) {
            evictOldest();
        }

        return true;
    }

    /**
     * 移除最旧的条目（LRU 淘汰）
     */
    private void evictOldest() {
        String oldest = accessOrder.pollFirst();
        if (oldest != null) {
            cache.remove(oldest);
            size.decrementAndGet();
        }
    }

    /**
     * 检查 nonce 是否存在
     *
     * @param nonce 随机数
     * @return 如果存在返回 true
     */
    public boolean contains(@NonNull String nonce) {
        return cache.containsKey(nonce);
    }

    /**
     * 获取 nonce 的时间戳
     *
     * @param nonce 随机数
     * @return 时间戳，如果不存在返回 null
     */
    public @Nullable Long getTimestamp(@NonNull String nonce) {
        return cache.get(nonce);
    }

    /**
     * 清理过期的 nonce
     *
     * @param expireTime 过期时间点（毫秒时间戳）
     */
    public void cleanExpired(long expireTime) {
        // 遍历并移除过期的条目
        cache.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        // 同步更新访问顺序队列
        accessOrder.removeIf(nonce -> {
            Long timestamp = cache.get(nonce);
            return timestamp == null || timestamp < expireTime;
        });
        // 更新大小
        size.set(cache.size());
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
        accessOrder.clear();
        size.set(0);
    }

    /**
     * 获取当前缓存大小
     */
    public int size() {
        return size.get();
    }
}
