package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

import java.util.List;

public class PagedResultProcessor implements ResultProcessor {
    @Override
    public int priority() {
        return 300;
    }

    @Override
    public boolean supports(Object result, OperationMetadata metadata) {
        return metadata.paged() && result instanceof List;
    }

    @Override
    public Object process(Object result, ResultContext context) {
        return PagedResult.of((List<?>) result);
    }
}
