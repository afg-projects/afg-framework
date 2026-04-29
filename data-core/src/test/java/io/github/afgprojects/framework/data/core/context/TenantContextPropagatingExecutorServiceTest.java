package io.github.afgprojects.framework.data.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * TenantContextPropagatingExecutorService comprehensive tests
 */
class TenantContextPropagatingExecutorServiceTest {

    private TenantContextHolder holder;
    private ExecutorService originalExecutor;
    private ExecutorService wrappedExecutor;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
        originalExecutor = Executors.newFixedThreadPool(2);
        wrappedExecutor = TenantContextPropagatingExecutorService.wrap(originalExecutor, holder);
    }

    @AfterEach
    void tearDown() {
        holder.clear();
        if (wrappedExecutor != null && !wrappedExecutor.isShutdown()) {
            wrappedExecutor.shutdown();
        }
    }

    // ==================== wrap() factory method tests ====================

    @Test
    void shouldWrapExecutorService() {
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(originalExecutor, holder);
        assertThat(wrapped).isNotNull();
        assertThat(wrapped).isInstanceOf(TenantContextPropagatingExecutorService.class);
    }

    @Test
    void shouldWrapScheduledExecutorService() {
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
        ScheduledExecutorService wrapped =
                TenantContextPropagatingExecutorService.wrap(scheduledExecutor, holder);

        assertThat(wrapped).isNotNull();
        assertThat(wrapped).isInstanceOf(ScheduledExecutorService.class);

        scheduledExecutor.shutdown();
    }

    // ==================== execute() tests ====================

    @Test
    void shouldExecutePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-execute");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        wrappedExecutor.execute(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isEqualTo("tenant-execute");
    }

    @Test
    void shouldExecuteNotPropagateWhenNoTenantSet() throws Exception {
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        wrappedExecutor.execute(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isNull();
    }

    @Test
    void shouldExecuteClearTenantContextAfterExecution() throws Exception {
        holder.setTenantId("tenant-execute");
        CountDownLatch latch = new CountDownLatch(1);

        wrappedExecutor.execute(() -> {
            // Task runs with tenant context
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);

        // Submit another task without tenant context
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch2 = new CountDownLatch(1);
        holder.clear();
        wrappedExecutor.execute(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch2.countDown();
        });

        latch2.await(1, TimeUnit.SECONDS);

        // Should not have leaked the previous tenant
        assertThat(capturedTenantId.get()).isNull();
    }

    // ==================== submit(Runnable) tests ====================

    @Test
    void shouldSubmitRunnablePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-submit");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        Future<?> future = wrappedExecutor.submit(() -> {
            capturedTenantId.set(holder.getTenantId());
        });

        future.get(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isEqualTo("tenant-submit");
    }

    // ==================== submit(Runnable, T result) tests ====================

    @Test
    void shouldSubmitRunnableWithResultPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-submit-result");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        String expectedResult = "result-value";

        Future<String> future = wrappedExecutor.submit(() -> {
            capturedTenantId.set(holder.getTenantId());
        }, expectedResult);

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo(expectedResult);
        assertThat(capturedTenantId.get()).isEqualTo("tenant-submit-result");
    }

    @Test
    void shouldSubmitRunnableWithNullResult() throws Exception {
        holder.setTenantId("tenant-submit-null");

        Future<String> future = wrappedExecutor.submit(() -> {}, (String) null);

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isNull();
    }

    // ==================== submit(Callable) tests ====================

    @Test
    void shouldSubmitCallablePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-callable");

        Future<String> future = wrappedExecutor.submit(() -> holder.getTenantId());

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("tenant-callable");
    }

    @Test
    void shouldSubmitCallableClearContextAfterExecution() throws Exception {
        holder.setTenantId("tenant-callable-clear");

        Future<String> future = wrappedExecutor.submit(() -> {
            return holder.getTenantId();
        });

        future.get(1, TimeUnit.SECONDS);

        // Submit another task to check thread context is clean
        holder.clear();
        Future<String> future2 = wrappedExecutor.submit(() -> holder.getTenantId());
        String result2 = future2.get(1, TimeUnit.SECONDS);

        assertThat(result2).isNull();
    }

    @Test
    void shouldSubmitCallablePropagateException() throws Exception {
        holder.setTenantId("tenant-exception");

        Future<String> future = wrappedExecutor.submit(() -> {
            throw new RuntimeException("Test exception");
        });

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Test exception");
    }

    // ==================== invokeAll tests ====================

    @Test
    void shouldInvokeAllPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-invokeAll");

        Collection<Callable<String>> tasks = Arrays.asList(
                () -> holder.getTenantId() + "-1",
                () -> holder.getTenantId() + "-2",
                () -> holder.getTenantId() + "-3"
        );

        List<Future<String>> futures = wrappedExecutor.invokeAll(tasks);

        assertThat(futures).hasSize(3);
        assertThat(futures.get(0).get()).isEqualTo("tenant-invokeAll-1");
        assertThat(futures.get(1).get()).isEqualTo("tenant-invokeAll-2");
        assertThat(futures.get(2).get()).isEqualTo("tenant-invokeAll-3");
    }

    @Test
    void shouldInvokeAllWithTimeoutPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-invokeAll-timeout");

        Collection<Callable<String>> tasks = Arrays.asList(
                () -> holder.getTenantId(),
                () -> holder.getTenantId()
        );

        List<Future<String>> futures = wrappedExecutor.invokeAll(tasks, 1, TimeUnit.SECONDS);

        assertThat(futures).hasSize(2);
        for (Future<String> future : futures) {
            assertThat(future.get()).isEqualTo("tenant-invokeAll-timeout");
        }
    }

    @Test
    void shouldInvokeAllWithEmptyCollection() throws Exception {
        holder.setTenantId("tenant-empty");

        List<Future<String>> futures = wrappedExecutor.invokeAll(new ArrayList<>());

        assertThat(futures).isEmpty();
    }

    // ==================== invokeAny tests ====================

    @Test
    void shouldInvokeAnyPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-invokeAny");

        Collection<Callable<String>> tasks = Arrays.asList(
                () -> holder.getTenantId() + "-1",
                () -> holder.getTenantId() + "-2"
        );

        String result = wrappedExecutor.invokeAny(tasks);

        assertThat(result).startsWith("tenant-invokeAny-");
    }

    @Test
    void shouldInvokeAnyWithTimeoutPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-invokeAny-timeout");

        Collection<Callable<String>> tasks = Arrays.asList(
                () -> {
                    Thread.sleep(10);
                    return holder.getTenantId();
                }
        );

        String result = wrappedExecutor.invokeAny(tasks, 1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("tenant-invokeAny-timeout");
    }

    // ==================== shutdown tests ====================

    @Test
    void shouldShutdownDelegateToOriginalExecutor() {
        assertThat(wrappedExecutor.isShutdown()).isFalse();

        wrappedExecutor.shutdown();

        assertThat(wrappedExecutor.isShutdown()).isTrue();
        assertThat(originalExecutor.isShutdown()).isTrue();
    }

    @Test
    void shouldShutdownNowReturnTasks() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(executor, holder);

        // Submit a task that will block
        CountDownLatch blockLatch = new CountDownLatch(1);
        wrapped.submit(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait a bit for task to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<Runnable> notExecuted = wrapped.shutdownNow();

        assertThat(wrapped.isShutdown()).isTrue();

        executor.shutdown();
    }

    @Test
    void shouldIsShutdownReflectState() {
        assertThat(wrappedExecutor.isShutdown()).isFalse();

        wrappedExecutor.shutdown();

        assertThat(wrappedExecutor.isShutdown()).isTrue();
    }

    @Test
    void shouldIsTerminatedReflectState() throws Exception {
        assertThat(wrappedExecutor.isTerminated()).isFalse();

        wrappedExecutor.shutdown();
        wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(wrappedExecutor.isTerminated()).isTrue();
    }

    @Test
    void shouldAwaitTerminationReturnTrue() throws Exception {
        wrappedExecutor.shutdown();

        boolean terminated = wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(terminated).isTrue();
    }

    @Test
    void shouldAwaitTerminationTimeout() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(executor, holder);

        // Don't shutdown, just wait with short timeout
        boolean terminated = wrapped.awaitTermination(100, TimeUnit.MILLISECONDS);

        assertThat(terminated).isFalse();

        executor.shutdown();
    }

    // ==================== Complex scenarios ====================

    @Test
    void shouldPropagateDifferentTenantsToDifferentTasks() throws Exception {
        AtomicReference<String> task1Tenant = new AtomicReference<>();
        AtomicReference<String> task2Tenant = new AtomicReference<>();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        holder.setTenantId("tenant-1");
        Future<?> f1 = wrappedExecutor.submit(() -> {
            task1Tenant.set(holder.getTenantId());
            latch1.countDown();
        });
        latch1.await(1, TimeUnit.SECONDS);

        holder.setTenantId("tenant-2");
        Future<?> f2 = wrappedExecutor.submit(() -> {
            task2Tenant.set(holder.getTenantId());
            latch2.countDown();
        });
        latch2.await(1, TimeUnit.SECONDS);

        assertThat(task1Tenant.get()).isEqualTo("tenant-1");
        assertThat(task2Tenant.get()).isEqualTo("tenant-2");
    }

    @Test
    void shouldHandleConcurrentSubmissions() throws Exception {
        int taskCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(taskCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            final String tenantId = "tenant-" + i;
            holder.setTenantId(tenantId);
            futures.add(wrappedExecutor.submit(() -> {
                // Each task should have its own tenant context
                String captured = holder.getTenantId();
                if (captured != null && captured.startsWith("tenant-")) {
                    successCount.incrementAndGet();
                }
                endLatch.countDown();
            }));
        }

        endLatch.await(5, TimeUnit.SECONDS);

        // All tasks should have some tenant context propagated
        assertThat(successCount.get()).isEqualTo(taskCount);
    }

    @Test
    void shouldWrappedCallableHandleExceptionGracefully() throws Exception {
        holder.setTenantId("tenant-exception");

        Future<String> future = wrappedExecutor.submit(() -> {
            throw new IllegalArgumentException("Intentional test exception");
        });

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotModifyCallerThreadContext() throws Exception {
        holder.setTenantId("tenant-caller");
        String originalTenantId = holder.getTenantId();

        wrappedExecutor.submit(() -> holder.getTenantId()).get(1, TimeUnit.SECONDS);

        assertThat(holder.getTenantId()).isEqualTo(originalTenantId);
    }

    @Test
    void shouldPropagateTenantContextWithCompletableFutureAsync() throws Exception {
        holder.setTenantId("tenant-completable");

        java.util.concurrent.CompletableFuture<String> future =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> holder.getTenantId(),
                        wrappedExecutor
                );

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("tenant-completable");
    }
}
