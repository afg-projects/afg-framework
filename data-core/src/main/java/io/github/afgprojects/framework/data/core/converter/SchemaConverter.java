package io.github.afgprojects.framework.data.core.converter;

/**
 * Schema 转换器接口
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 */
@FunctionalInterface
public interface SchemaConverter<S, T> {

    /**
     * 转换源对象到目标对象
     *
     * @param source 源对象
     * @return 目标对象
     */
    T convert(S source);
}
