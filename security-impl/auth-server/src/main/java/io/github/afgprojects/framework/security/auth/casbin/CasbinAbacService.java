package io.github.afgprojects.framework.security.auth.casbin;

import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.core.permission.AbacService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * Casbin ABAC 服务实现。
 *
 * <p>基于 Casbin 实现属性访问控制（ABAC）服务。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * CasbinAfgEnforcer enforcer = new CasbinAfgEnforcer(properties, policyService);
 * AbacService abacService = new CasbinAbacService(enforcer);
 *
 * // 检查权限
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
@Slf4j
public class CasbinAbacService implements AbacService {

    private final CasbinAfgEnforcer enforcer;

    /**
     * 构造函数。
     *
     * @param enforcer Casbin 执行器
     */
    public CasbinAbacService(@NonNull CasbinAfgEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public boolean enforce(@NonNull String subject, @NonNull String resource, @NonNull String action) {
        return enforcer.enforce(subject, resource, action);
    }

    @Override
    public boolean enforce(@NonNull String subject, @NonNull String domain,
                          @NonNull String resource, @NonNull String action) {
        return enforcer.enforce(subject, domain, resource, action);
    }

    @Override
    public void addPolicy(@NonNull String subject, @NonNull String resource, @NonNull String action) {
        // CasbinAfgEnforcer 的 addPolicy 需要 domain 参数，使用空字符串作为默认域
        enforcer.addPolicy(subject, "", resource, action);
        log.info("Added policy: subject={}, resource={}, action={}", subject, resource, action);
    }

    @Override
    public void addPolicy(@NonNull String subject, @NonNull String domain,
                         @NonNull String resource, @NonNull String action) {
        enforcer.addPolicy(subject, domain, resource, action);
        log.info("Added policy with domain: subject={}, domain={}, resource={}, action={}",
                subject, domain, resource, action);
    }

    @Override
    public void removePolicy(@NonNull String subject, @NonNull String resource, @NonNull String action) {
        // CasbinAfgEnforcer 的 removePolicy 需要 domain 参数，使用空字符串作为默认域
        enforcer.removePolicy(subject, "", resource, action);
        log.info("Removed policy: subject={}, resource={}, action={}", subject, resource, action);
    }

    @Override
    public void removePolicy(@NonNull String subject, @NonNull String domain,
                             @NonNull String resource, @NonNull String action) {
        enforcer.removePolicy(subject, domain, resource, action);
        log.info("Removed policy with domain: subject={}, domain={}, resource={}, action={}",
                subject, domain, resource, action);
    }

    /**
     * 获取底层 Casbin 执行器。
     *
     * @return Casbin 执行器
     */
    public CasbinAfgEnforcer getEnforcer() {
        return enforcer;
    }
}
