package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantInvocationInterceptor implements InvocationInterceptor {

    @Override
    public int order() { return 200; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().tenantScope()) return true;
        // Tenant context injection is handled by the existing TenantFilter/TenantContextHolder mechanism
        // This interceptor marks the invocation as tenant-scoped for downstream processing
        context.attributes().put("tenantScope", true);
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
