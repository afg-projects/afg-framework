package io.github.afgprojects.framework.core.web.shutdown;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ShutdownHook.
 */
class ShutdownHookTest {

    private ShutdownProperties properties;
    private ShutdownHook shutdownHook;

    @BeforeEach
    void setUp() {
        properties = new ShutdownProperties();
        shutdownHook = new ShutdownHook(properties);
    }

    @Test
    void shouldExecuteShutdownCallbacksInOrder() {
        List<String> executionOrder = new ArrayList<>();

        shutdownHook.addCallback("cleanup", 3, () -> executionOrder.add("third"));
        shutdownHook.addCallback("cleanup", 1, () -> executionOrder.add("first"));
        shutdownHook.addCallback("cleanup", 2, () -> executionOrder.add("second"));

        shutdownHook.executePhase("cleanup");

        assertEquals(List.of("first", "second", "third"), executionOrder);
    }

    @Test
    void shouldExecutePhasesInSequence() {
        List<String> executedPhases = new ArrayList<>();

        shutdownHook.addCallback("drain", 0, () -> executedPhases.add("drain"));
        shutdownHook.addCallback("cleanup", 0, () -> executedPhases.add("cleanup"));
        shutdownHook.addCallback("force", 0, () -> executedPhases.add("force"));

        // 按顺序执行阶段
        for (ShutdownProperties.Phase phase : properties.getPhases()) {
            shutdownHook.executePhaseWithTimeout(phase.getName(), phase.getTimeout());
        }

        assertEquals(List.of("drain", "cleanup", "force"), executedPhases);
    }

    @Test
    void shouldSkipPhaseWhenNoCallbacks() {
        // 没有为任何阶段注册回调
        assertTrue(shutdownHook.getCallbacks().isEmpty());

        // 不应抛出异常
        shutdownHook.executePhaseWithTimeout("drain", Duration.ofSeconds(1));
        shutdownHook.executePhaseWithTimeout("cleanup", Duration.ofSeconds(1));
        shutdownHook.executePhaseWithTimeout("force", Duration.ofSeconds(1));
    }

    @Test
    void shouldRegisterCallback() {
        shutdownHook.addCallback("drain", 10, () -> {});

        var callbacks = shutdownHook.getCallbacks();
        assertTrue(callbacks.containsKey("drain"));
        assertEquals(1, callbacks.get("drain").size());
    }

    @Test
    void shouldRegisterMultipleCallbacksForSamePhase() {
        shutdownHook.addCallback("cleanup", 1, () -> {});
        shutdownHook.addCallback("cleanup", 2, () -> {});
        shutdownHook.addCallback("cleanup", 3, () -> {});

        var callbacks = shutdownHook.getCallbacks();
        assertEquals(3, callbacks.get("cleanup").size());
    }

    @Test
    void shouldHandleNegativeOrder() {
        List<String> executionOrder = new ArrayList<>();

        shutdownHook.addCallback("cleanup", -1, () -> executionOrder.add("negative"));
        shutdownHook.addCallback("cleanup", 0, () -> executionOrder.add("zero"));
        shutdownHook.addCallback("cleanup", 1, () -> executionOrder.add("positive"));

        shutdownHook.executePhase("cleanup");

        assertEquals(List.of("negative", "zero", "positive"), executionOrder);
    }

    @Test
    void shouldHandleSameOrder() {
        AtomicInteger counter = new AtomicInteger(0);

        shutdownHook.addCallback("cleanup", 0, counter::incrementAndGet);
        shutdownHook.addCallback("cleanup", 0, counter::incrementAndGet);
        shutdownHook.addCallback("cleanup", 0, counter::incrementAndGet);

        shutdownHook.executePhase("cleanup");

        assertEquals(3, counter.get());
    }

    @Test
    void shouldExecuteWithinTimeout() {
        List<String> executed = new ArrayList<>();

        shutdownHook.addCallback("drain", 0, () -> {
            executed.add("done");
        });

        shutdownHook.executePhaseWithTimeout("drain", Duration.ofSeconds(1));

        assertEquals(List.of("done"), executed);
    }

    @Test
    void shouldExecuteSameOrderCallbacksInParallelWhenEnabled() throws InterruptedException {
        // 开启并行执行
        properties.setParallelExecutionEnabled(true);

        List<String> executionOrder = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);
        CountDownLatch startLatch = new CountDownLatch(1);

