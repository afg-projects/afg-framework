package io.github.afgprojects.framework.core.invocation.interceptor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;
import io.github.afgprojects.framework.core.invocation.InvocationInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuditInvocationInterceptor implements InvocationInterceptor {
    @Override public int order() { return 400; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().audit()) return true;
        log.info("Invoking {}.{}", context.serviceMetadata().serviceName(), context.operationMetadata().name());
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) {
        if (!context.operationMetadata().audit()) return result;
        log.info("Invoked {}.{} successfully", context.serviceMetadata().serviceName(), context.operationMetadata().name());
        return result;
    }

    @Override
    public void onError(InvocationContext context, Exception exception) {
        if (!context.operationMetadata().audit()) return;
        log.error("Invoked {}.{} failed", context.serviceMetadata().serviceName(), context.operationMetadata().name(), exception);
    }
}
