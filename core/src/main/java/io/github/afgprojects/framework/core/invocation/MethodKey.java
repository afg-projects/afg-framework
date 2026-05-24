package io.github.afgprojects.framework.core.invocation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public record MethodKey(String methodName, List<String> parameterTypes) {

    private static final ConcurrentHashMap<MethodKey, Method> CACHE = new ConcurrentHashMap<>();

    public Method resolve(Class<?> serviceType) {
        return CACHE.computeIfAbsent(this, k -> {
            Class<?>[] paramTypes = parameterTypes().stream()
                    .map(this::loadClass)
                    .toArray(Class<?>[]::new);
            try {
                return serviceType.getMethod(methodName(), paramTypes);
            } catch (NoSuchMethodException e) {
                throw new ServiceInvocationException(
                        "Method not found: " + methodName() + " in " + serviceType.getName(), e);
            }
        });
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ServiceInvocationException("Class not found: " + className, e);
        }
    }
}
