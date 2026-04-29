/**
 * 数据权限处理模块
 * <p>
 * 提供 SQL 改写时的数据权限占位符解析功能。
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider} - 数据权限上下文提供者接口</li>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext} - 数据权限用户上下文</li>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.DataScopeProcessor} - 占位符解析处理器</li>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.DataScopeContextProviders} - 提供者工厂类</li>
 *   <li>{@link io.github.afgprojects.framework.data.sql.scope.DataScopePlaceholders} - 占位符常量</li>
 * </ul>
 * <p>
 * 支持的占位符：
 * <ul>
 *   <li>#{currentUserId} - 当前用户ID</li>
 *   <li>#{currentDeptId} - 当前用户部门ID</li>
 *   <li>#{currentUserDeptIds} - 当前用户部门ID列表</li>
 *   <li>#{currentUserDeptAndChildIds} - 当前用户部门及子部门ID列表</li>
 *   <li>#{currentTenantId} - 当前租户ID</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * // 1. 创建数据权限上下文提供者
 * DataScopeContextProvider provider = DataScopeContextProviders.user(userId, deptId);
 *
 * // 2. 创建数据权限处理器
 * DataScopeProcessor processor = new DataScopeProcessor(provider);
 *
 * // 3. 解析占位符
 * String sql = processor.resolvePlaceholders("user_id = #{currentUserId}");
 * // 结果: "user_id = 123"
 *
 * // 4. 在 SqlRewriter 中使用
 * SqlRewriter rewriter = new SqlRewriter(statement, context, provider);
 * String rewrittenSql = rewriter.rewrite();
 * </pre>
 * <p>
 * 缓存支持：
 * <pre>
 * // 使用缓存提供者
 * DataScopeContextProvider cachedProvider = CachedDataScopeContextProvider.wrap(
 *     originalProvider,
 *     Duration.ofMinutes(5)
 * );
 * </pre>
 */
package io.github.afgprojects.framework.data.sql.scope;