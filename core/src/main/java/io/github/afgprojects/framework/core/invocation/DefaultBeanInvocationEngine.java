package io.github.afgprojects.framework.core.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.ArgumentResolver;
import io.github.afgprojects.framework.core.invocation.resolver.DefaultResolveContext;
import io.github.afgprojects.framework.core.invocation.resolver.ResolveContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class DefaultBeanInvocationEngine implements BeanInvocationEngine {

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
        this.interceptors = interceptors.stream().sorted(Comparator.comparingInt(InvocationInterceptor::order)).toList();
        this.argumentResolvers = argumentResolvers.stream().sorted(Comparator.comparingInt(ArgumentResolver::priority)).toList();
        this.resultProcessors = resultProcessors.stream().sorted(Comparator.comparingInt(ResultProcessor::priority)).toList();
        this.objectMapper = objectMapper;
        this.asyncExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                r -> new Thread(r, "bean-invocation-async"));
    }

    @Override
    public Object invoke(String serviceName, String operationName, Map<String, Object> arguments) {
        InvocationPlan plan = plan(serviceName, operationName, arguments);
        InvocationContext context = new DefaultInvocationContext(
                plan.serviceMetadata(), plan.operationMetadata(), plan.targetBean(),
                plan.resolvedArguments(), arguments, new HashMap<>());

        // before interceptors
        for (InvocationInterceptor interceptor : interceptors) {
            if (!interceptor.before(context)) {
                throw new InvocationRejectedException(interceptor.getClass().getSimpleName(), "Interceptor rejected invocation");
            }
        }

        // execute
        Object result;
        try {
            Method method = plan.operationMetadata().method().resolve(plan.serviceMetadata().serviceType());
            result = method.invoke(plan.targetBean(), plan.resolvedArguments());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            Exception ex = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.onError(context, ex);
            }
            throw new ServiceInvocationException("Invocation failed: " + serviceName + "." + operationName, e);
        }

        // after interceptors
        for (InvocationInterceptor interceptor : interceptors) {
            result = interceptor.after(context, result);
        }

        // result processors
        for (ResultProcessor processor : resultProcessors) {
            if (processor.supports(result, plan.operationMetadata())) {
                result = processor.process(result, null);
                break;
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
        Object result = invoke(serviceName, operationName, arguments);
        if (result == null) return null;
        if (returnType.isAssignableFrom(result.getClass())) return (T) result;
        return objectMapper.convertValue(result, returnType);
    }

    @Override
    public CompletableFuture<Object> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments) {
        OperationMetadata op = registry.getOperation(serviceName, operationName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));
        if (!op.async()) {
            log.warn("Operation {}.{} is not marked as async but invoked asynchronously", serviceName, operationName);
        }
        return CompletableFuture.supplyAsync(() -> invoke(serviceName, operationName, arguments), asyncExecutor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> invokeAsync(String serviceName, String operationName, Map<String, Object> arguments, Class<T> returnType) {
        return invokeAsync(serviceName, operationName, arguments).thenApply(result -> {
            if (result == null) return null;
            if (returnType.isAssignableFrom(result.getClass())) return returnType.cast(result);
            return objectMapper.convertValue(result, returnType);
        });
    }

    @Override
    public InvocationPlan plan(String serviceName, String operationName, Map<String, Object> arguments) {
        ServiceMetadata<?> serviceMeta = registry.get(serviceName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));
        OperationMetadata opMeta = serviceMeta.operations().stream()
                .filter(op -> op.name().equals(operationName))
                .findFirst()
                .orElseThrow(() -> new ServiceNotFoundException(serviceName, operationName));

        Object bean = beanProvider.getBean(serviceName);
        Object[] resolvedArgs = resolveArguments(opMeta, arguments);
        return new DefaultInvocationPlan(serviceMeta, opMeta, bean, resolvedArgs, interceptors);
    }

    private Object[] resolveArguments(OperationMetadata opMeta, Map<String, Object> arguments) {
        Object[] resolved = new Object[opMeta.parameters().size()];
        for (ParameterMetadata param : opMeta.parameters()) {
            if (param.injected()) { resolved[param.index()] = null; continue; }

            Object rawValue = arguments.get(param.name());
            if (rawValue == null && !param.defaultValue().isEmpty()) { rawValue = resolveDefaultValue(param); }
            if (rawValue == null && param.required()) { throw new MissingArgumentException(param.name()); }
            if (rawValue != null) { resolved[param.index()] = convertArgument(param, rawValue); }
            else { resolved[param.index()] = null; }
        }
        return resolved;
    }

    private static final Map<String, Class<?>> PRIMITIVE_TYPES = Map.of(
            "boolean", boolean.class, "byte", byte.class, "char", char.class,
            "short", short.class, "int", int.class, "long", long.class,
            "float", float.class, "double", double.class, "void", void.class
    );

    private Class<?> resolveType(String typeName) throws ClassNotFoundException {
        Class<?> primitive = PRIMITIVE_TYPES.get(typeName);
        if (primitive != null) return primitive;
        return Class.forName(typeName);
    }

    private Object resolveDefaultValue(ParameterMetadata param) {
        if (param.defaultValue().isEmpty()) return null;
        try {
            Class<?> targetType = resolveType(param.type());
            ResolveContext ctx = new DefaultResolveContext(param, objectMapper, Map.of());
            for (ArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(String.class, targetType)) {
                    return resolver.resolve(param.defaultValue(), targetType, ctx);
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("Cannot load class for parameter type: {}", param.type());
        }
        return param.defaultValue();
    }

    private Object convertArgument(ParameterMetadata param, Object rawValue) {
        try {
            Class<?> targetType = resolveType(param.type());
            if (targetType.isAssignableFrom(rawValue.getClass())) return rawValue;
            ResolveContext ctx = new DefaultResolveContext(param, objectMapper, Map.of());
            for (ArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(rawValue.getClass(), targetType)) {
                    try { return resolver.resolve(rawValue, targetType, ctx); }
                    catch (Exception e) { /* try next */ }
                }
            }
            throw new ArgumentConversionException(param.name(), rawValue.getClass(), targetType, null);
        } catch (ClassNotFoundException e) {
            throw new ArgumentConversionException(param.name(), rawValue.getClass(), Object.class, e);
        }
    }
}
