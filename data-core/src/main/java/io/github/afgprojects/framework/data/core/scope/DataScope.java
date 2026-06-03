package io.github.afgprojects.framework.data.core.scope;

import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
import org.jspecify.annotations.Nullable;

/**
 * 数据权限配置
 *
 * @param table           表名
 * @param column          权限过滤列名
 * @param scopeType       数据范围类型
 * @param customCondition 自定义 SQL 条件（scopeType 为 CUSTOM 时使用）
 *                        <p><b>警告：</b>此字段允许传入有限 SQL 片段，框架会进行安全验证。
 *                        <b>仅供内部框架使用</b>，禁止在业务代码中直接设置此字段。
 *                        如需自定义条件，请使用 {@link io.github.afgprojects.framework.data.core.condition.Conditions} API。
 * @param aliasPrefix     别名前缀
 */
public record DataScope(
    String table,
    String column,
    DataScopeType scopeType,
    @Nullable String customCondition,
    @Nullable String aliasPrefix
) {
    /**
     * Compact constructor：验证 customCondition 安全性
     *
     * @throws IllegalArgumentException 如果 customCondition 在非 CUSTOM 模式下设置，或包含不安全内容
     */
    public DataScope {
        if (customCondition != null && scopeType != DataScopeType.CUSTOM) {
            throw new IllegalArgumentException(
                    "customCondition can only be set when scopeType is CUSTOM, got: " + scopeType);
        }
        if (customCondition != null) {
            SqlIdentifierValidator.validateSqlConditionFragment(customCondition);
        }
    }

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
