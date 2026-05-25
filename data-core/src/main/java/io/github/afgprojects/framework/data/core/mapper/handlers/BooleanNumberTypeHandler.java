package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

public class BooleanNumberTypeHandler implements TypeHandler<Boolean> {
    @Override public Class<Boolean> getType() { return Boolean.class; }
    @Override public int priority() { return 10; }

    @Override
    public Boolean convert(Object value, Class<Boolean> targetType) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number num) return num.intValue() != 0;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}