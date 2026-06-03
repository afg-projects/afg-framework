package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * JOIN 子句构建器
 * <p>
 * 处理各种类型的 JOIN 连接
 */
public class JoinClauseBuilder {

    private final Dialect dialect;
    private final ConditionToSqlConverter conditionConverter;
    private final List<JoinClause> joins = new ArrayList<>();

    public JoinClauseBuilder(Dialect dialect, ConditionToSqlConverter conditionConverter) {
        this.dialect = dialect;
        this.conditionConverter = conditionConverter;
    }

    /**
     * JOIN 连接
     *
     * @throws IllegalArgumentException 如果表名非法
     */
    public JoinClauseBuilder join(@NonNull String table, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        joins.add(new JoinClause("JOIN", table, null, on));
        return this;
    }

    /**
     * LEFT JOIN 连接
     *
     * @throws IllegalArgumentException 如果表名非法
     */
    public JoinClauseBuilder leftJoin(@NonNull String table, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        joins.add(new JoinClause("LEFT JOIN", table, null, on));
        return this;
    }

    /**
     * RIGHT JOIN 连接
     *
     * @throws IllegalArgumentException 如果表名非法
     */
    public JoinClauseBuilder rightJoin(@NonNull String table, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        joins.add(new JoinClause("RIGHT JOIN", table, null, on));
        return this;
    }

    /**
     * INNER JOIN 连接
     *
     * @throws IllegalArgumentException 如果表名非法
     */
    public JoinClauseBuilder innerJoin(@NonNull String table, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        joins.add(new JoinClause("INNER JOIN", table, null, on));
        return this;
    }

    /**
     * JOIN 连接（带别名）
     *
     * @throws IllegalArgumentException 如果表名或别名非法
     */
    public JoinClauseBuilder join(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        SqlIdentifierValidator.validateAlias(alias);
        joins.add(new JoinClause("JOIN", table, alias, on));
        return this;
    }

    /**
     * LEFT JOIN 连接（带别名）
     *
     * @throws IllegalArgumentException 如果表名或别名非法
     */
    public JoinClauseBuilder leftJoin(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        SqlIdentifierValidator.validateTable(table);
        SqlIdentifierValidator.validateAlias(alias);
        joins.add(new JoinClause("LEFT JOIN", table, alias, on));
        return this;
    }

    /**
     * 构建 JOIN 子句 SQL
     *
     * @return JOIN 子句字符串（含前导空格）
     */
    public String build() {
        if (joins.isEmpty()) {
            return "";
        }
        StringBuilder sql = new StringBuilder();
        for (JoinClause join : joins) {
            sql.append(" ").append(join.type());
            sql.append(" ").append(dialect.quoteIdentifier(join.table()));
            if (join.alias() != null) {
                sql.append(" ").append(dialect.quoteIdentifier(join.alias()));
            }
            sql.append(" ON ").append(conditionConverter.convert(join.on()).sql());
        }
        return sql.toString();
    }

    /**
     * 是否有 JOIN 子句
     */
    public boolean hasJoins() {
        return !joins.isEmpty();
    }

    /**
     * JOIN 子句记录
     */
    record JoinClause(String type, String table, String alias, Condition on) {}
}