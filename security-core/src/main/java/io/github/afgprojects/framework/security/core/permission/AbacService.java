package io.github.afgprojects.framework.security.core.permission;

import org.jspecify.annotations.NonNull;

/**
 * 属性权限服务接口。
 *
 * <p>提供基于属性的访问控制（ABAC）服务。
 *
 * <h3>ABAC 模型</h3>
 * <p>主体属性 + 资源属性 + 操作 + 环境 -> 决策
 * <pre>{@code
 * // 检查用户是否可以对资源执行操作
 * boolean allowed = abacService.enforce("user-001", "document-001", "read");
 *
 * // 带域的权限检查
 * boolean allowed = abacService.enforce("user-001", "tenant-001", "document-001", "delete");
 *
 * // 添加策略
 * abacService.addPolicy("user-001", "document-001", "read");
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AbacService {

    /**
     * 检查主体是否可以对资源执行操作。
     *
     * @param subject 主体标识（如用户 ID），永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作（如 read, write, delete），永不为 null
     * @return 如果允许返回 true
     */
    boolean enforce(@NonNull String subject, @NonNull String resource, @NonNull String action);

    /**
     * 检查主体是否可以在域内对资源执行操作。
     *
     * @param subject 主体标识（如用户 ID），永不为 null
     * @param domain 域标识（如租户 ID），永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作（如 read, write, delete），永不为 null
     * @return 如果允许返回 true
     */
    boolean enforce(@NonNull String subject, @NonNull String domain,
                    @NonNull String resource, @NonNull String action);

    /**
     * 添加策略规则。
     *
     * @param subject 主体标识，永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作，永不为 null
     */
    void addPolicy(@NonNull String subject, @NonNull String resource, @NonNull String action);

    /**
     * 添加带域的策略规则。
     *
     * @param subject 主体标识，永不为 null
     * @param domain 域标识，永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作，永不为 null
     */
    void addPolicy(@NonNull String subject, @NonNull String domain,
                   @NonNull String resource, @NonNull String action);

    /**
     * 移除策略规则。
     *
     * @param subject 主体标识，永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作，永不为 null
     */
    void removePolicy(@NonNull String subject, @NonNull String resource, @NonNull String action);

    /**
     * 移除带域的策略规则。
     *
     * @param subject 主体标识，永不为 null
     * @param domain 域标识，永不为 null
     * @param resource 资源标识，永不为 null
     * @param action 操作，永不为 null
     */
    void removePolicy(@NonNull String subject, @NonNull String domain,
                      @NonNull String resource, @NonNull String action);
}
