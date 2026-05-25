package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;

public class IdentityProcessor implements ResultProcessor {
    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(InvocationContext context, Object result) {
        return true;
    }

    @Override
    public Object process(ResultContext context) {
        return context.result();
    }
}
