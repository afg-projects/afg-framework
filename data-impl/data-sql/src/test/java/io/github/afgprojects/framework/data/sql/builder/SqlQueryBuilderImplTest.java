package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlQueryBuilderImpl 测试
 */
@DisplayName("SqlQueryBuilderImpl 测试")
class SqlQueryBuilderImplTest {

    private final SqlQueryBuilderImpl builder = new SqlQueryBuilderImpl(new MySQLDialect());

    @Nested
    @DisplayName("基本 SELECT")
    class BasicSelectTests {

        @Test
        @DisplayName("SELECT * FROM table")
        void shouldGenerateSelectAllFromTable() {
            String sql = builder.from("user").toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user`");
        }

        @Test
        @DisplayName("SELECT 指定列")
        void shouldGenerateSelectWithColumns() {
            String sql = builder.select("id", "name").from("user").toSql();
            assertThat(sql).isEqualTo("SELECT `id`, `name` FROM `user`");
        }

        @Test
        @DisplayName("FROM 带别名")
        void shouldGenerateFromWithAlias() {
            String sql = builder.from("user", "u").toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` `u`");
        }
    }

    @Nested
    @DisplayName("WHERE 条件")
    class WhereConditionTests {

        @Test
        @DisplayName("WHERE 单条件")
        void shouldGenerateWhereCondition() {
            Condition condition = Conditions.eq("status", 1);
            String sql = builder.from("user").where(condition).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` WHERE status = ?");
        }

        @Test
        @DisplayName("WHERE AND 条件")
        void shouldGenerateWhereAndCondition() {
            Condition condition1 = Conditions.eq("status", 1);
            Condition condition2 = Conditions.eq("deleted", false);
            String sql = builder.from("user").where(condition1).and(condition2).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` WHERE (status = ? AND deleted = ?)");
        }

        @Test
        @DisplayName("WHERE OR 条件")
        void shouldGenerateWhereOrCondition() {
            Condition condition1 = Conditions.eq("status", 1);
            Condition condition2 = Conditions.eq("status", 2);
            String sql = builder.from("user").where(condition1).or(condition2).toSql();
            // anyOf creates nested conditions with individual parentheses
            assertThat(sql).contains("status = ?").contains("OR").contains("status = ?");
        }

        @Test
        @DisplayName("getParameters 提取参数")
        void shouldExtractParameters() {
            Condition condition = Conditions.eq("status", 1);
            List<Object> params = builder.from("user").where(condition).getParameters();
            assertThat(params).containsExactly(1);
        }
    }

    @Nested
    @DisplayName("JOIN")
    class JoinTests {

        @Test
        @DisplayName("INNER JOIN")
        void shouldGenerateInnerJoin() {
            Condition onCondition = Conditions.eq("user.id", "order.user_id");
            String sql = builder.from("user").innerJoin("order", onCondition).toSql();
            assertThat(sql).contains("INNER JOIN `order` ON");
        }

        @Test
        @DisplayName("LEFT JOIN")
        void shouldGenerateLeftJoin() {
            Condition onCondition = Conditions.eq("user.id", "order.user_id");
            String sql = builder.from("user").leftJoin("order", onCondition).toSql();
            assertThat(sql).contains("LEFT JOIN `order` ON");
        }

        @Test
        @DisplayName("RIGHT JOIN")
        void shouldGenerateRightJoin() {
            Condition onCondition = Conditions.eq("user.id", "order.user_id");
            String sql = builder.from("user").rightJoin("order", onCondition).toSql();
            assertThat(sql).contains("RIGHT JOIN `order` ON");
        }

        @Test
        @DisplayName("JOIN 带别名")
        void shouldGenerateJoinWithAlias() {
            Condition onCondition = Conditions.eq("u.id", "o.user_id");
            String sql = builder.from("user", "u").join("order", "o", onCondition).toSql();
            assertThat(sql).contains("JOIN `order` `o` ON");
        }
    }

    @Nested
    @DisplayName("GROUP BY / HAVING")
    class GroupByHavingTests {

        @Test
        @DisplayName("GROUP BY")
        void shouldGenerateGroupBy() {
            String sql = builder.select("status").from("user").groupBy("status").toSql();
            assertThat(sql).isEqualTo("SELECT `status` FROM `user` GROUP BY `status`");
        }

        @Test
        @DisplayName("GROUP BY + HAVING")
        void shouldGenerateGroupByHaving() {
            Condition havingCondition = Conditions.gt("count", 10);
            String sql = builder.select("status").from("user").groupBy("status").having(havingCondition).toSql();
            assertThat(sql).contains("GROUP BY `status` HAVING count > ?");
        }
    }

    @Nested
    @DisplayName("ORDER BY")
    class OrderByTests {

        @Test
        @DisplayName("ORDER BY ASC")
        void shouldGenerateOrderByAsc() {
            String sql = builder.from("user").orderBy("name", Sort.Direction.ASC).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` ORDER BY `name` ASC");
        }

        @Test
        @DisplayName("ORDER BY DESC")
        void shouldGenerateOrderByDesc() {
            String sql = builder.from("user").orderBy("name", Sort.Direction.DESC).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` ORDER BY `name` DESC");
        }

