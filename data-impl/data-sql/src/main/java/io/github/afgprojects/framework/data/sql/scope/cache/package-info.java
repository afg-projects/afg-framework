/**
 * 数据权限缓存模块
 * <p>
 * 提供数据权限上下文的缓存功能，避免每次查询都重新计算用户权限信息。
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.cache.DataScopeContextCache} - 数据权限上下文缓存</li>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.cache.CachedDataScopeContextProvider} - 缓存数据权限上下文提供者</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * // 直接使用缓存
 * DataScopeContextCache cache = new DataScopeContextCache(Duration.ofMinutes(5));
 * DataScopeUserContext context = cache.getOrCreate(userId, () -> loadUserContext(userId));
 *
 * // 包装提供者
 * DataScopeContextProvider cachedProvider = CachedDataScopeContextProvider.wrap(
 *     originalProvider,
 *     Duration.ofMinutes(5)
 * );
 *
 * // 手动失效缓存
 * cachedProvider.invalidate(userId);
 * cachedProvider.invalidateAll();
 * </pre>
 */
package io.github.afgprojects.framework.data.sql.scope.cache;