package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.trace.TraceContextPropagator.Restore;
import io.github.afgprojects.framework.core.trace.TraceContextPropagator.TraceContextSnapshot;

/**
 * TraceContextPropagator 测试类
 */
@DisplayName("TraceContextPropagator 测试")
class TraceContextPropagatorTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        BaggageContext.clear();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        BaggageContext.clear();
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("捕获当前追踪上下文")
    void testCapture() {
        BaggageContext.set("field1", "value1");
        BaggageContext.set("field2", "value2");

        TraceContextSnapshot snapshot = TraceContextPropagator.capture();

        assertThat(snapshot.baggage()).containsEntry("field1", "value1");
        assertThat(snapshot.baggage()).containsEntry("field2", "value2");
    }

    @Test
    @DisplayName("恢复追踪上下文")
    void testRestore() {
        // 创建快照
        BaggageContext.set("field1", "value1");
        TraceContextSnapshot snapshot = TraceContextPropagator.capture();

        // 清除当前上下文
        BaggageContext.clear();
        assertThat(BaggageContext.get("field1")).isNull();

        // 恢复上下文
        try (Restore restore = TraceContextPropagator.restore(snapshot)) {
            assertThat(BaggageContext.get("field1")).isEqualTo("value1");
        }

        // 关闭后恢复为空
        assertThat(BaggageContext.get("field1")).isNull();
    }

    @Test
    @DisplayName("包装 Runnable 任务")
    void testWrapRunnable() throws Exception {
        BaggageContext.set("testField", "testValue");

        Runnable wrappedTask = TraceContextPropagator.wrap(() -> {
            assertThat(BaggageContext.get("testField")).isEqualTo("testValue");
        });

        // 清除当前上下文后执行
        BaggageContext.clear();

        Future<?> future = executor.submit(wrappedTask);
        future.get(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("包装 Callable 任务")
    void testWrapCallable() throws Exception {
        BaggageContext.set("testField", "testValue");

        java.util.concurrent.Callable<String> wrappedTask = TraceContextPropagator.wrap(() -> {
            assertThat(BaggageContext.get("testField")).isEqualTo("testValue");
            return "result";
        });

        // 清除当前上下文后执行
        BaggageContext.clear();

        Future<String> future = executor.submit(wrappedTask);
        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("包装 ExecutorService")
    void testWrapExecutorService() throws Exception {
        ExecutorService wrappedExecutor = TraceContextPropagator.wrapExecutor(executor);

        // 设置上下文后提交任务
        BaggageContext.set("testField", "testValue");

        Future<String> future = wrappedExecutor.submit(() -> {
            assertThat(BaggageContext.get("testField")).isEqualTo("testValue");
            return "result";
        });

        String result = future.get(1, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("result");

        BaggageContext.clear();
    }

    @Test
    @DisplayName("快照有效性判断")
    void testSnapshotValidity() {
        // 空快照无效
        TraceContextSnapshot emptySnapshot = TraceContextPropagator.capture();
        assertThat(emptySnapshot.isValid()).isFalse();

        // 有 baggage 的快照有效
        BaggageContext.set("field", "value");
        TraceContextSnapshot validSnapshot = TraceContextPropagator.capture();
        assertThat(validSnapshot.isValid()).isTrue();
    }

    @Test
    @DisplayName("使用指定快照包装任务")
    void testWrapWithSpecificSnapshot() throws Exception {
        BaggageContext.set("field1", "value1");
        TraceContextSnapshot snapshot = TraceContextPropagator.capture();

        // 清除当前上下文
        BaggageContext.clear();

        Runnable wrappedTask = TraceContextPropagator.wrap(() -> {
            assertThat(BaggageContext.get("field1")).isEqualTo("value1");
        }, snapshot);

        wrappedTask.run();

        BaggageContext.clear();
    }

    @Test
    @DisplayName("多线程并发执行保持各自上下文")
    void testConcurrentExecution() throws Exception {
        BaggageContext.set("sharedField", "sharedValue");

        Runnable task1 = TraceContextPropagator.wrap(() -> {
            BaggageContext.set("threadField", "thread1");
            assertThat(BaggageContext.get("sharedField")).isEqualTo("sharedValue");
        });

        Runnable task2 = TraceContextPropagator.wrap(() -> {
            BaggageContext.set("threadField", "thread2");
            assertThat(BaggageContext.get("sharedField")).isEqualTo("sharedValue");
        });

        Future<?> f1 = executor.submit(task1);
        Future<?> f2 = executor.submit(task2);

        f1.get(1, TimeUnit.SECONDS);
        f2.get(1, TimeUnit.SECONDS);
    }
}