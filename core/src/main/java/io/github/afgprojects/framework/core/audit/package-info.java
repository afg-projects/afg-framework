/**
 * 审计日志模块
 * <p>
 * 提供结构化的操作审计日志记录机制，支持自动拦截标记了 {@code @Audited} 注解的方法
 * </p>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.audit.Audited} - 审计注解，标记需要审计的方法</li>
 *   <li>{@link io.github.afgprojects.framework.core.audit.AuditLog} - 审计日志记录</li>
 *   <li>{@link io.github.afgprojects.framework.core.audit.AuditLogAspect} - 切面，自动记录审计日志</li>
 *   <li>{@link io.github.afgprojects.framework.core.audit.AuditLogStorage} - 存储接口</li>
 *   <li>{@code io.github.afgprojects.impl.redis.audit.RedisAuditLogStorage} - Redis 存储（在 afg-redis 模块中）</li>
 *   <li>{@link io.github.afgprojects.framework.core.audit.LogAuditLogStorage} - 日志存储</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Service
 * public class UserService {
 *
 *     @Audited(operation = "创建用户", module = "用户管理")
 *     public User createUser(UserRequest request) {
 *         // 业务逻辑
 *     }
 *
 *     @Audited(
 *         operation = "更新密码",
 *         module = "用户管理",
 *         sensitiveFields = {"password", "oldPassword"},
 *         target = "#userId"
 *     )
 *     public void updatePassword(String userId, String password, String oldPassword) {
 *         // 业务逻辑
 *     }
 * }
 * }</pre>
 *
 * <h2>配置项</h2>
 * <pre>
 * afg.audit.enabled=true
 * afg.audit.storage-type=redis
 * afg.audit.max-size=10000
 * afg.audit.ttl=7d
 * afg.audit.multi-tenant=true
 * </pre>
 */
package io.github.afgprojects.framework.core.audit;
