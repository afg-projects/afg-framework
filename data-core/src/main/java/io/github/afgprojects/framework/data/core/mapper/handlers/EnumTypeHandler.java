package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

public class EnumTypeHandler implements TypeHandler<Enum> {
    @Override public Class<Enum> getType() { return Enum.class; }
    @Override public int priority() { return 5; }

    @Override
    @SuppressWarnings("unchecked")
    public Enum convert(Object value, Class<Enum> targetType) {
        if (value == null) return null;
        Class<? extends Enum> enumType = targetType;
        if (value instanceof String s) {
            for (var c : enumType.getEnumConstants()) {
                if (c.name().equals(s)) return c;
            }
        }
        if (value instanceof Number num) {
            int codeValue = num.intValue();
            for (var c : enumType.getEnumConstants()) {
                try {
                    var getCode = c.getClass().getMethod("getCode");
                    Object codeResult = getCode.invoke(c);
                    if (codeResult instanceof Number codeNum && codeNum.intValue() == codeValue) return c;
                } catch (NoSuchMethodException e) {
                    break;
                } catch (ReflectiveOperationException e) {
                    continue;
                }
            }
            Object[] constants = enumType.getEnumConstants();
            if (codeValue >= 0 && codeValue < constants.length) return (Enum) constants[codeValue];
        }
        return null;
    }
}