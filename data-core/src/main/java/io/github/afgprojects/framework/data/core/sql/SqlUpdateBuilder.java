package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * SQL 更新构建器
 */
public interface SqlUpdateBuilder {

    /**
     * 指定更新的表
     */
    @NonNull SqlUpdateBuilder table(@NonNull String table);

    /**
     * 设置更新值
     */
    @NonNull SqlUpdateBuilder set(@NonNull String column, Object value);

    /**
     * 设置更新值（批量）
     */
    @NonNull SqlUpdateBuilder set(@NonNull Map<String, Object> values);

    /**
     * 设置更新值（Lambda 方式）
     */
    @NonNull SqlUpdateBuilder set(@NonNull SFunction<?, ?> getter, Object value);

    /**
     * 设置 WHERE 条件
     */
    @NonNull SqlUpdateBuilder where(@NonNull Condition condition);

    /**
     * 构建 SQL 字符串
     */
    @NonNull String toSql();

    /**
     * 获取参数值
     */
    @NonNull List<Object> getParameters();

    /**
     * 执行更新
     */
    int execute();
}