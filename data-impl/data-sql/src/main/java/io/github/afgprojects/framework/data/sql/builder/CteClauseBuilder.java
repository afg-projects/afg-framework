package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * CTE (Common Table Expression) 子句构建器
 * <p>
 * 处理 WITH / WITH RECURSIVE 子句
 */
public class CteClauseBuilder {

    private final Dialect dialect;
    private final List<CteClause> cteClauses = new ArrayList<>();

    public CteClauseBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    /**
     * 添加非递归 CTE（WITH 子句）
     *
     * @param name CTE 名称
     * @param cte  CTE 查询定义
     */
    public CteClauseBuilder with(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, null, cte, false));
        return this;
    }

    /**
     * 添加递归 CTE（WITH RECURSIVE 子句）
     *
     * @param name CTE 名称
     * @param cte  CTE 查询定义
     */
    public CteClauseBuilder withRecursive(@NonNull String name, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, null, cte, true));
        return this;
    }

    /**
     * 添加带列名的 CTE（WITH name(column1, column2, ...) AS (...)）
     *
     * @param name    CTE 名称
     * @param columns 列名数组
     * @param cte     CTE 查询定义
     */
    public CteClauseBuilder withColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, columns, cte, false));
        return this;
    }

    /**
     * 添加带列名的递归 CTE
     *
     * @param name    CTE 名称
     * @param columns 列名数组
     * @param cte     CTE 查询定义
     */
    public CteClauseBuilder withRecursiveColumnNames(@NonNull String name, @NonNull String[] columns, @NonNull SqlQueryBuilder cte) {
        cteClauses.add(new CteClause(name, columns, cte, true));
        return this;
    }

    /**
     * 构建 WITH 子句 SQL
     *
     * @return WITH 子句字符串（含后导空格）
     */
    public String build() {
        if (cteClauses.isEmpty()) {
            return "";
        }
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

    /**
     * 获取所有 CTE 的参数
     */
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (CteClause cteClause : cteClauses) {
            params.addAll(cteClause.cte().getParameters());
        }
        return params;
    }

    /**
     * 是否有 CTE 子句
     */
    public boolean hasCtes() {
        return !cteClauses.isEmpty();
    }

    /**
     * CTE 子句记录
     */
    record CteClause(String name, String[] columns, SqlQueryBuilder cte, boolean recursive) {}
}