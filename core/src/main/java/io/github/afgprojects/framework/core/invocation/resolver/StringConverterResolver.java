package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class StringConverterResolver implements ArgumentResolver {

    private static final Map<String, StringConverter<?>> CONVERTERS = Map.ofEntries(
            Map.entry("byte", StringConverter.of(Byte::parseByte)),
            Map.entry("java.lang.Byte", StringConverter.of(Byte::valueOf)),
            Map.entry("short", StringConverter.of(Short::parseShort)),
            Map.entry("java.lang.Short", StringConverter.of(Short::valueOf)),
            Map.entry("int", StringConverter.of(Integer::parseInt)),
            Map.entry("java.lang.Integer", StringConverter.of(Integer::valueOf)),
            Map.entry("long", StringConverter.of(Long::parseLong)),
            Map.entry("java.lang.Long", StringConverter.of(Long::valueOf)),
            Map.entry("float", StringConverter.of(Float::parseFloat)),
            Map.entry("java.lang.Float", StringConverter.of(Float::valueOf)),
            Map.entry("double", StringConverter.of(Double::parseDouble)),
            Map.entry("java.lang.Double", StringConverter.of(Double::valueOf)),
            Map.entry("boolean", StringConverter.of(Boolean::parseBoolean)),
            Map.entry("java.lang.Boolean", StringConverter.of(Boolean::valueOf)),
            Map.entry("java.math.BigDecimal", StringConverter.of(BigDecimal::new)),
            Map.entry("java.math.BigInteger", StringConverter.of(BigInteger::new)),
            Map.entry("java.util.UUID", StringConverter.of(UUID::fromString)),
            Map.entry("java.time.LocalDate", StringConverter.of(LocalDate::parse)),
            Map.entry("java.time.LocalDateTime", StringConverter.of(LocalDateTime::parse)),
            Map.entry("java.lang.String", StringConverter.of(s -> s))
    );

    @FunctionalInterface
    interface StringConverter<T> {
        T convert(String value);

        static <T> StringConverter<T> of(java.util.function.Function<String, T> fn) {
            return fn::apply;
        }
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public boolean supports(ParameterMetadata param, Object rawValue) {
        return rawValue instanceof String && CONVERTERS.containsKey(param.type());
    }

    @Override
    public Object resolve(ResolveContext context) {
        String value = (String) context.rawValue();
        StringConverter<?> converter = CONVERTERS.get(context.parameterMetadata().type());
        if (converter == null) {
            return value;
        }
        return converter.convert(value);
    }
}