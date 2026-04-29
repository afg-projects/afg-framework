package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.Nullable;

/**
 * 类型化条件构建器（Lambda 字段引用）
 *
 * @param <T> 实体类型
 */
public interface TypedConditionBuilder<T> {

    <R> TypedConditionBuilder<T> eq(SFunction<T, R> getter, @Nullable Object value);
    <R> TypedConditionBuilder<T> ne(SFunction<T, R> getter, @Nullable Object value);
    <R extends Comparable<?>> TypedConditionBuilder<T> gt(SFunction<T, R> getter, @Nullable R value);
    <R extends Comparable<?>> TypedConditionBuilder<T> ge(SFunction<T, R> getter, @Nullable R value);
    <R extends Comparable<?>> TypedConditionBuilder<T> lt(SFunction<T, R> getter, @Nullable R value);
    <R extends Comparable<?>> TypedConditionBuilder<T> le(SFunction<T, R> getter, @Nullable R value);
    TypedConditionBuilder<T> like(SFunction<T, String> getter, @Nullable String value);
    TypedConditionBuilder<T> likeLeft(SFunction<T, String> getter, @Nullable String value);
    TypedConditionBuilder<T> likeRight(SFunction<T, String> getter, @Nullable String value);
    TypedConditionBuilder<T> notLike(SFunction<T, String> getter, @Nullable String value);
    <R> TypedConditionBuilder<T> in(SFunction<T, R> getter, @Nullable Iterable<?> values);
    <R> TypedConditionBuilder<T> notIn(SFunction<T, R> getter, @Nullable Iterable<?> values);
    <R> TypedConditionBuilder<T> isNull(SFunction<T, R> getter);
    <R> TypedConditionBuilder<T> isNotNull(SFunction<T, R> getter);
    <R extends Comparable<?>> TypedConditionBuilder<T> between(SFunction<T, R> getter, @Nullable R from, @Nullable R to);
    <R extends Comparable<?>> TypedConditionBuilder<T> notBetween(SFunction<T, R> getter, @Nullable R from, @Nullable R to);

    TypedConditionBuilder<T> and(Condition condition);
    TypedConditionBuilder<T> or(Condition condition);

    Condition build();
}