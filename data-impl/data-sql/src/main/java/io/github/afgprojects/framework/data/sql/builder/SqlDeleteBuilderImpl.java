package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 删除构建器实现
 */
public class SqlDeleteBuilderImpl implements SqlDeleteBuilder {

    /**
     * 合法标识符正则：字母/下划线开头，后跟字母/数字/下划线
     */
    private static final java.util.regex.Pattern VALID_IDENTIFIER_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final Dialect dialect;
    private String tableName;
    private Condition whereCondition;
    private final ConditionToSqlConverter conditionConverter = new ConditionToSqlConverter();

    public SqlDeleteBuilderImpl() {
        this.dialect = new MySQLDialect();
    }

    public SqlDeleteBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public @NonNull SqlDeleteBuilder from(@NonNull String table) {
        validateIdentifier(table, "table name");
        this.tableName = table;
        return this;
    }

    @Override
    public @NonNull SqlDeleteBuilder where(@NonNull Condition condition) {
        this.whereCondition = condition;
        return this;
    }

    @Override
    public @NonNull String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(dialect.quoteIdentifier(tableName));

        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter.SqlResult result = conditionConverter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        if (whereCondition != null && !whereCondition.isEmpty()) {
            return new ArrayList<>(conditionConverter.convert(whereCondition).parameters());
        }
        return List.of();
    }

    @Override
    public int execute() {
        throw new UnsupportedOperationException("Use DataManager to execute delete");
    }

    /**
     * 验证标识符合法性，防止 SQL 注入
     *
     * @param identifier 标识符
     * @param type       标识符类型描述（用于错误消息）
     * @throws IllegalArgumentException 如果标识符非法
     */
    private void validateIdentifier(String identifier, String type) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException(type + " cannot be null or empty");
        }
        if (!VALID_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid " + type + ": '" + identifier + "'. " +
                    "Identifier must start with a letter or underscore, " +
                    "followed by letters, digits, or underscores.");
        }
    }
}