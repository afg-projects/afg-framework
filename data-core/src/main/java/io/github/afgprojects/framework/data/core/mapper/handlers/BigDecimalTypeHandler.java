package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import java.math.BigDecimal;

public class BigDecimalTypeHandler implements TypeHandler<BigDecimal> {
    @Override public Class<BigDecimal> getType() { return BigDecimal.class; }
    @Override public int priority() { return 5; }

    @Override
    public BigDecimal convert(Object value, Class<BigDecimal> targetType) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        return null;
    }
}