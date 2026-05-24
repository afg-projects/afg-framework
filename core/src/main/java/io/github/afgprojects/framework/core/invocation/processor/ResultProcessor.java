package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.OperationMetadata;

public interface ResultProcessor {
    int priority();
    boolean supports(Object result, OperationMetadata metadata);
    Object process(Object result, ResultContext context);
}
