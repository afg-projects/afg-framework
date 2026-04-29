package io.github.afgprojects.framework.core.web.security;

/**
 * AFG 权限执行器
 *
 * 与jcasbin Enforcer.enforce(sub, obj, act) 天然对应，
 * auth模块的实现直接委托给jcasbin Enforcer。
 */
@FunctionalInterface
public interface AfgEnforcer {

    default boolean enforce(AfgSecurityContext context, String resource, String action) {
        String subject = context != null && context.getPrincipal() != null
                ? context.getPrincipal().getId()
                : null;
        return enforce(subject, resource, action);
    }

    boolean enforce(String subject, String resource, String action);
}
