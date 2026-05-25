package io.github.afgprojects.framework.core.invocation.processor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;

public interface ResultProcessor {

    int priority();

    boolean supports(InvocationContext context, Object result);

    Object process(ResultContext context);
}