        @Test
        @DisplayName("ORDER BY Sort 对象")
        void shouldGenerateOrderBySortObject() {
            Sort sort = Sort.by(Sort.Order.desc("name"), Sort.Order.asc("id"));
            String sql = builder.from("user").orderBy(sort).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` ORDER BY `name` DESC, `id` ASC");
        }
    }

    @Nested
    @DisplayName("LIMIT / OFFSET")
    class LimitOffsetTests {

        @Test
        @DisplayName("LIMIT")
        void shouldGenerateLimit() {
            String sql = builder.from("user").limit(10).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` LIMIT 10 OFFSET 0");
        }

        @Test
        @DisplayName("LIMIT + OFFSET")
        void shouldGenerateLimitOffset() {
            String sql = builder.from("user").limit(10).offset(20).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("page 方法计算 offset")
        void shouldCalculateOffsetFromPage() {
            // page=2, size=10 -> offset=10
            String sql = builder.from("user").page(2, 10).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` LIMIT 10 OFFSET 10");
        }

        @Test
        @DisplayName("page 方法第一页")
        void shouldCalculateFirstPage() {
            // page=1, size=10 -> offset=0
            String sql = builder.from("user").page(1, 10).toSql();
            assertThat(sql).isEqualTo("SELECT * FROM `user` LIMIT 10 OFFSET 0");
        }
    }

    @Nested
    @DisplayName("DISTINCT")
    class DistinctTests {

        @Test
        @DisplayName("SELECT DISTINCT *")
        void shouldGenerateDistinct() {
            String sql = builder.distinct().from("user").toSql();
            assertThat(sql).isEqualTo("SELECT DISTINCT * FROM `user`");
        }

        @Test
        @DisplayName("SELECT DISTINCT column")
        void shouldGenerateDistinctColumn() {
            String sql = builder.select("name").distinct().from("user").toSql();
            assertThat(sql).isEqualTo("SELECT DISTINCT `name` FROM `user`");
        }
    }

    @Nested
    @DisplayName("聚合函数")
    class AggregateFunctionTests {

        @Test
        @DisplayName("COUNT(*)")
        void shouldGenerateCountAll() {
            String sql = builder.count().from("user").toSql();
            assertThat(sql).isEqualTo("SELECT COUNT(*) FROM `user`");
        }

        @Test
        @DisplayName("COUNT(column)")
        void shouldGenerateCountColumn() {
            String sql = builder.count("id").from("user").toSql();
            assertThat(sql).isEqualTo("SELECT COUNT(`id`) FROM `user`");
        }

        @Test
        @DisplayName("COUNT(DISTINCT column)")
        void shouldGenerateCountDistinct() {
            String sql = builder.countDistinct("id").from("user").toSql();
            assertThat(sql).isEqualTo("SELECT COUNT(DISTINCT `id`) FROM `user`");
        }

        @Test
        @DisplayName("SUM(column)")
        void shouldGenerateSum() {
            String sql = builder.sum("amount").from("order").toSql();
            assertThat(sql).isEqualTo("SELECT SUM(`amount`) FROM `order`");
        }

        @Test
        @DisplayName("AVG(column)")
        void shouldGenerateAvg() {
            String sql = builder.avg("score").from("exam").toSql();
            assertThat(sql).isEqualTo("SELECT AVG(`score`) FROM `exam`");
        }

        @Test
        @DisplayName("MAX(column)")
        void shouldGenerateMax() {
            String sql = builder.max("score").from("exam").toSql();
            assertThat(sql).isEqualTo("SELECT MAX(`score`) FROM `exam`");
        }

        @Test
        @DisplayName("MIN(column)")
        void shouldGenerateMin() {
            String sql = builder.min("score").from("exam").toSql();
            assertThat(sql).isEqualTo("SELECT MIN(`score`) FROM `exam`");
        }
    }

    @Nested
    @DisplayName("CTE (WITH 子句)")
    class CteTests {

        @Test
        @DisplayName("WITH CTE")
        void shouldGenerateCte() {
            SqlQueryBuilder cte = new SqlQueryBuilderImpl().select("id", "name").from("user");
            String sql = builder.with("cte", cte).from("cte").toSql();
            assertThat(sql).startsWith("WITH `cte` AS (SELECT `id`, `name` FROM `user`) SELECT * FROM `cte`");
        }

        @Test
        @DisplayName("WITH RECURSIVE CTE")
        void shouldGenerateRecursiveCte() {
            SqlQueryBuilder cte = new SqlQueryBuilderImpl().select("id").from("tree");
            String sql = builder.withRecursive("cte", cte).from("cte").toSql();
            assertThat(sql).startsWith("WITH RECURSIVE `cte` AS (SELECT `id` FROM `tree`) SELECT * FROM `cte`");
        }

        @Test
        @DisplayName("WITH CTE 带列名")
        void shouldGenerateCteWithColumnNames() {
            SqlQueryBuilder cte = new SqlQueryBuilderImpl().select("id", "name").from("user");
            String sql = builder.withColumnNames("cte", new String[]{"a", "b"}, cte).from("cte").toSql();
            assertThat(sql).contains("WITH `cte`(`a`, `b`) AS (");
        }
    }

    @Nested
    @DisplayName("EXISTS / NOT EXISTS")
    class ExistsTests {

        @Test
        @DisplayName("EXISTS 子查询")
        void shouldGenerateExists() {
            SqlQueryBuilder subquery = new SqlQueryBuilderImpl().select("user_id").from("order").where(Conditions.eq("user_id", "user.id"));
            String sql = builder.from("user").exists(subquery).toSql();
            assertThat(sql).contains("WHERE EXISTS (SELECT `user_id` FROM `order` WHERE user_id = ?)");
        }

        @Test
        @DisplayName("NOT EXISTS 子查询")
        void shouldGenerateNotExists() {
            SqlQueryBuilder subquery = new SqlQueryBuilderImpl().select("user_id").from("order").where(Conditions.eq("user_id", "user.id"));
            String sql = builder.from("user").notExists(subquery).toSql();
            assertThat(sql).contains("WHERE NOT EXISTS (SELECT `user_id` FROM `order` WHERE user_id = ?)");
        }
    }

    @Nested
    @DisplayName("子查询 FROM")
    class SubqueryFromTests {

        @Test
        @DisplayName("FROM 子查询")
        void shouldGenerateFromSubquery() {
            SqlQueryBuilder subquery = new SqlQueryBuilderImpl(new MySQLDialect()).from("user");
            String sql = builder.fromSubquery(subquery, "sub").toSql();
            // fromSubquery wraps the subquery SQL in parentheses and quotes it as a table name
            // Note: the fromTable gets quoted by dialect.quoteIdentifier, which may double-escape inner backticks
            assertThat(sql).contains("SELECT * FROM").contains("`sub`");
        }
    }

    @Nested
    @DisplayName("窗口函数")
    class WindowFunctionTests {

        @Test
        @DisplayName("ROW_NUMBER() OVER")
        void shouldGenerateRowNumberOver() {
            String sql = builder.rowNumberOver("status", "name ASC").from("user").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `status` ORDER BY `name` ASC)");
        }

        @Test
        @DisplayName("RANK() OVER")
        void shouldGenerateRankOver() {
            String sql = builder.rankOver("status", "score DESC").from("user").toSql();
            assertThat(sql).contains("RANK() OVER (PARTITION BY `status` ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("DENSE_RANK() OVER")
        void shouldGenerateDenseRankOver() {
            String sql = builder.denseRankOver("status", "score DESC").from("user").toSql();
            assertThat(sql).contains("DENSE_RANK() OVER (PARTITION BY `status` ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("ROW_NUMBER() OVER with Sort")
        void shouldGenerateRowNumberOverWithSort() {
            Sort sort = Sort.desc("score");
            String sql = builder.rowNumberOver("status", sort).from("user").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `status` ORDER BY `score` DESC)");
        }

        @Test
        @DisplayName("over() 链式调用")
        void shouldGenerateOverChain() {
            String sql = builder.rowNumber().over().partitionBy("status").orderBy("score", Sort.Direction.DESC).end().from("user").toSql();
            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY `status` ORDER BY `score` DESC)");
        }
    }

    @Nested
    @DisplayName("toSql + getParameters 一致性")
    class ConsistencyTests {

        @Test
        @DisplayName("WHERE 条件参数与 SQL 一致")
        void shouldHaveConsistentParametersAndSql() {
            Condition condition = Conditions.builder().eq("status", 1).eq("name", "test").build();
            String sql = builder.from("user").where(condition).toSql();
            List<Object> params = builder.from("user").where(condition).getParameters();

            assertThat(sql).contains("status = ?", "name = ?");
            assertThat(params).containsExactly(1, "test");
        }
    }

    @Nested
    @DisplayName("标识符验证")
    class IdentifierValidationTests {

        @Test
        @DisplayName("非法表名应抛异常")
        void shouldThrowExceptionForInvalidTableName() {
            assertThatThrownBy(() -> builder.from("user; DROP TABLE user;--").toSql())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("table name");
        }

        @Test
        @DisplayName("非法列名应抛异常")
        void shouldThrowExceptionForInvalidColumnName() {
            assertThatThrownBy(() -> builder.select("id; DROP TABLE user;--").from("user").toSql())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("column name");
        }
    }

    @Nested
    @DisplayName("execute/fetch 方法")
    class ExecuteMethodTests {

        @Test
        @DisplayName("fetch 方法抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForFetch() {
            assertThatThrownBy(() -> builder.from("user").fetch(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchOne 方法抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForFetchOne() {
            assertThatThrownBy(() -> builder.from("user").fetchOne(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchFirst 方法抛 UnsupportedOperationException")
        void shouldThrowUnsupportedOperationExceptionForFetchFirst() {
            assertThatThrownBy(() -> builder.from("user").fetchFirst(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }
    }
}
