package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public record DefaultInvocationPlan(
    ServiceMetadata<?> serviceMetadata,
    OperationMetadata operationMetadata,
    Object targetBean,
    Object[] resolvedArguments,
    List<InvocationInterceptor> applicableInterceptors
) implements InvocationPlan {}
