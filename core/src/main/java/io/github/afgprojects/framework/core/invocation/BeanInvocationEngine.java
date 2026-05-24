package io.github.afgprojects.framework.core.invocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface BeanInvocationEngine {
    Object invoke(String serviceName, String operationName, Map<String, Object> arguments);
    <T> T invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType);
    CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments);
    <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType);
    InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments);
}
