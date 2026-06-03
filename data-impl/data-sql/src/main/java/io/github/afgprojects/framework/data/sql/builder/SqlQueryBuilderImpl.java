package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.mapper.ResultMapper;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.WindowFunctionBuilder;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL 查询构建器实现
 * <p>
 * 使用门面模式组合各子构建器，保持 API 简洁
 * <p>
 * <strong>线程安全说明：</strong>此类不是线程安全的。每个实例应该只在单个线程中使用。
 * 如果需要在多线程环境中使用，应该为每个线程创建独立的实例，或者使用外部同步机制。
 * <p>
 * 典型用法是在方法内部创建实例并立即使用：
 * <pre>
 * String sql = new SqlQueryBuilderImpl()
 *     .select("id", "name")
 *     .from("user")
 *     .where(Conditions.builder(User.class).eq(User::getStatus, 1).build())
 *     .toSql();
 * </pre>
 */
public class SqlQueryBuilderImpl implements SqlQueryBuilder {

    private final Dialect dialect;
    private final ConditionToSqlConverter conditionConverter;

    // 子构建器
    private final SelectClauseBuilder selectBuilder;
    private final JoinClauseBuilder joinBuilder;
    private final OrderByClauseBuilder orderByBuilder;
    private final CteClauseBuilder cteBuilder;

    // 查询状态
    private String fromTable;
    private String fromAlias;
    private Condition whereCondition;
    private List<String> groupByColumns = new ArrayList<>();
    private Condition havingCondition;
    private Long limitValue;
    private Long offsetValue;
    private final List<ExistsClause> existsClauses = new ArrayList<>();
    private String pendingWindowFunction;

    public SqlQueryBuilderImpl() {
        this(new MySQLDialect());
    }

