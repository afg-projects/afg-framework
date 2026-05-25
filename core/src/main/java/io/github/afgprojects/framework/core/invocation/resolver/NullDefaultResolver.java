package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public class NullDefaultResolver implements ArgumentResolver {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(ParameterMetadata param, Object rawValue) {
        return rawValue == null && !param.defaultValue().isEmpty();
    }

    @Override
    public Object resolve(ResolveContext context) {
        StringConverterResolver stringResolver = new StringConverterResolver();
        if (stringResolver.supports(context.parameterMetadata(), context.parameterMetadata().defaultValue())) {
            return stringResolver.resolve(new DefaultResolveContext(
                    context.parameterMetadata(),
                    context.objectMapper(),
                    context.parameterMetadata().defaultValue()
            ));
        }
        return context.parameterMetadata().defaultValue();
    }
}