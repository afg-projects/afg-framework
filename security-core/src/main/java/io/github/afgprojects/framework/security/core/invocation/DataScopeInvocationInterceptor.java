package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataScopeInvocationInterceptor implements InvocationInterceptor {

    @Override
    public int order() { return 300; }

    @Override
    public boolean before(InvocationContext context) {
        if (!context.operationMetadata().dataScope()) return true;
        context.attributes().put("dataScopeEnabled", true);
        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
