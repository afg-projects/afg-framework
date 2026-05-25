package io.github.afgprojects.framework.data.core.mapper;

import java.util.function.Function;

public interface Projection<T, R> {
    Class<T> sourceType();
    Class<R> resultType();
    R map(T source);

    static <T, R> Projection<T, R> of(Class<T> sourceType, Class<R> resultType, Function<T, R> mapper) {
        return new Projection<>() {
            @Override public Class<T> sourceType() { return sourceType; }
            @Override public Class<R> resultType() { return resultType; }
            @Override public R map(T source) { return mapper.apply(source); }
        };
    }
}
