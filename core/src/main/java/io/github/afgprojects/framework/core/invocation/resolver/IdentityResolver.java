package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public class IdentityResolver implements ArgumentResolver {

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean supports(ParameterMetadata param, Object rawValue) {
        try {
            Class<?> targetClass = Class.forName(param.type());
            return targetClass.isInstance(rawValue);
        } catch (ClassNotFoundException e) {
            return rawValue.getClass().getName().equals(param.type());
        }
    }

    @Override
    public Object resolve(ResolveContext context) {
        return context.rawValue();
    }
}