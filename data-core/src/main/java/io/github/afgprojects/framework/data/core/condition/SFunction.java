package io.github.afgprojects.framework.data.core.condition;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Lambda 字段引用函数式接口
 * <p>
 * 继承 Serializable 以支持序列化获取方法引用信息
 *
 * @param <T> 实体类型
 * @param <R> 字段类型
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}