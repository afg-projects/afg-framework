package io.github.afgprojects.framework.core.web.security;

/**
 * AFG 安全上下文
 */
public interface AfgSecurityContext {

    AfgPrincipal getPrincipal();

    String getTenantId();

    <T> T getAttribute(String key);
}
