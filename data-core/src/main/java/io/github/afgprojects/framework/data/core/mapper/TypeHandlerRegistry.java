package io.github.afgprojects.framework.data.core.mapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TypeHandlerRegistry {

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<TypeHandler<?>>> handlers = new ConcurrentHashMap<>();

    public void register(TypeHandler<?> handler) {
        handlers.computeIfAbsent(handler.getType(), k -> new CopyOnWriteArrayList<>())
                .add(handler);
        handlers.get(handler.getType()).sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    }

    public void unregister(Class<?> type) {
        handlers.remove(type);
    }

    public Object convert(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        List<TypeHandler<?>> list = handlers.get(targetType);
        if (list != null && !list.isEmpty()) {
            @SuppressWarnings("unchecked")
            TypeHandler<Object> handler = (TypeHandler<Object>) list.get(0);
            return handler.convert(value, (Class<Object>) targetType);
        }
        return value;
    }

    public static TypeHandlerRegistry defaultRegistry() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.NumberTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.BooleanNumberTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.DateTimeTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.EnumTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.StringTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.BlobTypeHandler());
        registry.register(new io.github.afgprojects.framework.data.core.mapper.handlers.BigDecimalTypeHandler());
        return registry;
    }
}