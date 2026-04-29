package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * SQL 解析器接口
 */
public interface SqlParser {

    /**
     * 解析 SQL 为 AST
     *
     * @param sql SQL 字符串
     * @return SQL 语句对象
     */
    @NonNull SqlStatement parse(@NonNull String sql);

    /**
     * 改写 SQL（用于数据权限、多租户等场景）
     *
     * @param sql    原始 SQL
     * @param context 改写上下文
     * @return 改写后的 SQL
     */
    @NonNull String rewrite(@NonNull String sql, @NonNull SqlRewriteContext context);

    /**
     * 验证 SQL 语法
     *
     * @param sql SQL 字符串
     * @return 是否有效
     */
    boolean validate(@NonNull String sql);
}