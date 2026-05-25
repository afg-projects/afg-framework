package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.ArgumentResolver;
import io.github.afgprojects.framework.core.invocation.resolver.DefaultResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultBeanInvocationEngine implements BeanInvocationEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultBeanInvocationEngine.class);

    @FunctionalInterface
    public interface BeanProvider {
        Object getBean(String serviceName);
    }

    private final ServiceMetadataRegistry registry;
    private final BeanProvider beanProvider;
    private final List<InvocationInterceptor> interceptors;
    private final List<ArgumentResolver> argumentResolvers;
    private final List<ResultProcessor> resultProcessors;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncExecutor;

    public DefaultBeanInvocationEngine(ServiceMetadataRegistry registry,
                                       BeanProvider beanProvider,
                                       List<InvocationInterceptor> interceptors,
                                       List<ArgumentResolver> argumentResolvers,
                                       List<ResultProcessor> resultProcessors,
                                       ObjectMapper objectMapper) {
        this.registry = registry;
        this.beanProvider = beanProvider;
        this.interceptors = interceptors;
        this.argumentResolvers = argumentResolvers;
        this.resultProcessors = resultProcessors;
        this.objectMapper = objectMapper;
        this.asyncExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new InvocationContextTaskDecorator());
    }

    public DefaultBeanInvocationEngine(ServiceMetadataRegistry registry,
                                       BeanProvider beanProvider,
                                       List<InvocationInterceptor> interceptors,
                                       List<ArgumentResolver> argumentResolvers,
                                       List<ResultProcessor> resultProcessors,
                                       ObjectMapper objectMapper,
                                       int asyncPoolSize) {
        this.registry = registry;
        this.beanProvider = beanProvider;
        this.interceptors = interceptors;
        this.argumentResolvers = argumentResolvers;
        this.resultProcessors = resultProcessors;
        this.objectMapper = objectMapper;
        this.asyncExecutor = Executors.newFixedThreadPool(
                asyncPoolSize,
                new InvocationContextTaskDecorator());
    }

    @Override
    public Object invoke(String serviceName, String operationName, Map<String, Object> arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        return executePlan(plan);
    }

    @Override
    public Object invoke(String serviceName, String operationName, Object... arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        return executePlan(plan);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<R> resultType) {
        Object result = invoke(serviceName, operationName, arguments);
        return convertResult(result, resultType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R invoke(String serviceName, String operationName, Object[] arguments, Class<R> resultType) {
        Object result = invoke(serviceName, operationName, arguments);
        return convertResult(result, resultType);
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        return CompletableFuture.supplyAsync(() -> executePlan(plan), asyncExecutor);
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
        return invokeAsync(serviceName, operationName, arguments)
                .thenApply(result -> convertResult(result, returnType));
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Object... arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        return CompletableFuture.supplyAsync(() -> executePlan(plan), asyncExecutor);
    }

    @Override
    public <R> CompletableFuture<R> invokeAsync(String serviceName, String operationName, Object[] arguments, Class<R> resultType) {
        return invokeAsync(serviceName, operationName, arguments)
                .thenApply(result -> convertResult(result, resultType));
    }

    @Override
    public InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments) {
        ServiceMetadata<?> metadata = resolveService(serviceName);
        OperationMetadata operation = resolveOperation(metadata, operationName);
        Object[] resolvedArgs = resolveArguments(operation, arguments);
        Method method = operation.method().resolve(metadata.serviceType());
        Object bean = beanProvider.getBean(serviceName);

        InvocationContext context = new DefaultInvocationContext(
                serviceName, operationName, metadata, operation,
                arguments, resolvedArgs, bean, method);

        return new DefaultInvocationPlan(context, metadata, operation, bean, resolvedArgs, method, interceptors);
    }

    @Override
    public InvocationPlan plan(String serviceName, String operationName, Object... arguments) {
        ServiceMetadata<?> metadata = resolveService(serviceName);
        OperationMetadata operation = resolveOperation(metadata, operationName);
        Method method = operation.method().resolve(metadata.serviceType());
        Object bean = beanProvider.getBean(serviceName);

        Object[] args;
        if (arguments != null && arguments.length == 1 && arguments[0] instanceof Map<?, ?> mapArgs) {
            args = resolveArguments(operation, (Map<String, Object>) mapArgs);
        } else {
            args = arguments != null ? arguments : new Object[0];
        }

        InvocationContext context = new DefaultInvocationContext(
                serviceName, operationName, metadata, operation,
                null, args, bean, method);

        return new DefaultInvocationPlan(context, metadata, operation, bean, args, method, interceptors);
    }

    private Object executePlan(InvocationPlan plan) {
        InvocationContext context = plan.context();
        Object[] args = plan.resolvedArguments();

        runBeforeInterceptors(context);

        try {
            Object result = plan.method().invoke(plan.targetBean(), args);
            result = runAfterInterceptors(context, result);
            result = processResult(result, context);
            return result;
        } catch (Exception e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ie
                    ? ie.getTargetException() : e;
            runErrorInterceptors(context, cause);
            if (cause instanceof RuntimeException re) throw re;
            throw new ServiceInvocationException(
                    "Invocation failed: " + context.serviceName() + "." + context.operationName() + " - " + cause.getMessage(), cause);
        }
    }

    private ServiceMetadata<?> resolveService(String serviceName) {
        return registry.get(serviceName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName));
    }

    private OperationMetadata resolveOperation(ServiceMetadata<?> metadata, String operationName) {
        return metadata.operations().stream()
                .filter(op -> op.name().equals(operationName))
                .findFirst()
                .orElseThrow(() -> new ServiceNotFoundException(
                        metadata.serviceName() + "." + operationName));
    }

    private Object[] resolveArguments(OperationMetadata operation, Map<String, Object> arguments) {
        Object[] resolved = new Object[operation.parameters().size()];

        for (ParameterMetadata param : operation.parameters()) {
            Object rawValue = arguments.get(param.name());
            if (rawValue == null && !param.required()) {
                rawValue = param.defaultValue().isEmpty() ? null : param.defaultValue();
            }
            if (rawValue == null && param.required()) {
                throw new MissingArgumentException(
                        param.name());
            }
            resolved[param.index()] = resolveArgument(param, rawValue);
        }

        return resolved;
    }

    private Object resolveArgument(ParameterMetadata param, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        for (ArgumentResolver resolver : argumentResolvers) {
            if (resolver.supports(param, rawValue)) {
                try {
                    return resolver.resolve(new DefaultResolveContext(param, objectMapper, rawValue));
                } catch (Exception e) {
                    throw new ArgumentConversionException(
                            param.name(), param.type(), rawValue.getClass().getName(), e);
                }
            }
        }

        return rawValue;
    }

    @SuppressWarnings("unchecked")
    private <R> R convertResult(Object result, Class<R> resultType) {
        if (result == null) return null;
        if (resultType.isInstance(result)) return (R) result;
        return objectMapper.convertValue(result, resultType);
    }

    private Object processResult(Object result, InvocationContext context) {
        for (ResultProcessor processor : resultProcessors) {
            if (processor.supports(context, result)) {
                result = processor.process(new io.github.afgprojects.framework.core.invocation.processor.DefaultResultContext(context, result, objectMapper));
            }
        }
        return result;
    }

    private void runBeforeInterceptors(InvocationContext context) {
        for (InvocationInterceptor interceptor : interceptors) {
            if (!interceptor.before(context)) {
                throw new InvocationRejectedException(
                        interceptor.getClass().getSimpleName(), "before returned false");
            }
        }
    }

    private Object runAfterInterceptors(InvocationContext context, Object result) {
        for (InvocationInterceptor interceptor : interceptors) {
            result = interceptor.after(context, result);
        }
        return result;
    }

    private void runErrorInterceptors(InvocationContext context, Throwable error) {
        Exception ex = error instanceof Exception e ? e : new ServiceInvocationException(error.getMessage(), error);
        for (InvocationInterceptor interceptor : interceptors) {
            try {
                interceptor.onError(context, ex);
            } catch (Exception e) {
                log.error("Interceptor {} error handler failed", interceptor.getClass().getSimpleName(), e);
            }
        }
    }
}