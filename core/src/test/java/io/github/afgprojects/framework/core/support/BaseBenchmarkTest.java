package io.github.afgprojects.framework.core.support;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH 基准测试基类
 * 提供性能基准测试的基础配置
 *
 * <p>使用示例:
 * <pre>{@code
 * @State(Scope.Benchmark)
 * public class MyBenchmark extends BaseBenchmarkTest {
 *
 *     @Benchmark
 *     public void testMethod() {
 *         // 被测代码
 *     }
 *
 *     public static void main(String[] args) throws RunnerException {
 *         new MyBenchmark().runBenchmark();
 *     }
 * }
 * }</pre>
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public abstract class BaseBenchmarkTest {

    /**
     * 运行基准测试
     * 子类可以在 main 方法中调用此方法
     */
    public void runBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder().include(getClass().getSimpleName()).build();
        new Runner(opt).run();
    }

    /**
     * 运行指定方法的基准测试
     */
    public void runBenchmark(@NonNull String methodName) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(getClass().getSimpleName() + "." + methodName)
                .build();
        new Runner(opt).run();
    }

    /**
     * 使用自定义配置运行基准测试
     */
    public void runBenchmark(@NonNull Options options) throws RunnerException {
        new Runner(options).run();
    }
}
