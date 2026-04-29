package io.github.afgprojects.framework.data.jdbc.metadata;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * 字段访问性能基准测试
 * <p>
 * 对比传统反射访问与缓存字段访问器的性能差异
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FieldAccessorBenchmark {

    // 测试实体
    static class TestEntity {
        private Long id;
        private String name;
        private Integer age;
        private Double score;

        public TestEntity() {
        }

        public TestEntity(Long id, String name, Integer age, Double score) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.score = score;
        }
    }

    // 传统反射方式
    private Field idFieldReflect;
    private Field nameFieldReflect;
    private Field ageFieldReflect;
    private Field scoreFieldReflect;

    // 缓存访问器方式
    private CachedFieldAccessor idFieldAccessor;
    private CachedFieldAccessor nameFieldAccessor;
    private CachedFieldAccessor ageFieldAccessor;
    private CachedFieldAccessor scoreFieldAccessor;

    // 测试数据
    private TestEntity entity;

    @Setup
    public void setup() throws Exception {
        // 初始化传统反射方式
        idFieldReflect = TestEntity.class.getDeclaredField("id");
        idFieldReflect.setAccessible(true);
        nameFieldReflect = TestEntity.class.getDeclaredField("name");
        nameFieldReflect.setAccessible(true);
        ageFieldReflect = TestEntity.class.getDeclaredField("age");
        ageFieldReflect.setAccessible(true);
        scoreFieldReflect = TestEntity.class.getDeclaredField("score");
        scoreFieldReflect.setAccessible(true);

        // 初始化缓存访问器方式
        idFieldAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("id"));
        nameFieldAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("name"));
        ageFieldAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("age"));
        scoreFieldAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("score"));

        // 创建测试实体
        entity = new TestEntity(1L, "test", 25, 98.5);
    }

    /**
     * 传统反射方式 - 每次获取字段值（已缓存 Field 和 setAccessible）
     */
    @Benchmark
    public Object[] traditionalReflectionGet() throws Exception {
        Object[] values = new Object[4];
        values[0] = idFieldReflect.get(entity);
        values[1] = nameFieldReflect.get(entity);
        values[2] = ageFieldReflect.get(entity);
        values[3] = scoreFieldReflect.get(entity);
        return values;
    }

    /**
     * 缓存访问器方式 - 每次获取字段值
     */
    @Benchmark
    public Object[] cachedAccessorGet() {
        Object[] values = new Object[4];
        values[0] = idFieldAccessor.getValue(entity);
        values[1] = nameFieldAccessor.getValue(entity);
        values[2] = ageFieldAccessor.getValue(entity);
        values[3] = scoreFieldAccessor.getValue(entity);
        return values;
    }

    /**
     * 传统反射方式 - 每次设置字段值（已缓存 Field 和 setAccessible）
     */
    @Benchmark
    public void traditionalReflectionSet() throws Exception {
        TestEntity newEntity = new TestEntity();
        idFieldReflect.set(newEntity, 1L);
        nameFieldReflect.set(newEntity, "test");
        ageFieldReflect.set(newEntity, 25);
        scoreFieldReflect.set(newEntity, 98.5);
    }

    /**
     * 缓存访问器方式 - 每次设置字段值
     */
    @Benchmark
    public void cachedAccessorSet() {
        TestEntity newEntity = new TestEntity();
        idFieldAccessor.setValue(newEntity, 1L);
        nameFieldAccessor.setValue(newEntity, "test");
        ageFieldAccessor.setValue(newEntity, 25);
        scoreFieldAccessor.setValue(newEntity, 98.5);
    }

    /**
     * 模拟传统反射方式 - 每次都调用 getDeclaredField 和 setAccessible
     * <p>
     * 这是最慢的方式，用于展示未优化时的性能（原始 JdbcEntityProxy 实现）
     */
    @Benchmark
    public Object[] worstCaseReflectionGet() throws Exception {
        Object[] values = new Object[4];

        Field f1 = TestEntity.class.getDeclaredField("id");
        f1.setAccessible(true);
        values[0] = f1.get(entity);

        Field f2 = TestEntity.class.getDeclaredField("name");
        f2.setAccessible(true);
        values[1] = f2.get(entity);

        Field f3 = TestEntity.class.getDeclaredField("age");
        f3.setAccessible(true);
        values[2] = f3.get(entity);

        Field f4 = TestEntity.class.getDeclaredField("score");
        f4.setAccessible(true);
        values[3] = f4.get(entity);

        return values;
    }

    /**
     * 模拟传统反射方式 - 每次都调用 getDeclaredField 和 setAccessible（设置值）
     * <p>
     * 这是最慢的方式，用于展示未优化时的性能（原始 JdbcEntityProxy 实现）
     */
    @Benchmark
    public void worstCaseReflectionSet() throws Exception {
        TestEntity newEntity = new TestEntity();

        Field f1 = TestEntity.class.getDeclaredField("id");
        f1.setAccessible(true);
        f1.set(newEntity, 1L);

        Field f2 = TestEntity.class.getDeclaredField("name");
        f2.setAccessible(true);
        f2.set(newEntity, "test");

        Field f3 = TestEntity.class.getDeclaredField("age");
        f3.setAccessible(true);
        f3.set(newEntity, 25);

        Field f4 = TestEntity.class.getDeclaredField("score");
        f4.setAccessible(true);
        f4.set(newEntity, 98.5);
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FieldAccessorBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
