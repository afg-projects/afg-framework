package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public interface ProjectedQuery<T, R> {

    @NonNull ProjectedQuery<T, R> select(@NonNull SFunction<T, ?>... getters);

    @NonNull ProjectedQuery<T, R> select(@NonNull String... fields);

    @NonNull ProjectedQuery<T, R> where(@NonNull Condition condition);

    @NonNull ProjectedQuery<T, R> orderBy(@NonNull Sort sort);

    @NonNull ProjectedQuery<T, R> withDataScope();

    @NonNull ProjectedQuery<T, R> withDataScope(@NonNull String deptField);

    @NonNull ProjectedQuery<T, R> withDataScope(@NonNull DataScopeType scopeType);

    @NonNull ProjectedQuery<T, R> withTenant(@NonNull String tenantId);

    @NonNull ProjectedQuery<T, R> includeDeleted();

    @NonNull ProjectedQuery<T, R> limit(int limit);

    @NonNull ProjectedQuery<T, R> offset(int offset);

    @NonNull List<R> list();

    @NonNull Optional<R> one();

    @NonNull Optional<R> first();

    @NonNull Page<R> page(@NonNull PageRequest pageRequest);

    long count();
}