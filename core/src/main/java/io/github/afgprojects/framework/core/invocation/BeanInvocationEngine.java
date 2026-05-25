package io.github.afgprojects.framework.core.invocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface BeanInvocationEngine {

    /**
     * Invokes an operation by name with named arguments.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     named arguments as a Map
     * @return the result of the invocation
     * @throws ServiceNotFoundException    if the service or operation is not found
     * @throws MissingArgumentException    if a required argument is missing
     * @throws ArgumentConversionException if an argument cannot be converted
     * @throws ServiceInvocationException  if the invocation fails
     */
    Object invoke(String serviceName, String operationName, Map<String, Object> arguments);

    /**
     * Invokes an operation by name with positional arguments.
     * <p>
     * Arguments are matched by position to the operation's parameter list.
     * No argument resolution is performed — arguments are passed directly
     * to the method as-is.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     positional arguments in method parameter order
     * @return the result of the invocation
     * @throws ServiceNotFoundException    if the service or operation is not found
     * @throws ServiceInvocationException  if the invocation fails
     */
    Object invoke(String serviceName, String operationName, Object... arguments);

    /**
     * Invokes an operation by name with named arguments and converts the result.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     named arguments as a Map
     * @param resultType    the expected result type
     * @param <R>           the result type
     * @return the result converted to the expected type
     * @throws ServiceNotFoundException    if the service or operation is not found
     * @throws MissingArgumentException    if a required argument is missing
     * @throws ArgumentConversionException if an argument cannot be converted
     * @throws ServiceInvocationException  if the invocation fails
     */
    <R> R invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<R> resultType);

    /**
     * Invokes an operation by name with positional arguments and converts the result.
     * <p>
     * Arguments are matched by position to the operation's parameter list.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     positional arguments in method parameter order
     * @param resultType    the expected result type
     * @param <R>           the result type
     * @return the result converted to the expected type
     * @throws ServiceNotFoundException    if the service or operation is not found
     * @throws ServiceInvocationException  if the invocation fails
     */
    <R> R invoke(String serviceName, String operationName, Object[] arguments, Class<R> resultType);

    CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments);

    <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType);

    /**
     * Invokes an operation asynchronously with positional arguments.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     positional arguments in method parameter order
     * @return a CompletableFuture with the result
     */
    CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Object... arguments);

    /**
     * Invokes an operation asynchronously with positional arguments and converts the result.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     positional arguments in method parameter order
     * @param resultType    the expected result type
     * @param <R>           the result type
     * @return a CompletableFuture with the result converted to the expected type
     */
    <R> CompletableFuture<R> invokeAsync(String serviceName, String operationName, Object[] arguments, Class<R> resultType);

    InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments);

    /**
     * Creates an invocation plan with positional arguments.
     * <p>
     * Arguments are matched by position — no argument resolution is performed.
     *
     * @param serviceName   the service name
     * @param operationName the operation name
     * @param arguments     positional arguments in method parameter order
     * @return the invocation plan
     * @throws ServiceNotFoundException if the service or operation is not found
     */
    InvocationPlan plan(String serviceName, String operationName, Object... arguments);
}