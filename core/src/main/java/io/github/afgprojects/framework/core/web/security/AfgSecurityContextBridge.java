package io.github.afgprojects.framework.core.web.security;

import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * AFG 安全上下文桥接器
 *
 * 将 AfgSecurityContext 中的认证信息同步到 RequestContext。
 */
public interface AfgSecurityContextBridge {

    void populate(AfgSecurityContext securityContext, RequestContext requestContext);
}
