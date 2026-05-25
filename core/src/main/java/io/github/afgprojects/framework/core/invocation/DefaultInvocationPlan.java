package io.github.afgprojects.framework.core.invocation;

import java.lang.reflect.Method;
import java.util.List;

public record DefaultInvocationPlan(
        InvocationContext context,
        ServiceMetadata<?> serviceMetadata,
        OperationMetadata operationMetadata,
        Object targetBean,
        Object[] resolvedArguments,
        Method method,
        List<InvocationInterceptor> applicableInterceptors
) implements InvocationPlan {
}