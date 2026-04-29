package io.github.afgprojects.framework.data.sql.benchmark;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SQL 查询构建器性能基准测试
 * <p>
 * 测试场景：
 * <ul>
 *   <li>简单查询构建</li>
 *   <li>复杂条件查询构建</li>
 *   <li>多表 JOIN 查询构建</li>
 *   <li>分页查询构建</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class SqlQueryBuilderBenchmark {

    /**
     * 简单查询状态
     */
    @State(Scope.Thread)
    public static class SimpleQueryState {
        SqlQueryBuilderImpl builder;

        @Setup(Level.Invocation)
        public void setup() {
            builder = new SqlQueryBuilderImpl();
        }
    }

    /**
     * 复杂条件状态
     */
    @State(Scope.Thread)
    public static class ComplexConditionState {
        Condition complexCondition;
        Sort sort;

        @Setup(Level.Trial)
        public void setup() {
            // 构建复杂条件：(a = 1 AND b > 10) OR (c LIKE '%test%' AND d IN (1, 2, 3))
            Condition condition1 = Conditions.builder()
                    .eq("a", 1)
                    .gt("b", 10)
                    .build();

            Condition condition2 = Conditions.builder()
                    .like("c", "test")
                    .in("d", List.of(1, 2, 3))
                    .build();

            complexCondition = Conditions.builder()
                    .and(condition1)
                    .or(condition2)
                    .build();

            sort = Sort.by(Sort.Direction.DESC, "created_at")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
        }
    }

    /**
     * JOIN 查询状态
     */
    @State(Scope.Thread)
    public static class JoinQueryState {
        Condition joinCondition1;
        Condition joinCondition2;
        Condition whereCondition;

        @Setup(Level.Trial)
        public void setup() {
            joinCondition1 = Conditions.eq("u.department_id", "d.id");
            joinCondition2 = Conditions.eq("u.role_id", "r.id");
            whereCondition = Conditions.builder()
                    .eq("u.status", 1)
                    .gt("u.age", 18)
                    .build();
        }
    }

    // ==================== 简单查询测试 ====================

    /**
     * 测试简单 SELECT * FROM table
     */
    @Benchmark
    public String simpleSelectAll(SimpleQueryState state) {
        return state.builder
                .selectAll()
                .from("users")
                .toSql();
    }

    /**
     * 测试指定列的简单查询
     */
    @Benchmark
    public String simpleSelectColumns(SimpleQueryState state) {
        return state.builder
                .select("id", "name", "email", "created_at")
                .from("users")
                .toSql();
    }

    /**
     * 测试带 WHERE 条件的查询
     */
    @Benchmark
    public String simpleWhereQuery(SimpleQueryState state) {
        return state.builder
                .select("id", "name")
                .from("users")
                .where(Conditions.eq("status", 1))
                .toSql();
    }

    // ==================== 条件构建测试 ====================

    /**
     * 测试单个条件构建
     */
    @Benchmark
    public Condition singleConditionBuild() {
        return Conditions.eq("name", "test");
    }

    /**
     * 测试多条件 AND 构建
     */
    @Benchmark
    public Condition multipleAndConditionsBuild() {
        return Conditions.builder()
                .eq("status", 1)
                .gt("age", 18)
                .like("name", "test")
                .build();
    }

    /**
     * 测试复杂嵌套条件构建
     */
    @Benchmark
    public Condition nestedConditionBuild() {
        Condition inner1 = Conditions.builder()
                .eq("a", 1)
                .gt("b", 10)
                .build();

        Condition inner2 = Conditions.builder()
                .like("c", "test")
                .in("d", List.of(1, 2, 3))
                .build();

        return Conditions.builder()
                .and(inner1)
                .or(inner2)
                .build();
    }

    // ==================== SQL 生成测试 ====================

    /**
     * 测试复杂查询 SQL 生成
     */
    @Benchmark
    public String complexQueryGeneration(SimpleQueryState state, ComplexConditionState condState) {
        return state.builder
                .select("id", "name", "email", "status", "created_at")
                .from("users")
                .where(condState.complexCondition)
                .orderBy(condState.sort)
                .limit(10)
                .offset(0)
                .toSql();
    }

    /**
     * 测试聚合函数查询
     */
    @Benchmark
    public String aggregateQueryGeneration(SimpleQueryState state) {
        return state.builder
                .count()
                .sum("amount")
                .avg("score")
                .max("created_at")
                .min("updated_at")
                .from("orders")
                .groupBy("status", "category")
                .toSql();
    }

    /**
     * 测试 DISTINCT 查询
     */
    @Benchmark
    public String distinctQueryGeneration(SimpleQueryState state) {
        return state.builder
                .distinct()
                .select("category", "status")
                .from("products")
                .toSql();
    }

    // ==================== JOIN 查询测试 ====================

    /**
     * 测试单表 JOIN 查询
     */
    @Benchmark
    public String singleJoinQuery(SimpleQueryState state, JoinQueryState joinState) {
        return state.builder
                .select("u.id", "u.name", "d.name")
                .from("users", "u")
                .leftJoin("departments", "d", joinState.joinCondition1)
                .where(joinState.whereCondition)
                .toSql();
    }

    /**
     * 测试多表 JOIN 查询
     */
    @Benchmark
    public String multipleJoinQuery(SimpleQueryState state, JoinQueryState joinState) {
        return state.builder
                .select("u.id", "u.name", "d.name", "r.name")
                .from("users", "u")
                .leftJoin("departments", "d", joinState.joinCondition1)
                .leftJoin("roles", "r", joinState.joinCondition2)
                .where(joinState.whereCondition)
                .toSql();
    }

    // ==================== 分页查询测试 ====================

    /**
     * 测试分页查询构建
     */
    @Benchmark
    public String paginationQuery(SimpleQueryState state) {
        return state.builder
                .select("id", "name", "email")
                .from("users")
                .where(Conditions.eq("status", 1))
                .orderBy(Sort.by(Sort.Direction.DESC, "created_at"))
                .page(2, 20)
                .toSql();
    }

    /**
     * 测试 LIMIT OFFSET 查询
     */
    @Benchmark
    public String limitOffsetQuery(SimpleQueryState state) {
        return state.builder
                .selectAll()
                .from("users")
                .limit(100)
                .offset(200)
                .toSql();
    }

    // ==================== 参数获取测试 ====================

    /**
     * 测试获取查询参数
     */
    @Benchmark
    public List<Object> getQueryParameters(SimpleQueryState state) {
        Condition condition = Conditions.builder()
                .eq("status", 1)
                .gt("age", 18)
                .like("name", "test")
                .build();

        state.builder
                .select("id", "name")
                .from("users")
                .where(condition);

        return state.builder.getParameters();
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SqlQueryBuilderBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
