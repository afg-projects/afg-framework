package io.github.afgprojects.framework.core.invocation.resolver;

public interface ArgumentResolver {
    int priority();
    boolean supports(Class<?> sourceType, Class<?> targetType);
    Object resolve(Object source, Class<?> targetType, ResolveContext context);
}
