package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;

import java.util.List;

public class PagedResultProcessor implements ResultProcessor {
    @Override
    public int priority() {
        return 300;
    }

    @Override
    public boolean supports(InvocationContext context, Object result) {
        return context.operationMetadata().paged() && result instanceof List;
    }

    @Override
    public Object process(ResultContext context) {
        return PagedResult.of((List<?>) context.result());
    }
}
