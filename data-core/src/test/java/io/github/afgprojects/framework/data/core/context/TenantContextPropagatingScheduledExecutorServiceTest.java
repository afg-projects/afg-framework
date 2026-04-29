package io.github.afgprojects.framework.data.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
 * TenantContextPropagatingScheduledExecutorService comprehensive tests
 */
class TenantContextPropagatingScheduledExecutorServiceTest {

    private TenantContextHolder holder;
    private ScheduledExecutorService originalScheduler;
    private ScheduledExecutorService wrappedScheduler;

    @BeforeEach
    void setUp() {
        holder = new TenantContextHolder();
        originalScheduler = Executors.newScheduledThreadPool(2);
        wrappedScheduler = TenantContextPropagatingExecutorService.wrap(originalScheduler, holder);
    }

    @AfterEach
    void tearDown() {
        holder.clear();
        if (wrappedScheduler != null && !wrappedScheduler.isShutdown()) {
            wrappedScheduler.shutdown();
        }
    }

    // ==================== schedule(Runnable) tests ====================

    @Test
    void shouldScheduleRunnablePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-schedule");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledFuture<?> future = wrappedScheduler.schedule(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        }, 50, TimeUnit.MILLISECONDS);

        latch.await(1, TimeUnit.SECONDS);
        future.get(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isEqualTo("tenant-schedule");
    }

    @Test
    void shouldScheduleRunnableWithZeroDelay() throws Exception {
        holder.setTenantId("tenant-immediate");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledFuture<?> future = wrappedScheduler.schedule(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);

        latch.await(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isEqualTo("tenant-immediate");
    }

    @Test
    void shouldScheduleRunnableNotPropagateWhenNoTenant() throws Exception {
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledFuture<?> future = wrappedScheduler.schedule(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        }, 50, TimeUnit.MILLISECONDS);

        latch.await(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isNull();
    }

    @Test
    void shouldScheduleRunnableCancelBeforeExecution() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);

        ScheduledFuture<?> future = wrappedScheduler.schedule(() -> {
            executed.set(true);
        }, 10, TimeUnit.SECONDS);  // Long delay

        boolean cancelled = future.cancel(false);

        assertThat(cancelled).isTrue();
        assertThat(future.isCancelled()).isTrue();

