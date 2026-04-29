package io.github.afgprojects.framework.data.sql.scope.cache;

import io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider;
import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.function.Function;

/**
 * 缓存数据权限上下文提供者
 * <p>
 * 包装另一个 DataScopeContextProvider，为其结果添加缓存。
 * 缓存基于用户ID，避免重复计算用户权限信息。
 * <p>
 * 使用示例：
 * <pre>
 * // 原始提供者
 * DataScopeContextProvider originalProvider = () -> loadUserContext();
 *
 * // 创建缓存提供者
 * CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
 *     originalProvider,
 *     Duration.ofMinutes(5)
 * );
 *
 * // 或者使用静态工厂方法
 * DataScopeContextProvider cachedProvider = CachedDataScopeContextProvider.wrap(
 *     originalProvider,
 *     Duration.ofMinutes(5)
 * );
 * </pre>
 */
public class CachedDataScopeContextProvider implements DataScopeContextProvider {

    /**
     * 缓存
     */
    private final DataScopeContextCache cache;

    /**
     * 用户ID解析器
     */
    private final Function<DataScopeUserContext, Long> userIdExtractor;

    /**
     * 委托的上下文提供者
     */
    private final DataScopeContextProvider delegate;

    /**
     * 创建缓存数据权限上下文提供者
     *
     * @param delegate         委托的上下文提供者
     * @param expireDuration   缓存过期时间
     */
    public CachedDataScopeContextProvider(
            DataScopeContextProvider delegate,
            Duration expireDuration) {
        this(delegate, expireDuration, DataScopeUserContext::getUserId);
    }

    /**
     * 创建缓存数据权限上下文提供者
     *
     * @param delegate         委托的上下文提供者
     * @param expireDuration   缓存过期时间
     * @param userIdExtractor  用户ID提取函数
     */
    public CachedDataScopeContextProvider(
            DataScopeContextProvider delegate,
            Duration expireDuration,
            Function<DataScopeUserContext, Long> userIdExtractor) {
        this.delegate = delegate;
        this.userIdExtractor = userIdExtractor;
        this.cache = new DataScopeContextCache(expireDuration);
    }

    @Override
    public @Nullable DataScopeUserContext provide() {
        // 首先从委托提供者获取上下文，以确定用户ID
        DataScopeUserContext freshContext = delegate.provide();
        if (freshContext == null) {
            return null;
        }

        Long userId = userIdExtractor.apply(freshContext);
        if (userId == null) {
            // 没有用户ID，不缓存
            return freshContext;
        }

        // 使用缓存
        return cache.getOrCreate(userId, () -> freshContext);
    }

    /**
     * 使指定用户的缓存失效
     *
     * @param userId 用户ID
     */
    public void invalidate(Long userId) {
        cache.invalidate(userId);
    }

    /**
     * 使所有缓存失效
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的条目数
     */
    public int cacheSize() {
        return cache.size();
    }

    /**
     * 关闭缓存
     */
    public void shutdown() {
        cache.shutdown();
    }

    /**
     * 包装一个 DataScopeContextProvider 为带缓存的版本
     *
     * @param delegate       原始提供者
     * @param expireDuration 缓存过期时间
     * @return 带缓存的提供者
     */
    public static CachedDataScopeContextProvider wrap(
            DataScopeContextProvider delegate,
            Duration expireDuration) {
        return new CachedDataScopeContextProvider(delegate, expireDuration);
    }
}