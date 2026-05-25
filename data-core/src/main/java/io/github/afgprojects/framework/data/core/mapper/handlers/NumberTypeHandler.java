package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberTypeHandler implements TypeHandler<Number> {
    @Override public Class<Number> getType() { return Number.class; }

    @Override
    public Number convert(Object value, Class<Number> targetType) {
        if (!(value instanceof Number num)) return null;
        Class<?> target = targetType;
        if (target == Long.class || target == long.class) return num.longValue();
        if (target == Integer.class || target == int.class) return num.intValue();
        if (target == Short.class || target == short.class) return num.shortValue();
        if (target == Byte.class || target == byte.class) return num.byteValue();
        if (target == Double.class || target == double.class) return num.doubleValue();
        if (target == Float.class || target == float.class) return num.floatValue();
        if (target == BigInteger.class && num instanceof BigDecimal bd) return bd.toBigInteger();
        return num;
    }
}
