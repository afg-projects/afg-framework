package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ArgumentConversionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Map.entry;

public class StringConverterResolver implements ArgumentResolver {
    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.ofEntries(
        entry(Integer.class, Integer::valueOf),
        entry(int.class, s -> Integer.parseInt(s)),
        entry(Long.class, Long::valueOf),
        entry(long.class, s -> Long.parseLong(s)),
        entry(Double.class, Double::valueOf),
        entry(double.class, s -> Double.parseDouble(s)),
        entry(Float.class, Float::valueOf),
        entry(float.class, s -> Float.parseFloat(s)),
        entry(Boolean.class, Boolean::valueOf),
        entry(boolean.class, s -> Boolean.parseBoolean(s)),
        entry(BigDecimal.class, BigDecimal::new),
        entry(LocalDate.class, LocalDate::parse),
        entry(LocalDateTime.class, LocalDateTime::parse),
        entry(UUID.class, UUID::fromString)
    );

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public boolean supports(Class<?> sourceType, Class<?> targetType) {
        return (sourceType == String.class && CONVERTERS.containsKey(targetType)) || targetType == String.class;
    }

    @Override
    public Object resolve(Object source, Class<?> targetType, ResolveContext context) {
        if (source instanceof String s && CONVERTERS.containsKey(targetType)) {
            try {
                return CONVERTERS.get(targetType).apply(s);
            } catch (Exception e) {
                throw new ArgumentConversionException(context.parameterMetadata().name(), source.getClass(), targetType, e);
            }
        }
        if (targetType == String.class) {
            return source.toString();
        }
        throw new ArgumentConversionException(context.parameterMetadata().name(), source.getClass(), targetType, null);
    }
}
