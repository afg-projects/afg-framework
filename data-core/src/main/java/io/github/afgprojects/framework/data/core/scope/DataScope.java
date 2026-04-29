package io.github.afgprojects.framework.data.core.scope;

import org.jspecify.annotations.Nullable;

/**
 * 数据权限配置
 *
 * @param table           表名
 * @param column          权限过滤列名
 * @param scopeType       数据范围类型
 * @param customCondition 自定义 SQL 条件（scopeType 为 CUSTOM 时使用）
 * @param aliasPrefix     别名前缀
 */
public record DataScope(
    String table,
    String column,
    DataScopeType scopeType,
    @Nullable String customCondition,
    @Nullable String aliasPrefix
) {
    public static DataScope of(String table, String column, DataScopeType scopeType) {
        return new DataScope(table, column, scopeType, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String table;
        private String column;
        private DataScopeType scopeType;
        private String customCondition;
        private String aliasPrefix;

        public Builder table(String table) { this.table = table; return this; }
        public Builder column(String column) { this.column = column; return this; }
        public Builder scopeType(DataScopeType scopeType) { this.scopeType = scopeType; return this; }
        public Builder customCondition(String customCondition) { this.customCondition = customCondition; return this; }
        public Builder aliasPrefix(String aliasPrefix) { this.aliasPrefix = aliasPrefix; return this; }

        public DataScope build() {
            return new DataScope(table, column, scopeType, customCondition, aliasPrefix);
        }
    }
}
