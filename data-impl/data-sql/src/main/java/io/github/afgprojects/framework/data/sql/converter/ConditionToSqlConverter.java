package io.github.afgprojects.framework.data.sql.converter;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.DenyAllCondition;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.NotCondition;
import io.github.afgprojects.framework.data.core.query.Operator;
import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Condition 到 SQL WHERE 子句的转换器
 */
public class ConditionToSqlConverter {

    /** 方言，用于生成数据库兼容的 JSON 操作 SQL（强制要求） */
    private final @NonNull Dialect dialect;

    /** 操作符处理器映射 */
    private final Map<Operator, BiConsumer<StringBuilder, List<Object>>> simpleOperators = Map.of(
            Operator.EQ, (sql, params) -> { sql.append(" = ?"); },
            Operator.NE, (sql, params) -> { sql.append(" != ?"); },
            Operator.GT, (sql, params) -> { sql.append(" > ?"); },
            Operator.GE, (sql, params) -> { sql.append(" >= ?"); },
            Operator.LT, (sql, params) -> { sql.append(" < ?"); },
            Operator.LE, (sql, params) -> { sql.append(" <= ?"); }
    );

    public ConditionToSqlConverter(@NonNull Dialect dialect) {
        this.dialect = dialect;
    }

    /**
     * 验证字段名是否合法，防止 SQL 注入
     *
     * @param field 字段名
     * @throws IllegalArgumentException 如果字段名不合法
     */
    private void validateFieldName(String field) {
        SqlIdentifierValidator.validateColumn(field);
    }


    /**
     * 将 Condition 转换为 WHERE 子句 SQL
     *
     * @param condition 查询条件
     * @return SQL 结果（包含 SQL 和参数）
     */
    public @NonNull SqlResult convert(@NonNull Condition condition) {
        if (condition.isEmpty()) {
            return SqlResult.empty();
        }

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        convertCondition(condition, sql, parameters);

        return new SqlResult(sql.toString(), parameters);
    }

    private void convertCondition(Condition condition, StringBuilder sql, List<Object> parameters) {
        // 处理 DENY_ALL 条件（永假条件，生成 1 = 0）
        if (condition instanceof DenyAllCondition) {
            sql.append("1 = 0");
            return;
        }

        // 处理 NOT 条件（始终用括号包裹，避免优先级问题）
        if (condition instanceof NotCondition notCondition) {
            sql.append("NOT (");
            convertCondition(notCondition.getOriginal(), sql, parameters);
            sql.append(")");
            return;
        }

        List<Criterion> criteria = condition.getCriteria();
        if (criteria.isEmpty()) {
            return;
        }

        boolean needsParentheses = criteria.size() > 1;
        if (needsParentheses) {
            sql.append("(");
        }

        LogicalOperator conditionOperator = condition.getOperator();

        for (int i = 0; i < criteria.size(); i++) {
            Criterion criterion = criteria.get(i);

            if (i > 0) {
                LogicalOperator nextOp = criteria.get(i - 1).nextOperator();
                if (nextOp != null) {
                    sql.append(" ").append(nextOp.getSymbol()).append(" ");
                } else {
                    sql.append(" ").append(conditionOperator.getSymbol()).append(" ");
                }
            }

            convertCriterion(criterion, sql, parameters);
        }

        if (needsParentheses) {
            sql.append(")");
        }
    }

    private void convertCriterion(Criterion criterion, StringBuilder sql, List<Object> parameters) {
        String field = criterion.field();
        Operator operator = criterion.operator();
        Object value = criterion.value();

        // 处理嵌套条件
        if (criterion.isNested()) {
            if (criterion.isNegated()) {
                // NOT 嵌套条件
                sql.append("NOT (");
                convertCondition(criterion.nestedCondition(), sql, parameters);
                sql.append(")");
            } else {
                sql.append("(");
                convertCondition(criterion.nestedCondition(), sql, parameters);
                sql.append(")");
            }
            return;
        }

        // 安全检查：验证字段名只包含合法字符，防止 SQL 注入
        validateFieldName(field);

        // JSON 操作符需要委托给 Dialect 生成（列名是表达式的一部分）
        if (operator == Operator.JSON_CONTAINS || operator == Operator.JSON_CONTAINED || operator == Operator.JSON_PATH) {
            handleJsonOperator(field, operator, sql, parameters, value);
            return;
        }

        // 直接使用字段名（已由 FieldNameResolver 在 Conditions 中转换为列名）
        sql.append(field);

        // 处理简单比较操作符
        if (handleSimpleOperator(operator, sql, parameters, value)) {
            return;
        }

        // 处理特殊操作符
        switch (operator) {
            case LIKE, LIKE_STARTS_WITH, LIKE_ENDS_WITH -> handleLike(sql, parameters, value, false);
            case NOT_LIKE -> handleLike(sql, parameters, value, true);
            case IN -> handleIn(sql, parameters, value, false);
            case NOT_IN -> handleIn(sql, parameters, value, true);
            case IS_NULL -> sql.append(" IS NULL");
            case IS_NOT_NULL -> sql.append(" IS NOT NULL");
            case BETWEEN -> handleBetween(sql, parameters, value, false);
            case NOT_BETWEEN -> handleBetween(sql, parameters, value, true);
            default -> { /* 其他操作符已在 handleSimpleOperator 中处理 */ }
        }
    }

