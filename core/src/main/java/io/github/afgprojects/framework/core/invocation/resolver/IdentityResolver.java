package io.github.afgprojects.framework.core.invocation.resolver;

public class IdentityResolver implements ArgumentResolver {
    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return targetType.isAssignableFrom(sourceType);
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        return source;
    }
}
