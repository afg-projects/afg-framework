package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.SFunction;

/**
 * 类型化排序构建器（Lambda 字段引用）
 * <p>
 * 提供类型安全的排序构建方式，避免字段名拼写错误。
 * <p>
 * 使用示例：
 * <pre>
 * Sort sort = Sort.builder(User.class)
 *     .asc(User::getCreateTime)
 *     .desc(User::getStatus)
 *     .build();
 * </pre>
 *
 * @param <T> 实体类型
 */
public interface TypedSortBuilder<T> {

    /**
     * 添加升序排序
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 构建器（支持链式调用）
     */
    <R> TypedSortBuilder<T> asc(SFunction<T, R> getter);

    /**
     * 添加降序排序
     *
     * @param getter 字段 getter 方法引用
     * @param <R>    字段类型
     * @return 构建器（支持链式调用）
     */
    <R> TypedSortBuilder<T> desc(SFunction<T, R> getter);

    /**
     * 构建排序对象
     *
     * @return 排序对象
     */
    Sort build();
}
