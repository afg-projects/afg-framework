package io.github.afgprojects.framework.data.sql.benchmark;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
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
 * 条件转换器性能基准测试
 * <p>
 * 测试 Condition 到 SQL WHERE 子句的转换性能
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ConditionToSqlConverterBenchmark {

    private ConditionToSqlConverter converter;

    /**
     * 单条件状态
     */
    @State(Scope.Thread)
    public static class SingleConditionState {
        Condition condition;

        @Setup(Level.Invocation)
        public void setup() {
            condition = Conditions.eq("name", "test");
        }
    }

    /**
     * 多条件 AND 状态
     */
    @State(Scope.Thread)
    public static class MultipleAndConditionState {
        Condition condition;

        @Setup(Level.Invocation)
        public void setup() {
            condition = Conditions.builder()
                    .eq("status", 1)
                    .gt("age", 18)
                    .like("name", "test")
                    .ne("deleted", true)
                    .build();
        }
    }

    /**
     * 复杂嵌套条件状态
     */
    @State(Scope.Thread)
    public static class NestedConditionState {
        Condition condition;

        @Setup(Level.Invocation)
        public void setup() {
            Condition inner1 = Conditions.builder()
                    .eq("a", 1)
                    .gt("b", 10)
                    .build();

            Condition inner2 = Conditions.builder()
                    .like("c", "test")
                    .in("d", List.of(1, 2, 3, 4, 5))
                    .build();

            condition = Conditions.builder()
                    .and(inner1)
                    .or(inner2)
                    .build();
        }
    }

    /**
     * IN 条件状态
     */
    @State(Scope.Thread)
    public static class InConditionState {
        Condition condition;

        @Setup(Level.Invocation)
        public void setup() {
            condition = Conditions.in("id", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }
    }

    /**
     * BETWEEN 条件状态
     */
    @State(Scope.Thread)
    public static class BetweenConditionState {
        Condition condition;

        @Setup(Level.Invocation)
        public void setup() {
            condition = Conditions.builder()
                    .between("age", 18, 65)
                    .between("salary", 5000, 50000)
                    .build();
        }
    }

    @Setup(Level.Trial)
    public void setup() {
        converter = new ConditionToSqlConverter();
    }

    // ==================== 单条件转换测试 ====================

    /**
     * 测试单条件转换 (EQ)
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertSingleEqCondition(SingleConditionState state) {
        return converter.convert(state.condition);
    }

    // ==================== 多条件转换测试 ====================

    /**
     * 测试多条件 AND 转换
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertMultipleAndConditions(MultipleAndConditionState state) {
        return converter.convert(state.condition);
    }

    // ==================== 嵌套条件转换测试 ====================

    /**
     * 测试嵌套条件转换
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertNestedCondition(NestedConditionState state) {
        return converter.convert(state.condition);
    }

    // ==================== IN 条件转换测试 ====================

    /**
     * 测试 IN 条件转换
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertInCondition(InConditionState state) {
        return converter.convert(state.condition);
    }

    // ==================== BETWEEN 条件转换测试 ====================

    /**
     * 测试 BETWEEN 条件转换
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertBetweenCondition(BetweenConditionState state) {
        return converter.convert(state.condition);
    }

    // ==================== 空条件转换测试 ====================

    /**
     * 测试空条件转换
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertEmptyCondition() {
        return converter.convert(Conditions.empty());
    }

    // ==================== 各种操作符转换测试 ====================

    /**
     * 测试各种比较操作符
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertComparisonOperators() {
        Condition condition = Conditions.builder()
                .eq("a", 1)
                .ne("b", 2)
                .gt("c", 3)
                .ge("d", 4)
                .lt("e", 5)
                .le("f", 6)
                .build();
        return converter.convert(condition);
    }

    /**
     * 测试各种 LIKE 操作符
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertLikeOperators() {
        Condition condition = Conditions.builder()
                .like("name", "test")
                .likeLeft("email", "test")
                .likeRight("phone", "123")
                .notLike("address", "spam")
                .build();
        return converter.convert(condition);
    }

    /**
     * 测试 NULL 操作符
     */
    @Benchmark
    public ConditionToSqlConverter.SqlResult convertNullOperators() {
        Condition condition = Conditions.builder()
                .isNull("deleted_at")
                .isNotNull("email")
                .build();
        return converter.convert(condition);
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConditionToSqlConverterBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
