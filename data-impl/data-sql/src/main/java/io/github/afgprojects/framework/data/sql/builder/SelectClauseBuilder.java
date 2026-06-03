package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECT 子句构建器
 * <p>
 * 处理 SELECT 列、DISTINCT、聚合函数
 */
public class SelectClauseBuilder {

    private final Dialect dialect;
    private List<String> selectColumns = new ArrayList<>();
    private boolean distinct = false;

    public SelectClauseBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    /**
     * 指定查询列
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder select(@NonNull String... columns) {
        List<String> validated = new ArrayList<>(columns.length);
        for (String column : columns) {
            // 特殊值 * 和表达式允许直接通过（如 COUNT(*) 等聚合函数结果）
            if ("*".equals(column)) {
                validated.add(column);
            } else {
                SqlIdentifierValidator.validateColumn(column);
                validated.add(dialect.quoteIdentifier(column));
            }
        }
        this.selectColumns = validated;
        return this;
    }

    /**
     * 指定查询列（Lambda 方式）
     */
    public SelectClauseBuilder select(@NonNull SFunction<?, ?>... getters) {
        selectColumns = new ArrayList<>();
        for (SFunction<?, ?> getter : getters) {
            String fieldName = Conditions.getFieldName(getter);
            selectColumns.add(dialect.quoteIdentifier(fieldName));
        }
        return this;
    }

    /**
     * 指定 DISTINCT
     */
    public SelectClauseBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * 指定查询所有列
     */
    public SelectClauseBuilder selectAll() {
        this.selectColumns = List.of("*");
        return this;
    }

    /**
     * 添加 COUNT(column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder count(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("COUNT(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加 COUNT(*) 聚合函数
     */
    public SelectClauseBuilder count() {
        selectColumns.add("COUNT(*)");
        return this;
    }

    /**
     * 添加 COUNT(DISTINCT column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder countDistinct(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("COUNT(DISTINCT " + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加 SUM(column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder sum(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("SUM(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加 AVG(column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder avg(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("AVG(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加 MAX(column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder max(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("MAX(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加 MIN(column) 聚合函数
     *
     * @throws IllegalArgumentException 如果列名非法
     */
    public SelectClauseBuilder min(@NonNull String column) {
        SqlIdentifierValidator.validateColumn(column);
        selectColumns.add("MIN(" + dialect.quoteIdentifier(column) + ")");
        return this;
    }

    /**
     * 添加窗口函数表达式
     */
    public SelectClauseBuilder addWindowFunctionExpression(String expression) {
        selectColumns.add(expression);
        return this;
    }

    /**
     * 构建 SELECT 子句 SQL
     *
     * @return SELECT 子句字符串（不含 SELECT 关键字）
     */
    public String build() {
        StringBuilder sql = new StringBuilder();
        if (distinct) {
            sql.append("DISTINCT ");
        }
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }
        return sql.toString();
    }

    /**
     * 获取 selectColumns 列表（供外部添加窗口函数表达式使用）
     */
    public List<String> getSelectColumns() {
        return selectColumns;
    }

    /**
     * 设置 selectColumns 列表
     */
    public void setSelectColumns(List<String> columns) {
        this.selectColumns = columns;
    }

    /**
     * 是否有 DISTINCT
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * 是否为空（无选择列）
     */
    public boolean isEmpty() {
        return selectColumns.isEmpty();
    }
}
