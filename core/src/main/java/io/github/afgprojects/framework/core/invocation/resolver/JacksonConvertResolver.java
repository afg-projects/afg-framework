package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

import java.util.Collection;
import java.util.Map;

public class JacksonConvertResolver implements ArgumentResolver {

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public boolean supports(ParameterMetadata param, Object rawValue) {
        if (rawValue == null) return false;
        // Support Map/Collection-to-POJO conversions
        if (rawValue instanceof Map || rawValue instanceof Collection) return true;
        // Support String-to-enum conversions
        if (rawValue instanceof String) {
            try {
                Class<?> targetClass = Class.forName(param.type());
                return targetClass.isEnum();
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Object resolve(ResolveContext context) {
        ObjectMapper om = context.objectMapper();
        Object rawValue = context.rawValue();
        String targetType = context.parameterMetadata().type();
        try {
            Class<?> targetClass = Class.forName(targetType);
            return om.convertValue(rawValue, targetClass);
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            throw new ArgumentConversionException(
                    context.parameterMetadata().name(), targetType,
                    rawValue.getClass().getName(), e);
        }
    }
}