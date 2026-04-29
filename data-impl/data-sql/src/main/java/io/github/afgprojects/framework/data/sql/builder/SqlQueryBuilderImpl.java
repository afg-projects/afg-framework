package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.WindowFunctionBuilder;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL 查询构建器实现
 */
public class SqlQueryBuilderImpl implements SqlQueryBuilder {

    private final Dialect dialect;
    private final ConditionToSqlConverter conditionConverter;

    private List<String> selectColumns = new ArrayList<>();
    private boolean distinct = false;
    private String fromTable;
    private String fromAlias;
    private final List<JoinClause> joins = new ArrayList<>();
    private Condition whereCondition;
    private List<String> groupByColumns = new ArrayList<>();
    private Condition havingCondition;
    private Sort orderBySort;
    private Long limitValue;
    private Long offsetValue;
    private final List<ExistsClause> existsClauses = new ArrayList<>();
    private final List<CteClause> cteClauses = new ArrayList<>();
    private String pendingWindowFunction;

    public SqlQueryBuilderImpl() {
        this.dialect = new MySQLDialect();
        this.conditionConverter = new ConditionToSqlConverter();
    }

    public SqlQueryBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
        this.conditionConverter = new ConditionToSqlConverter();
    }

    /**
     * 获取当前使用的数据库方言
     *
     * @return 数据库方言
     */
    Dialect getDialect() {
        return dialect;
    }

    @Override
    public @NonNull SqlQueryBuilder select(@NonNull String... columns) {
        selectColumns = new ArrayList<>(List.of(columns));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder select(@NonNull SFunction<?, ?>... getters) {
        selectColumns = new ArrayList<>();
        for (SFunction<?, ?> getter : getters) {
            selectColumns.add(Conditions.getFieldName(getter));
        }
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder selectAll() {
        this.selectColumns = List.of("*");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder count(@NonNull String column) {
        selectColumns.add("COUNT(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder count() {
        selectColumns.add("COUNT(*)");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder countDistinct(@NonNull String column) {
        selectColumns.add("COUNT(DISTINCT " + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder sum(@NonNull String column) {
        selectColumns.add("SUM(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder avg(@NonNull String column) {
        selectColumns.add("AVG(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder max(@NonNull String column) {
        selectColumns.add("MAX(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder min(@NonNull String column) {
        selectColumns.add("MIN(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    // ==================== 窗口函数 ====================

    @Override
    public @NonNull SqlQueryBuilder rowNumber() {
        this.pendingWindowFunction = "ROW_NUMBER()";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rank() {
        this.pendingWindowFunction = "RANK()";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder denseRank() {
        this.pendingWindowFunction = "DENSE_RANK()";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lead(@NonNull String column) {
        this.pendingWindowFunction = "LEAD(" + dialect.quoteIdentifier(column) + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lead(@NonNull String column, int offset) {
        this.pendingWindowFunction = "LEAD(" + dialect.quoteIdentifier(column) + ", " + offset + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lead(@NonNull String column, int offset, Object defaultValue) {
        this.pendingWindowFunction = "LEAD(" + dialect.quoteIdentifier(column) + ", " + offset + ", " + formatDefaultValue(defaultValue) + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lag(@NonNull String column) {
        this.pendingWindowFunction = "LAG(" + dialect.quoteIdentifier(column) + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lag(@NonNull String column, int offset) {
        this.pendingWindowFunction = "LAG(" + dialect.quoteIdentifier(column) + ", " + offset + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder lag(@NonNull String column, int offset, Object defaultValue) {
        this.pendingWindowFunction = "LAG(" + dialect.quoteIdentifier(column) + ", " + offset + ", " + formatDefaultValue(defaultValue) + ")";
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rowNumberOver(@NonNull String partitionBy, @NonNull String orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("ROW_NUMBER() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rowNumberOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("ROW_NUMBER() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull String orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull String orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("DENSE_RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectColumns.add("DENSE_RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull WindowFunctionBuilder over() {
        String windowFunc = this.pendingWindowFunction != null ? this.pendingWindowFunction : "ROW_NUMBER()";
        this.pendingWindowFunction = null;
        return new WindowFunctionBuilderImpl(this, windowFunc);
    }

    /**
     * 构建 OVER 子句（字符串版本）
     */
    private String buildOverClause(String partitionBy, String orderBy) {
        StringBuilder over = new StringBuilder("OVER (");
        if (partitionBy != null && !partitionBy.isBlank()) {
            over.append("PARTITION BY ").append(quoteIdentifiers(partitionBy));
            if (orderBy != null && !orderBy.isBlank()) {
                over.append(" ");
            }
        }
        if (orderBy != null && !orderBy.isBlank()) {
            over.append("ORDER BY ").append(quoteIdentifiers(orderBy));
        }
        over.append(")");
        return over.toString();
    }

    /**
     * 合法标识符正则：字母/下划线开头，后跟字母/数字/下划线/点号
     */
    private static final java.util.regex.Pattern VALID_IDENTIFIER_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    /**
     * 对逗号分隔的标识符列表进行引用
     * 例如: "col1, col2" -> "`col1`, `col2`"
     *
     * @throws IllegalArgumentException 如果标识符包含非法字符
     */
    private String quoteIdentifiers(String identifiers) {
        String[] parts = identifiers.split("\\s*,\\s*");
        List<String> quoted = new ArrayList<>();
        for (String part : parts) {
            // 处理可能包含 ASC/DESC 的情况
            String trimmed = part.trim();
            String upper = trimmed.toUpperCase();
            if (upper.endsWith(" ASC") || upper.endsWith(" DESC")) {
                int spaceIndex = trimmed.lastIndexOf(' ');
                String column = trimmed.substring(0, spaceIndex);
                String direction = trimmed.substring(spaceIndex + 1);
                validateIdentifier(column);
                quoted.add(dialect.quoteIdentifier(column) + " " + direction);
            } else {
                validateIdentifier(trimmed);
                quoted.add(dialect.quoteIdentifier(trimmed));
            }
        }
        return String.join(", ", quoted);
    }

    /**
     * 验证标识符合法性，防止 SQL 注入
     *
     * @param identifier 标识符
     * @throws IllegalArgumentException 如果标识符非法
     */
    private void validateIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (!VALID_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid identifier: '" + identifier + "'. " +
                    "Identifier must start with a letter or underscore, " +
                    "followed by letters, digits, underscores, or dots.");
        }
    }

    /**
     * 构建 OVER 子句（Sort 版本）
     */
    private String buildOverClause(String partitionBy, Sort orderBy) {
        StringBuilder over = new StringBuilder("OVER (");
        if (partitionBy != null && !partitionBy.isBlank()) {
            over.append("PARTITION BY ").append(quoteIdentifiers(partitionBy));
            if (orderBy != null && orderBy.isSorted()) {
                over.append(" ");
            }
        }
        if (orderBy != null && orderBy.isSorted()) {
            over.append("ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (Sort.Order order : orderBy.getOrders()) {
                orderParts.add(dialect.quoteIdentifier(order.getProperty()) + " " + order.getDirection().getSymbol());
            }
            over.append(String.join(", ", orderParts));
        }
        over.append(")");
        return over.toString();
    }

    /**
     * 格式化默认值
     */
    private String formatDefaultValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    /**
     * 添加窗口函数表达式到 selectColumns
     * 由 WindowFunctionBuilderImpl 调用
     */
    void addWindowFunctionExpression(String expression) {
        selectColumns.add(expression);
    }

    /**
     * 刷新待处理的窗口函数（添加到 selectColumns）
     */
    private void flushPendingWindowFunction() {
        if (pendingWindowFunction != null) {
            selectColumns.add(pendingWindowFunction);
            pendingWindowFunction = null;
        }
    }

    @Override
    public @NonNull SqlQueryBuilder from(@NonNull String table) {
        flushPendingWindowFunction();
        this.fromTable = table;
        this.fromAlias = null;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder from(@NonNull String table, @NonNull String alias) {
        flushPendingWindowFunction();
        this.fromTable = table;
        this.fromAlias = alias;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull Condition on) {
        joins.add(new JoinClause("JOIN", table, null, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull Condition on) {
        joins.add(new JoinClause("LEFT JOIN", table, null, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rightJoin(@NonNull String table, @NonNull Condition on) {
        joins.add(new JoinClause("RIGHT JOIN", table, null, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder innerJoin(@NonNull String table, @NonNull Condition on) {
        joins.add(new JoinClause("INNER JOIN", table, null, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        joins.add(new JoinClause("JOIN", table, alias, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        joins.add(new JoinClause("LEFT JOIN", table, alias, on));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder where(@NonNull Condition condition) {
        this.whereCondition = condition;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder and(@NonNull Condition condition) {
        if (this.whereCondition == null) {
            this.whereCondition = condition;
        } else {
            this.whereCondition = this.whereCondition.and(condition);
        }
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder or(@NonNull Condition condition) {
        if (this.whereCondition == null) {
            this.whereCondition = condition;
        } else {
            this.whereCondition = this.whereCondition.or(condition);
        }
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder groupBy(@NonNull String... columns) {
        this.groupByColumns = new ArrayList<>(List.of(columns));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder having(@NonNull Condition condition) {
        this.havingCondition = condition;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder orderBy(@NonNull Sort sort) {
        this.orderBySort = sort;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction) {
        this.orderBySort = direction == Sort.Direction.ASC ? Sort.asc(column) : Sort.desc(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder limit(long limit) {
        this.limitValue = limit;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder offset(long offset) {
        this.offsetValue = offset;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder page(long page, long size) {
        this.offsetValue = (page - 1) * size;
        this.limitValue = size;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder page(@NonNull PageRequest pageable) {
        this.offsetValue = pageable.offset();
        this.limitValue = (long) pageable.size();
        if (pageable.hasSort()) {
            this.orderBySort = pageable.sort();
        }
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder fromSubquery(@NonNull SqlQueryBuilder subquery, @NonNull String alias) {
        // 简化实现
        this.fromTable = "(" + subquery.toSql() + ")";
        this.fromAlias = alias;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder exists(@NonNull SqlQueryBuilder subquery) {
        existsClauses.add(new ExistsClause("EXISTS", subquery));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder notExists(@NonNull SqlQueryBuilder subquery) {
        existsClauses.add(new ExistsClause("NOT EXISTS", subquery));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder with(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, null, cte, false));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withRecursive(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, null, cte, true));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, columns, cte, false));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withRecursiveColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, columns, cte, true));
        return this;
    }

    @Override
    public @NonNull String toSql() {
        // 刷新待处理的窗口函数
        flushPendingWindowFunction();

        StringBuilder sql = new StringBuilder();

        // WITH (CTE) - 放在 SELECT 之前
        if (!cteClauses.isEmpty()) {
            sql.append(buildWithClause());
        }

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }

        // FROM
        sql.append(" FROM ").append(dialect.quoteIdentifier(fromTable));
        if (fromAlias != null) {
            sql.append(" ").append(fromAlias);
        }

        // JOINs
        for (JoinClause join : joins) {
            sql.append(" ").append(join.type());
            sql.append(" ").append(dialect.quoteIdentifier(join.table()));
            if (join.alias() != null) {
                sql.append(" ").append(join.alias());
            }
            sql.append(" ON ").append(conditionConverter.convert(join.on()).sql());
        }

        // WHERE
        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter.SqlResult result = conditionConverter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
            // EXISTS / NOT EXISTS 子查询
            appendExistsClauses(sql, true);
        } else if (!existsClauses.isEmpty()) {
            // 只有 EXISTS 条件，没有其他 WHERE 条件
            sql.append(" WHERE ");
            appendExistsClauses(sql, false);
        }

        // GROUP BY
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ");
            List<String> quotedColumns = new ArrayList<>();
            for (String column : groupByColumns) {
                quotedColumns.add(dialect.quoteIdentifier(column));
            }
            sql.append(String.join(", ", quotedColumns));
        }

        // HAVING
        if (havingCondition != null && !havingCondition.isEmpty()) {
            ConditionToSqlConverter.SqlResult result = conditionConverter.convert(havingCondition);
            sql.append(" HAVING ").append(result.sql());
        }

        // ORDER BY
        if (orderBySort != null && orderBySort.isSorted()) {
            sql.append(" ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (Sort.Order order : orderBySort.getOrders()) {
                orderParts.add(dialect.quoteIdentifier(order.getProperty()) + " " + order.getDirection().getSymbol());
            }
            sql.append(String.join(", ", orderParts));
        }

        // LIMIT / OFFSET
        if (limitValue != null) {
            sql.append(" LIMIT ").append(limitValue);
        }
        if (offsetValue != null) {
            sql.append(" OFFSET ").append(offsetValue);
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        List<Object> params = new ArrayList<>();

        // CTE 参数
        for (CteClause cteClause : cteClauses) {
            params.addAll(cteClause.cte().getParameters());
        }

        // WHERE 参数
        if (whereCondition != null && !whereCondition.isEmpty()) {
            params.addAll(conditionConverter.convert(whereCondition).parameters());
        }

        // EXISTS / NOT EXISTS 子查询参数
        for (ExistsClause existsClause : existsClauses) {
            params.addAll(existsClause.subquery().getParameters());
        }

        // HAVING 参数
        if (havingCondition != null && !havingCondition.isEmpty()) {
            params.addAll(conditionConverter.convert(havingCondition).parameters());
        }

        return params;
    }

    @Override
    public <T> @NonNull List<T> fetch(@NonNull Class<T> resultType) {
        // 需要 DataManager 支持
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchOne(@NonNull Class<T> resultType) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchFirst(@NonNull Class<T> resultType) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Page<T> fetchPage(@NonNull Class<T> resultType, @NonNull PageRequest pageable) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public long fetchCount() {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    private record JoinClause(String type, String table, String alias, Condition on) {}
    private record ExistsClause(String type, SqlQueryBuilder subquery) {}
    private record CteClause(String name, String[] columns, SqlQueryBuilder cte, boolean recursive) {}

    /**
     * 拼接 EXISTS / NOT EXISTS 子查询
     *
     * @param sql       SQL 构建器
     * @param hasWhere  是否已有 WHERE 条件
     */
    private void appendExistsClauses(StringBuilder sql, boolean hasWhere) {
        for (int i = 0; i < existsClauses.size(); i++) {
            ExistsClause clause = existsClauses.get(i);
            if (hasWhere || i > 0) {
                sql.append(" AND ");
            }
            sql.append(clause.type()).append(" (").append(clause.subquery().toSql()).append(")");
        }
    }

    /**
     * 构建 WITH 子句（CTE）
     *
     * @return WITH 子句字符串
     */
    private String buildWithClause() {
        StringBuilder sb = new StringBuilder();

        // 检查是否有递归 CTE
        boolean hasRecursive = cteClauses.stream().anyMatch(CteClause::recursive);

        sb.append("WITH ");
        if (hasRecursive) {
            sb.append("RECURSIVE ");
        }

        for (int i = 0; i < cteClauses.size(); i++) {
            CteClause cte = cteClauses.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(dialect.quoteIdentifier(cte.name()));
            // 添加列名（如果有）
            if (cte.columns() != null && cte.columns().length > 0) {
                List<String> quotedColumns = new ArrayList<>();
                for (String column : cte.columns()) {
                    quotedColumns.add(dialect.quoteIdentifier(column));
                }
                sb.append("(").append(String.join(", ", quotedColumns)).append(")");
            }
            sb.append(" AS (").append(cte.cte().toSql()).append(")");
        }
        sb.append(" ");

        return sb.toString();
    }
}
