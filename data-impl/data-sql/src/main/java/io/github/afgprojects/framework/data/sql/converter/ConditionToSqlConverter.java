package io.github.afgprojects.framework.data.sql.converter;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.entity.EncryptedFieldMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
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
 * <p>
 * 支持加密字段的盲索引查询：当条件字段标注了 {@code @EncryptedField(blindIndexColumn=...)} 时，
 * 自动将字段名替换为盲索引列名，将条件值替换为 HMAC 哈希值。
 * <p>
 * 加密字段仅支持 EQ、NE、IN、NOT_IN 操作符，
 * 其他操作符（LIKE、BETWEEN、GT 等）会抛出 {@link BusinessException}。
 */
public class ConditionToSqlConverter {

    /** 方言，用于生成数据库兼容的 JSON 操作 SQL（强制要求） */
    private final @NonNull Dialect dialect;

    /** 实体元数据（可选，用于加密字段盲索引查询） */
    private final @Nullable EntityMetadata<?> metadata;

    /** 盲索引提供者（可选，用于加密字段盲索引查询） */
    private final @Nullable BlindIndexProvider blindIndexProvider;

    /** 操作符处理器映射 */
    private final Map<Operator, BiConsumer<StringBuilder, List<Object>>> simpleOperators = Map.of(
            Operator.EQ, (sql, params) -> { sql.append(" = ?"); },
            Operator.NE, (sql, params) -> { sql.append(" != ?"); },
            Operator.GT, (sql, params) -> { sql.append(" > ?"); },
            Operator.GE, (sql, params) -> { sql.append(" >= ?"); },
            Operator.LT, (sql, params) -> { sql.append(" < ?"); },
            Operator.LE, (sql, params) -> { sql.append(" <= ?"); }
    );

    /**
     * 创建不带加密支持的转换器（向后兼容）
     *
     * @param dialect 数据库方言
     */
    public ConditionToSqlConverter(@NonNull Dialect dialect) {
        this.dialect = dialect;
        this.metadata = null;
        this.blindIndexProvider = null;
    }

    /**
     * 创建带加密支持的转换器
     *
     * @param dialect             数据库方言
     * @param metadata            实体元数据（用于检查字段是否为加密字段）
     * @param blindIndexProvider  盲索引提供者（用于计算 HMAC 值）
     */
    public ConditionToSqlConverter(@NonNull Dialect dialect,
                                   @Nullable EntityMetadata<?> metadata,
                                   @Nullable BlindIndexProvider blindIndexProvider) {
        this.dialect = dialect;
        this.metadata = metadata;
        this.blindIndexProvider = blindIndexProvider;
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

        // 检查是否为加密字段盲索引查询
        if (isEncryptedFieldWithBlindIndex(field) && operatorRequiresValue(operator)) {
            handleEncryptedFieldCriteria(field, operator, sql, parameters, value);
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
     * 检查字段是否为具有盲索引的加密字段
     */
    private boolean isEncryptedFieldWithBlindIndex(String field) {
        if (metadata == null || blindIndexProvider == null) {
            return false;
        }
        // field 是列名，需要将其转换为属性名
        String propertyName = resolvePropertyName(field);
        if (propertyName == null) {
            return false;
        }
        EncryptedFieldMetadata efm = metadata.getEncryptedField(propertyName);
        return efm != null && efm.hasBlindIndex();
    }

    /**
     * 从列名解析属性名
     * <p>
     * 由于 Conditions.builder() 会通过 FieldNameResolver 将 Lambda 属性名转换为列名，
     * condition 中存储的是列名。我们需要反向查找属性名以匹配加密字段元数据。
     */
    private String resolvePropertyName(String columnName) {
        if (metadata == null) {
            return null;
        }
        var columnToField = metadata.getColumnToFieldMap();
        if (columnToField.containsKey(columnName)) {
            return columnToField.get(columnName);
        }
        // 也尝试直接匹配属性名（某些情况下 field 仍为属性名）
        if (metadata.getField(columnName) != null) {
            return columnName;
        }
        return null;
    }

    /**
     * 检查操作符是否需要值（IS_NULL / IS_NOT_NULL 不需要值）
     */
    private boolean operatorRequiresValue(Operator operator) {
        return operator != Operator.IS_NULL && operator != Operator.IS_NOT_NULL;
    }

    /**
     * 处理加密字段的条件转换
     * <p>
     * 对于等值查询（EQ/IN）：将条件值转为 HMAC，将字段名替换为盲索引列名
     * 对于其他操作符：抛出异常（加密字段不支持 LIKE/范围查询）
     */
    private void handleEncryptedFieldCriteria(String field, Operator operator,
                                               StringBuilder sql, List<Object> parameters,
                                               Object value) {
        String propertyName = resolvePropertyName(field);
        EncryptedFieldMetadata efm = metadata.getEncryptedField(propertyName);
        String blindIndexColumn = efm.blindIndexColumn();

        switch (operator) {
            case EQ -> {
                sql.append(dialect.quoteIdentifier(blindIndexColumn)).append(" = ?");
                String blindIndex = blindIndexProvider.computeBlindIndex(
                    value.toString(), propertyName, efm.keyRef());
                parameters.add(blindIndex);
            }
            case NE -> {
                sql.append(dialect.quoteIdentifier(blindIndexColumn)).append(" != ?");
                String blindIndex = blindIndexProvider.computeBlindIndex(
                    value.toString(), propertyName, efm.keyRef());
                parameters.add(blindIndex);
            }
            case IN -> {
                sql.append(dialect.quoteIdentifier(blindIndexColumn)).append(" IN (");
                if (value instanceof Iterable<?> iterable) {
                    boolean first = true;
                    for (Object v : iterable) {
                        if (!first) sql.append(", ");
                        sql.append("?");
                        String blindIndex = blindIndexProvider.computeBlindIndex(
                            v.toString(), propertyName, efm.keyRef());
                        parameters.add(blindIndex);
                        first = false;
                    }
                    if (first) {
                        sql.append("NULL");
                    }
                } else {
                    throw new IllegalArgumentException(
                        "IN operator requires an Iterable value for encrypted field '"
                        + propertyName + "', got: " + value.getClass().getName());
                }
                sql.append(")");
            }
            case NOT_IN -> {
                sql.append(dialect.quoteIdentifier(blindIndexColumn)).append(" NOT IN (");
                if (value instanceof Iterable<?> iterable) {
                    boolean first = true;
                    for (Object v : iterable) {
                        if (!first) sql.append(", ");
                        sql.append("?");
                        String blindIndex = blindIndexProvider.computeBlindIndex(
                            v.toString(), propertyName, efm.keyRef());
                        parameters.add(blindIndex);
                        first = false;
                    }
                    if (first) {
                        sql.append("NULL");
                    }
                } else {
                    throw new IllegalArgumentException(
                        "NOT_IN operator requires an Iterable value for encrypted field '"
                        + propertyName + "', got: " + value.getClass().getName());
                }
                sql.append(")");
            }
            default -> throw new BusinessException(CommonErrorCode.ENCRYPTED_FIELD_QUERY_NOT_SUPPORTED,
                "Encrypted field '" + propertyName + "' does not support operator '" + operator.getSymbol()
                + "'. Encrypted fields only support EQ, NE, IN, and NOT_IN operators "
                + "when blind index is configured.");
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