    /**
     * 使用 Dialect 处理 JSON 操作符，生成数据库兼容的 SQL 表达式
     */
    private void handleJsonOperator(String field, Operator operator, StringBuilder sql, List<Object> parameters, Object value) {
        switch (operator) {
            case JSON_CONTAINS -> sql.append(dialect.getJsonContainsExpression(field));
            case JSON_CONTAINED -> sql.append(dialect.getJsonContainedExpression(field));
            case JSON_PATH -> sql.append(dialect.getJsonPathExpression(field));
            default -> throw new UnsupportedOperationException(
                "JSON operators require a Dialect to generate database-specific SQL. " +
                "Please provide a Dialect when creating ConditionToSqlConverter."
            );
        }
        parameters.add(value);
    }

    /**
     * 处理简单比较操作符（EQ, NE, GT, GE, LT, LE）
     */
    private boolean handleSimpleOperator(Operator operator, StringBuilder sql, List<Object> parameters, Object value) {
        BiConsumer<StringBuilder, List<Object>> handler = simpleOperators.get(operator);
        if (handler != null) {
            handler.accept(sql, parameters);
            parameters.add(value);
            return true;
        }
        return false;
    }

    /**
     * 处理 LIKE 操作符
     * <p>
     * 使用 ESCAPE 子句支持转义通配符。值中的转义字符（如 {@code !%} 和 {@code !_}）
     * 已在条件构建阶段由 {@link Conditions#escapeLikeWildcards(String)} 处理。
     */
    private void handleLike(StringBuilder sql, List<Object> parameters, Object value, boolean negate) {
        sql.append(negate ? " NOT LIKE ? ESCAPE '!'" : " LIKE ? ESCAPE '!'");
        parameters.add(value);
    }

    /**
     * 处理 IN 操作符
     */
    /**
     * 处理 IN 操作符
     *
     * @throws IllegalArgumentException 如果 value 不是 Iterable
     */
    private void handleIn(StringBuilder sql, List<Object> parameters, Object value, boolean negate) {
        if (!(value instanceof Iterable<?> iterable)) {
            throw new IllegalArgumentException(
                    "IN/NOT IN operator requires an Iterable value, got: " + value.getClass().getName());
        }

        sql.append(negate ? " NOT IN (" : " IN (");

        boolean first = true;
        for (Object v : iterable) {
            if (!first) sql.append(", ");
            sql.append("?");
            parameters.add(v);
            first = false;
        }

        // 空 Iterable 时生成 IN (NULL)，避免语法错误
        if (first) {
            sql.append("NULL");
        }

        sql.append(")");
    }

    /**
     * 处理 BETWEEN 操作符
     */
    private void handleBetween(StringBuilder sql, List<Object> parameters, Object value, boolean negate) {
        if (!(value instanceof Comparable<?>[] arr) || arr.length != 2) {
            throw new IllegalArgumentException(
                    "BETWEEN/NOT BETWEEN requires a Comparable array of length 2, got: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        sql.append(negate ? " NOT BETWEEN ? AND ?" : " BETWEEN ? AND ?");
        parameters.add(arr[0]);
        parameters.add(arr[1]);
    }

    /**
     * SQL 转换结果
     *
     * @param sql        SQL 字符串
     * @param parameters 参数列表
     */
    public record SqlResult(String sql, @NonNull List<Object> parameters) {

        public static SqlResult empty() {
            return new SqlResult("", List.of());
        }

        public boolean isEmpty() {
            return sql == null || sql.isEmpty();
        }

        public @NonNull String getWhereClause() {
            if (sql == null || sql.isEmpty()) {
                return "";
            }
            return "WHERE " + sql;
        }
    }
}