        // 添加 3 个相同 order 的回调，它们应该并行执行
        shutdownHook.addCallback("cleanup", 0, () -> {
            try {
                startLatch.await();
                synchronized (executionOrder) {
                    executionOrder.add("callback1");
                }
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        shutdownHook.addCallback("cleanup", 0, () -> {
            try {
                startLatch.await();
                synchronized (executionOrder) {
                    executionOrder.add("callback2");
                }
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        shutdownHook.addCallback("cleanup", 0, () -> {
            try {
                startLatch.await();
                synchronized (executionOrder) {
                    executionOrder.add("callback3");
                }
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 在另一个线程中执行，以便可以释放 startLatch
        Thread execThread = new Thread(() -> shutdownHook.executePhase("cleanup"));
        execThread.start();

        // 稍等一下让线程启动
        Thread.sleep(50);
        startLatch.countDown(); // 释放所有回调同时开始

        execThread.join(1000);
        assertFalse(execThread.isAlive(), "Execution should complete within timeout");

        // 所有 3 个回调都应该执行
        assertEquals(3, executionOrder.size());
        assertTrue(executionOrder.contains("callback1"));
        assertTrue(executionOrder.contains("callback2"));
        assertTrue(executionOrder.contains("callback3"));
    }

    @Test
    void shouldExecuteDifferentOrderCallbacksSequentiallyWhenParallelEnabled() {
        // 开启并行执行
        properties.setParallelExecutionEnabled(true);

        List<String> executionOrder = new ArrayList<>();

        // 添加不同 order 的回调，它们应该按顺序执行
        shutdownHook.addCallback("cleanup", 1, () -> executionOrder.add("order1"));
        shutdownHook.addCallback("cleanup", 2, () -> executionOrder.add("order2"));
        shutdownHook.addCallback("cleanup", 3, () -> executionOrder.add("order3"));

        shutdownHook.executePhase("cleanup");

        assertEquals(List.of("order1", "order2", "order3"), executionOrder);
    }

    @Test
    void shouldMaintainOrderGroupsExecutionSequence() {
        // 开启并行执行
        properties.setParallelExecutionEnabled(true);

        List<Long> timestamps = new ArrayList<>();

        // order 0 的 2 个回调
        shutdownHook.addCallback("cleanup", 0, () -> {
            synchronized (timestamps) {
                timestamps.add(System.nanoTime());
            }
        });
        shutdownHook.addCallback("cleanup", 0, () -> {
            synchronized (timestamps) {
                timestamps.add(System.nanoTime());
            }
        });

        // order 1 的 1 个回调
        shutdownHook.addCallback("cleanup", 1, () -> {
            synchronized (timestamps) {
                timestamps.add(System.nanoTime());
            }
        });

        shutdownHook.executePhase("cleanup");

        // 应该有 3 个时间戳
        assertEquals(3, timestamps.size());
        // order 1 的回调应该在 order 0 的回调之后执行
        // 由于并行执行，order 0 的两个回调时间戳可能交错，但 order 1 的时间戳应该最大
        assertTrue(timestamps.get(2) >= timestamps.get(0), "Order 1 callback should execute after order 0 callbacks");
        assertTrue(timestamps.get(2) >= timestamps.get(1), "Order 1 callback should execute after order 0 callbacks");
    }

    @Test
    void shouldExecuteSequentiallyWhenParallelDisabled() {
        // 确保并行执行是关闭的（默认）
        assertFalse(properties.isParallelExecutionEnabled());

        List<String> executionOrder = new ArrayList<>();

        shutdownHook.addCallback("cleanup", 0, () -> executionOrder.add("first"));
        shutdownHook.addCallback("cleanup", 0, () -> executionOrder.add("second"));
        shutdownHook.addCallback("cleanup", 0, () -> executionOrder.add("third"));

        shutdownHook.executePhase("cleanup");

        // 即使 order 相同，也应该按添加顺序执行
        assertEquals(List.of("first", "second", "third"), executionOrder);
    }

    @Test
    void shouldHandleExceptionInParallelExecution() {
        // 开启并行执行
        properties.setParallelExecutionEnabled(true);

        AtomicInteger counter = new AtomicInteger(0);

        // 添加一个会抛异常的回调和正常回调
        shutdownHook.addCallback("cleanup", 0, () -> {
            counter.incrementAndGet();
            throw new RuntimeException("Test exception");
        });
        shutdownHook.addCallback("cleanup", 0, () -> counter.incrementAndGet());

        // 不应该抛出异常
        assertDoesNotThrow(() -> shutdownHook.executePhase("cleanup"));

        // 两个回调都应该执行
        assertEquals(2, counter.get());
    }
}
