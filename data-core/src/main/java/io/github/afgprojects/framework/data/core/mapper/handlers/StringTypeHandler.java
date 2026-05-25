package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

public class StringTypeHandler implements TypeHandler<String> {
    @Override public Class<String> getType() { return String.class; }
    @Override public int priority() { return -100; }

    @Override
    public String convert(Object value, Class<String> targetType) {
        return value.toString();
    }
}