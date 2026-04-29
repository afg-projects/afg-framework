/**
 * AFG 缓存抽象层
 * <p>
 * 提供统一的缓存操作接口和实现，支持：
 * <ul>
 *   <li>本地缓存（基于 Caffeine）</li>
 *   <li>分布式缓存（基于 Redisson）</li>
 *   <li>多级缓存（本地 + 分布式）</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取缓存
 * AfgCache<User> cache = cacheManager.getCache("users");
 *
 * // 存入缓存
 * cache.put("user:1", user, 3600000);
 *
 * // 获取缓存
 * User user = cache.get("user:1");
 *
 * // 使用声明式缓存
 * @Cached(cacheName = "users", key = "#id", ttl = 60, timeUnit = TimeUnit.MINUTES)
 * public User getUser(String id) { ... }
 * }</pre>
 * </p>
 */
package io.github.afgprojects.framework.core.cache;