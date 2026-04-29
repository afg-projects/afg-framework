package io.github.afgprojects.framework.data.core.sql;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * SQL 改写上下文
 * <p>
 * 用于数据权限、多租户等场景的 SQL 改写
 */
public interface SqlRewriteContext {

    /**
     * 获取数据库类型
     */
    @NonNull DatabaseType getDatabaseType();

    /**
     * 获取数据权限配置列表
     */
    @NonNull List<DataScope> getDataScopes();

    /**
     * 获取租户ID
     */
    @Nullable String getTenantId();

    /**
     * 是否需要软删除过滤
     */
    boolean isSoftDeleteFilter();

    /**
     * 获取软删除字段名
     */
    @Nullable String getSoftDeleteColumn();

    /**
     * 获取软删除值（已删除）
     */
    @Nullable String getDeletedValue();

    /**
     * 是否忽略数据权限
     */
    boolean isIgnoreDataScope();

    /**
     * 是否忽略多租户
     */
    boolean isIgnoreTenant();

    /**
     * 获取表别名映射
     * <p>
     * 用于数据权限注入时确定表别名
     */
    @Nullable String getTableAlias(@NonNull String tableName);
}