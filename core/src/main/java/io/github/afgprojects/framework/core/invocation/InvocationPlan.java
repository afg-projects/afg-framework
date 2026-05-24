package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface InvocationPlan {
    ServiceMetadata<?> serviceMetadata();
    OperationMetadata operationMetadata();
    Object targetBean();
    Object[] resolvedArguments();
    List<InvocationInterceptor> applicableInterceptors();
}
