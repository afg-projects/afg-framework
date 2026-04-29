package io.github.afgprojects.framework.core.cache;

import org.jspecify.annotations.NonNull;

/**
 * 缓存管理器接口
 * <p>
 * 定义统一的缓存管理操作，支持多缓存实例管理和生命周期控制。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持多缓存实例管理</li>
 *   <li>支持按名称获取缓存</li>
 *   <li>支持类型安全的缓存获取</li>
 *   <li>支持缓存销毁</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * CacheManager manager = new DefaultCacheManager(properties);
 * AfgCache&lt;User&gt; userCache = manager.getCache("users");
 * AfgCache&lt;Product&gt; productCache = manager.getCache("products", Product.class);
 * </pre>
 */
public interface CacheManager {

    /**
     * 获取缓存实例
     * <p>
     * 如果缓存不存在，根据配置自动创建
     * </p>
     *
     * @param name 缓存名称
     * @param <V>  缓存值类型
     * @return 缓存实例
     */
    <V> AfgCache<V> getCache(@NonNull String name);

    /**
     * 获取缓存实例（指定类型）
     * <p>
     * type 参数用于类型安全检查，实际缓存实例与 getCache(name) 相同
     * </p>
     *
     * @param name 缓存名称
     * @param type 缓存值类型
     * @param <V>  缓存值类型
     * @return 缓存实例
     */
    <V> AfgCache<V> getCache(@NonNull String name, Class<V> type);

    /**
     * 销毁所有缓存
     * <p>
     * 清空并移除所有缓存实例
     * </p>
     */
    void destroy();
}