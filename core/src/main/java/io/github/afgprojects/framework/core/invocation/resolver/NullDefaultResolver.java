package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public class NullDefaultResolver implements ArgumentResolver {
    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return true;
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        if (source != null) return source;
        ParameterMetadata meta = context.parameterMetadata();
        if (meta.defaultValue().isEmpty()) return null;
        StringConverterResolver stringResolver = new StringConverterResolver();
        if (stringResolver.supports(String.class, targetType)) {
            return stringResolver.resolve(meta.defaultValue(), targetType, context);
        }
        return null;
    }
}
