package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * SQL 删除构建器
 */
public interface SqlDeleteBuilder {

    /**
     * 指定删除的表
     */
    @NonNull SqlDeleteBuilder from(@NonNull String table);

    /**
     * 设置 WHERE 条件
     */
    @NonNull SqlDeleteBuilder where(@NonNull Condition condition);

    /**
     * 构建 SQL 字符串
     */
    @NonNull String toSql();

    /**
     * 获取参数值
     */
    @NonNull List<Object> getParameters();

    /**
     * 执行删除
     */
    int execute();
}