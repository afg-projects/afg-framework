package io.github.afgprojects.framework.data.sql.converter;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Condition 到 SQL WHERE 子句的转换器
 */
public class ConditionToSqlConverter {

    /** 合法字段名正则：字母/下划线开头，后跟字母/数字/下划线/点（支持 table.field 格式） */
    private static final java.util.regex.Pattern FIELD_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    /**
     * 验证字段名是否合法，防止 SQL 注入
     *
     * @param field 字段名
     * @throws IllegalArgumentException 如果字段名不合法
     */
    private void validateFieldName(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (!FIELD_NAME_PATTERN.matcher(field).matches()) {
            throw new IllegalArgumentException(
                    "Invalid field name: '" + field + "'. Field name must start with a letter or underscore, "
                    + "and contain only letters, digits, underscores, or dots.");
        }
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
        if ("__nested__".equals(field) && value instanceof Condition nestedCondition) {
            convertCondition(nestedCondition, sql, parameters);
            return;
        }

        // 安全检查：验证字段名只包含合法字符，防止 SQL 注入
        validateFieldName(field);
        sql.append(field);

        switch (operator) {
            case EQ -> {
                sql.append(" = ?");
                parameters.add(value);
            }
            case NE -> {
                sql.append(" != ?");
                parameters.add(value);
            }
            case GT -> {
                sql.append(" > ?");
                parameters.add(value);
            }
            case GE -> {
                sql.append(" >= ?");
                parameters.add(value);
            }
            case LT -> {
                sql.append(" < ?");
                parameters.add(value);
            }
            case LE -> {
                sql.append(" <= ?");
                parameters.add(value);
            }
            case LIKE, LIKE_LEFT, LIKE_RIGHT -> {
                sql.append(" LIKE ?");
                parameters.add(value);
            }
            case NOT_LIKE -> {
                sql.append(" NOT LIKE ?");
                parameters.add(value);
            }
            case IN -> {
                sql.append(" IN (");
                if (value instanceof Iterable<?> iterable) {
                    boolean first = true;
                    for (Object v : iterable) {
                        if (!first) sql.append(", ");
                        sql.append("?");
                        parameters.add(v);
                        first = false;
                    }
                }
                sql.append(")");
            }
            case NOT_IN -> {
                sql.append(" NOT IN (");
                if (value instanceof Iterable<?> iterable) {
                    boolean first = true;
                    for (Object v : iterable) {
                        if (!first) sql.append(", ");
                        sql.append("?");
                        parameters.add(v);
                        first = false;
                    }
                }
                sql.append(")");
            }
            case IS_NULL -> sql.append(" IS NULL");
            case IS_NOT_NULL -> sql.append(" IS NOT NULL");
            case BETWEEN -> {
                if (value instanceof Comparable<?>[] arr && arr.length == 2) {
                    sql.append(" BETWEEN ? AND ?");
                    parameters.add(arr[0]);
                    parameters.add(arr[1]);
                }
            }
            case NOT_BETWEEN -> {
                if (value instanceof Comparable<?>[] arr && arr.length == 2) {
                    sql.append(" NOT BETWEEN ? AND ?");
                    parameters.add(arr[0]);
                    parameters.add(arr[1]);
                }
            }
        }
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