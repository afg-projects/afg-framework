package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;

public class JacksonConvertResolver implements ArgumentResolver {
    @Override
    public int priority() {
        return 2;
    }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return !targetType.isAssignableFrom(sourceType);
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        try {
            return context.objectMapper().convertValue(source, targetType);
        } catch (IllegalArgumentException e) {
            throw new ArgumentConversionException(context.parameterMetadata().name(), source.getClass(), targetType, e);
        }
    }
}
