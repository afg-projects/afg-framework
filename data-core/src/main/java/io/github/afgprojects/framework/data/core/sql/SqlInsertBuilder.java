package io.github.afgprojects.framework.data.core.sql;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * SQL 插入构建器
 */
public interface SqlInsertBuilder {

    /**
     * 指定插入的表
     */
    @NonNull SqlInsertBuilder into(@NonNull String table);

    /**
     * 指定插入的列
     */
    @NonNull SqlInsertBuilder columns(@NonNull String... columns);

    /**
     * 设置插入值
     */
    @NonNull SqlInsertBuilder values(@NonNull Object... values);

    /**
     * 添加一行数据（批量插入）
     */
    @NonNull SqlInsertBuilder row(@NonNull Object... values);

    /**
     * 构建 SQL 字符串
     */
    @NonNull String toSql();

    /**
     * 获取参数值
     */
    @NonNull List<Object> getParameters();

    /**
     * 执行插入
     */
    int execute();

    /**
     * 执行插入并返回生成的主键
     */
    long executeAndReturnKey();
}