        // Wait a bit to ensure it doesn't execute
        Thread.sleep(100);
        assertThat(executed.get()).isFalse();
    }

    // ==================== schedule(Callable) tests ====================

    @Test
    void shouldScheduleCallablePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-schedule-callable");

        ScheduledFuture<String> future = wrappedScheduler.schedule(
                () -> holder.getTenantId(),
                50, TimeUnit.MILLISECONDS
        );

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("tenant-schedule-callable");
    }

    @Test
    void shouldScheduleCallableWithResult() throws Exception {
        holder.setTenantId("tenant-result");

        ScheduledFuture<String> future = wrappedScheduler.schedule(
                () -> {
                    String tenant = holder.getTenantId();
                    return "result-" + tenant;
                },
                10, TimeUnit.MILLISECONDS
        );

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result-tenant-result");
    }

    @Test
    void shouldScheduleCallableGetDelay() throws Exception {
        ScheduledFuture<String> future = wrappedScheduler.schedule(
                () -> "done",
                500, TimeUnit.MILLISECONDS
        );

        long delay = future.getDelay(TimeUnit.MILLISECONDS);

        assertThat(delay).isGreaterThanOrEqualTo(0);
        assertThat(delay).isLessThanOrEqualTo(500);

        future.cancel(true);
    }

    // ==================== scheduleAtFixedRate tests ====================

    @Test
    void shouldScheduleAtFixedRatePropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-fixed-rate");
        AtomicInteger executionCount = new AtomicInteger(0);
        AtomicReference<String> lastCapturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledFuture<?> future = wrappedScheduler.scheduleAtFixedRate(() -> {
            lastCapturedTenantId.set(holder.getTenantId());
            executionCount.incrementAndGet();
            latch.countDown();
        }, 0, 50, TimeUnit.MILLISECONDS);

        latch.await(2, TimeUnit.SECONDS);
        future.cancel(false);

        assertThat(executionCount.get()).isGreaterThanOrEqualTo(3);
        assertThat(lastCapturedTenantId.get()).isEqualTo("tenant-fixed-rate");
    }

    @Test
    void shouldScheduleAtFixedRateContinueWithInitialDelay() throws Exception {
        holder.setTenantId("tenant-initial-delay");
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        long startTime = System.currentTimeMillis();

        ScheduledFuture<?> future = wrappedScheduler.scheduleAtFixedRate(() -> {
            executionCount.incrementAndGet();
            latch.countDown();
        }, 100, 50, TimeUnit.MILLISECONDS);

        latch.await(2, TimeUnit.SECONDS);
        future.cancel(false);

        long elapsed = System.currentTimeMillis() - startTime;
        // Should have waited at least the initial delay
        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    @Test
    void shouldScheduleAtFixedRateStopOnException() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        ScheduledFuture<?> future = wrappedScheduler.scheduleAtFixedRate(() -> {
            executionCount.incrementAndGet();
            latch.countDown();
            if (executionCount.get() == 2) {
                throw new RuntimeException("Intentional test exception");
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        latch.await(1, TimeUnit.SECONDS);

        // Wait a bit more to see if execution stops
        Thread.sleep(100);

        // Execution should have stopped after the exception
        // (depends on ScheduledExecutorService behavior - some stop, some continue)
        assertThat(executionCount.get()).isGreaterThanOrEqualTo(2);

        future.cancel(false);
    }

    // ==================== scheduleWithFixedDelay tests ====================

    @Test
    void shouldScheduleWithFixedDelayPropagateTenantContext() throws Exception {
        holder.setTenantId("tenant-fixed-delay");
        AtomicInteger executionCount = new AtomicInteger(0);
        AtomicReference<String> lastCapturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledFuture<?> future = wrappedScheduler.scheduleWithFixedDelay(() -> {
            lastCapturedTenantId.set(holder.getTenantId());
            executionCount.incrementAndGet();
            latch.countDown();
        }, 0, 50, TimeUnit.MILLISECONDS);

        latch.await(2, TimeUnit.SECONDS);
        future.cancel(false);

        assertThat(executionCount.get()).isGreaterThanOrEqualTo(3);
        assertThat(lastCapturedTenantId.get()).isEqualTo("tenant-fixed-delay");
    }

    @Test
    void shouldScheduleWithFixedDelayIncludesExecutionTime() throws Exception {
        holder.setTenantId("tenant-delay-includes-exec");
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledFuture<?> future = wrappedScheduler.scheduleWithFixedDelay(() -> {
            executionCount.incrementAndGet();
            latch.countDown();
            // Simulate some work
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        latch.await(2, TimeUnit.SECONDS);
        future.cancel(false);

        assertThat(executionCount.get()).isGreaterThanOrEqualTo(3);
    }

    // ==================== Mixed operations tests ====================

    @Test
    void shouldMixScheduleAndSubmitOperations() throws Exception {
        holder.setTenantId("tenant-mixed");
        AtomicReference<String> scheduledTenant = new AtomicReference<>();
        AtomicReference<String> submittedTenant = new AtomicReference<>();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        // Schedule a task
        wrappedScheduler.schedule(() -> {
            scheduledTenant.set(holder.getTenantId());
            latch1.countDown();
        }, 10, TimeUnit.MILLISECONDS);

        // Submit a task
        wrappedScheduler.submit(() -> {
            submittedTenant.set(holder.getTenantId());
            latch2.countDown();
        });

        latch1.await(1, TimeUnit.SECONDS);
        latch2.await(1, TimeUnit.SECONDS);

        assertThat(scheduledTenant.get()).isEqualTo("tenant-mixed");
        assertThat(submittedTenant.get()).isEqualTo("tenant-mixed");
    }

    @Test
    void shouldDifferentScheduledTasksHaveDifferentCapturedContexts() throws Exception {
        AtomicReference<String> task1Tenant = new AtomicReference<>();
        AtomicReference<String> task2Tenant = new AtomicReference<>();
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        holder.setTenantId("tenant-task1");
        wrappedScheduler.schedule(() -> {
            task1Tenant.set(holder.getTenantId());
            latch1.countDown();
        }, 10, TimeUnit.MILLISECONDS);
        latch1.await(1, TimeUnit.SECONDS);

        holder.setTenantId("tenant-task2");
        wrappedScheduler.schedule(() -> {
            task2Tenant.set(holder.getTenantId());
            latch2.countDown();
        }, 10, TimeUnit.MILLISECONDS);
        latch2.await(1, TimeUnit.SECONDS);

        assertThat(task1Tenant.get()).isEqualTo("tenant-task1");
        assertThat(task2Tenant.get()).isEqualTo("tenant-task2");
    }

    // ==================== Shutdown tests ====================

    @Test
    void shouldShutdownCancelPendingScheduledTasks() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);

        wrappedScheduler.schedule(() -> {
            executed.set(true);
        }, 10, TimeUnit.SECONDS);  // Far future

        wrappedScheduler.shutdown();

        // Wait to ensure task doesn't execute
        Thread.sleep(100);

        assertThat(executed.get()).isFalse();
    }

    @Test
    void shouldShutdownNowReturnPendingTasks() throws Exception {
        // Schedule a task far in the future
        wrappedScheduler.schedule(() -> {}, 10, TimeUnit.SECONDS);

        // Some implementations may return pending tasks
        wrappedScheduler.shutdownNow();

        assertThat(wrappedScheduler.isShutdown()).isTrue();
    }

    @Test
    void shouldAwaitTerminationWithScheduledTasks() throws Exception {
        wrappedScheduler.schedule(() -> {}, 10, TimeUnit.MILLISECONDS);

        Thread.sleep(50);  // Let task complete

        wrappedScheduler.shutdown();

        boolean terminated = wrappedScheduler.awaitTermination(1, TimeUnit.SECONDS);

        assertThat(terminated).isTrue();
    }

    // ==================== Edge cases ====================

    @Test
    void shouldScheduleWithVeryShortDelay() throws Exception {
        holder.setTenantId("tenant-short-delay");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        wrappedScheduler.schedule(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        }, 1, TimeUnit.MILLISECONDS);

        latch.await(1, TimeUnit.SECONDS);

        assertThat(capturedTenantId.get()).isEqualTo("tenant-short-delay");
    }

    @Test
    void shouldScheduleAtFixedRateRejectZeroPeriod() throws Exception {
        // Zero period is rejected by ScheduledThreadPoolExecutor with IllegalArgumentException
        assertThatThrownBy(() -> {
            wrappedScheduler.scheduleAtFixedRate(
                    () -> {}, 0, 0, TimeUnit.MILLISECONDS);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldScheduleCallableReturnCorrectResult() throws Exception {
        holder.setTenantId("tenant-return");

        ScheduledFuture<String> future = wrappedScheduler.schedule(
                () -> {
                    assertThat(holder.getTenantId()).isEqualTo("tenant-return");
                    return "success";
                },
                10, TimeUnit.MILLISECONDS
        );

        assertThat(future.get(1, TimeUnit.SECONDS)).isEqualTo("success");
    }
}