    public SqlQueryBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
        this.conditionConverter = new ConditionToSqlConverter();
        this.selectBuilder = new SelectClauseBuilder(dialect);
        this.joinBuilder = new JoinClauseBuilder(dialect, conditionConverter);
        this.orderByBuilder = new OrderByClauseBuilder(dialect);
        this.cteBuilder = new CteClauseBuilder(dialect);
    }

    /**
     * 获取当前使用的数据库方言
     *
     * @return 数据库方言
     */
    Dialect getDialect() {
        return dialect;
    }

    // ==================== SELECT ====================

    @Override
    public @NonNull SqlQueryBuilder select(@NonNull String... columns) {
        selectBuilder.select(columns);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder select(@NonNull SFunction<?, ?>... getters) {
        selectBuilder.select(getters);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder distinct() {
        selectBuilder.distinct();
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder selectAll() {
        selectBuilder.selectAll();
        return this;
    }

    // ==================== 聚合函数 ====================

    @Override
    public @NonNull SqlQueryBuilder count(@NonNull String column) {
        selectBuilder.count(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder count() {
        selectBuilder.count();
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder countDistinct(@NonNull String column) {
        selectBuilder.countDistinct(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder sum(@NonNull String column) {
        selectBuilder.sum(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder avg(@NonNull String column) {
        selectBuilder.avg(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder max(@NonNull String column) {
        selectBuilder.max(column);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder min(@NonNull String column) {
        selectBuilder.min(column);
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
        selectBuilder.addWindowFunctionExpression("ROW_NUMBER() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rowNumberOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectBuilder.addWindowFunctionExpression("ROW_NUMBER() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull String orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectBuilder.addWindowFunctionExpression("RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rankOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectBuilder.addWindowFunctionExpression("RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull String orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectBuilder.addWindowFunctionExpression("DENSE_RANK() " + overClause);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder denseRankOver(@NonNull String partitionBy, @NonNull Sort orderBy) {
        String overClause = buildOverClause(partitionBy, orderBy);
        selectBuilder.addWindowFunctionExpression("DENSE_RANK() " + overClause);
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
                SqlIdentifierValidator.validateColumn(column);
                quoted.add(dialect.quoteIdentifier(column) + " " + direction);
            } else {
                SqlIdentifierValidator.validateColumn(trimmed);
                quoted.add(dialect.quoteIdentifier(trimmed));
            }
        }
        return String.join(", ", quoted);
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
     * <p>
     * 注意：字符串值会转义单引号以防止 SQL 注入
     */
    private String formatDefaultValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String str) {
            // 转义单引号：' -> ''
            return "'" + str.replace("'", "''") + "'";
        }
        return String.valueOf(value);
    }

    /**
     * 添加窗口函数表达式到 selectColumns
     * 由 WindowFunctionBuilderImpl 调用
     */
    void addWindowFunctionExpression(String expression) {
        selectBuilder.addWindowFunctionExpression(expression);
    }

    /**
     * 刷新待处理的窗口函数（添加到 selectColumns）
     */
    private void flushPendingWindowFunction() {
        if (pendingWindowFunction != null) {
            selectBuilder.addWindowFunctionExpression(pendingWindowFunction);
            pendingWindowFunction = null;
        }
    }

    // ==================== FROM ====================

    @Override
    public @NonNull SqlQueryBuilder from(@NonNull String table) {
        flushPendingWindowFunction();
        SqlIdentifierValidator.validateTable(table);
        this.fromTable = table;
        this.fromAlias = null;
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder from(@NonNull String table, @NonNull String alias) {
        flushPendingWindowFunction();
        SqlIdentifierValidator.validateTable(table);
        SqlIdentifierValidator.validateAlias(alias);
        this.fromTable = table;
        this.fromAlias = alias;
        return this;
    }

    // ==================== JOIN ====================

    @Override
    public @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull Condition on) {
        joinBuilder.join(table, on);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull Condition on) {
        joinBuilder.leftJoin(table, on);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder rightJoin(@NonNull String table, @NonNull Condition on) {
        joinBuilder.rightJoin(table, on);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder innerJoin(@NonNull String table, @NonNull Condition on) {
        joinBuilder.innerJoin(table, on);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder join(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        joinBuilder.join(table, alias, on);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder leftJoin(@NonNull String table, @NonNull String alias, @NonNull Condition on) {
        joinBuilder.leftJoin(table, alias, on);
        return this;
    }

    // ==================== WHERE ====================

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

    // ==================== GROUP BY / HAVING ====================

    @Override
    public @NonNull SqlQueryBuilder groupBy(@NonNull String... columns) {
        for (String column : columns) {
            SqlIdentifierValidator.validateColumn(column);
        }
        this.groupByColumns = new ArrayList<>(List.of(columns));
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder having(@NonNull Condition condition) {
        this.havingCondition = condition;
        return this;
    }

    // ==================== ORDER BY ====================

    @Override
    public @NonNull SqlQueryBuilder orderBy(@NonNull Sort sort) {
        orderByBuilder.orderBy(sort);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder orderBy(@NonNull String column, Sort.@NonNull Direction direction) {
        orderByBuilder.orderBy(column, direction);
        return this;
    }

    // ==================== LIMIT / OFFSET ====================

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
            orderByBuilder.setSort(pageable.sort());
        }
        return this;
    }

    // ==================== CTE ====================

    @Override
    public @NonNull SqlQueryBuilder with(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteBuilder.with(name, cte);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withRecursive(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteBuilder.withRecursive(name, cte);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteBuilder.withColumnNames(name, columns, cte);
        return this;
    }

    @Override
    public @NonNull SqlQueryBuilder withRecursiveColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteBuilder.withRecursiveColumnNames(name, columns, cte);
        return this;
    }

    // ==================== 子查询 ====================

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

    // ==================== 构建 ====================

    @Override
    public @NonNull String toSql() {
        // 刷新待处理的窗口函数
        flushPendingWindowFunction();

        StringBuilder sql = new StringBuilder();

        // WITH (CTE) - 放在 SELECT 之前
        if (cteBuilder.hasCtes()) {
            sql.append(cteBuilder.build());
        }

        // SELECT
        sql.append("SELECT ").append(selectBuilder.build());

        // FROM
        sql.append(" FROM ").append(dialect.quoteIdentifier(fromTable));
        if (fromAlias != null) {
            sql.append(" ").append(dialect.quoteIdentifier(fromAlias));
        }

        // JOINs
        sql.append(joinBuilder.build());

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
        sql.append(orderByBuilder.build());

        // LIMIT / OFFSET（使用 Dialect 生成兼容 SQL）
        if (limitValue != null) {
            long offset = offsetValue != null ? offsetValue : 0;
            sql = new StringBuilder(dialect.getPaginationSql(sql.toString(), offset, limitValue));
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        List<Object> params = new ArrayList<>();

        // CTE 参数
        params.addAll(cteBuilder.getParameters());

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

    // ==================== 执行 ====================

    @Override
    public <T> @NonNull List<T> fetch(@NonNull Class<T> resultType) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull List<T> fetch(@NonNull Class<T> resultType, @NonNull ResultMapper<T> mapper) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchOne(@NonNull Class<T> resultType) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchOne(@NonNull Class<T> resultType, @NonNull ResultMapper<T> mapper) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchFirst(@NonNull Class<T> resultType) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Optional<T> fetchFirst(@NonNull Class<T> resultType, @NonNull ResultMapper<T> mapper) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Page<T> fetchPage(@NonNull Class<T> resultType, @NonNull PageRequest pageable) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public <T> @NonNull Page<T> fetchPage(@NonNull Class<T> resultType, @NonNull PageRequest pageable, @NonNull ResultMapper<T> mapper) {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    @Override
    public long fetchCount() {
        throw new UnsupportedOperationException("Use DataManager to execute query");
    }

    private record ExistsClause(String type, SqlQueryBuilder subquery) {}

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
}
