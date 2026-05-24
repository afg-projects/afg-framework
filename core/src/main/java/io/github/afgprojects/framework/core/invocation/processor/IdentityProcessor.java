package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

public class IdentityProcessor implements ResultProcessor {
    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(Object result, OperationMetadata metadata) {
        return true;
    }

    @Override
    public Object process(Object result, ResultContext context) {
        return result;
    }
}
