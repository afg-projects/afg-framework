package io.github.afgprojects.framework.core.web.security;

import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * 默认安全上下文桥接器
 *
 * 将 principal.id -> requestContext.userId,
 * principal.name -> requestContext.username,
 * tenantId -> requestContext.tenantId
 */
public class DefaultAfgSecurityContextBridge implements AfgSecurityContextBridge {

    @Override
    public void populate(AfgSecurityContext securityContext, RequestContext requestContext) {
        AfgPrincipal principal = securityContext.getPrincipal();
        if (principal != null) {
            requestContext.setUserId(principal.getId());
            requestContext.setUsername(principal.getName());
        }

        String tenantId = securityContext.getTenantId();
        if (tenantId != null) {
            requestContext.setTenantId(tenantId);
        }
    }
}
