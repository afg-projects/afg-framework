package io.github.afgprojects.framework.data.core.mapper;

public interface TypeHandler<T> {
    Class<T> getType();
    T convert(Object value, Class<T> targetType);
    default int priority() { return 0; }
}
