package io.github.afgprojects.framework.data.core.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextPropagationTest {

    @Test
    void shouldSnapshotTenantContext() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.setTenantId("tenant-001");

        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.tenantId()).isEqualTo("tenant-001");
        assertThat(snapshot.isValid()).isTrue();

        holder.clear();
    }

    @Test
    void shouldReturnNullSnapshotWhenNoTenantSet() {
        TenantContextHolder holder = new TenantContextHolder();

        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();

        assertThat(snapshot).isNull();
    }

    @Test
    void shouldRestoreFromSnapshot() {
        TenantContextHolder holder = new TenantContextHolder();
        TenantContextHolder.TenantContextSnapshot snapshot = new TenantContextHolder.TenantContextSnapshot("tenant-002");

        holder.restore(snapshot);

        assertThat(holder.getTenantId()).isEqualTo("tenant-002");

        holder.clear();
    }

    @Test
    void shouldClearWhenRestoreNullSnapshot() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.setTenantId("tenant-001");

        holder.restore(null);

        assertThat(holder.getTenantId()).isNull();
    }

    @Test
    void shouldRunWithSnapshot() {
        TenantContextHolder holder = new TenantContextHolder();
        TenantContextHolder.TenantContextSnapshot snapshot = new TenantContextHolder.TenantContextSnapshot("tenant-003");
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        holder.runWithSnapshot(snapshot, () -> capturedTenantId.set(holder.getTenantId()));

        assertThat(capturedTenantId.get()).isEqualTo("tenant-003");
        assertThat(holder.getTenantId()).isNull(); // 恢复为原始值（null）
    }

    @Test
    void shouldRestorePreviousTenantAfterRunWithSnapshot() {
        TenantContextHolder holder = new TenantContextHolder();
        holder.setTenantId("tenant-original");
        TenantContextHolder.TenantContextSnapshot snapshot = new TenantContextHolder.TenantContextSnapshot("tenant-temp");

        holder.runWithSnapshot(snapshot, () -> {});

        assertThat(holder.getTenantId()).isEqualTo("tenant-original");

        holder.clear();
    }

    @Test
    void shouldPropagateTenantContextViaTaskDecorator() throws InterruptedException {
        TenantContextHolder holder = new TenantContextHolder();
        TenantContextTaskDecorator decorator = new TenantContextTaskDecorator(holder);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        holder.setTenantId("tenant-async");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable decoratedTask = decorator.decorate(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        executor.submit(decoratedTask);
        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(capturedTenantId.get()).isEqualTo("tenant-async");

        holder.clear();
    }

    @Test
    void shouldNotPropagateWhenNoTenantSet() throws InterruptedException {
        TenantContextHolder holder = new TenantContextHolder();
        TenantContextTaskDecorator decorator = new TenantContextTaskDecorator(holder);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTenantId = new AtomicReference<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable decoratedTask = decorator.decorate(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        executor.submit(decoratedTask);
        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(capturedTenantId.get()).isNull();
    }

    @Test
    void shouldPropagateTenantContextViaWrappedExecutorService() throws Exception {
        TenantContextHolder holder = new TenantContextHolder();
        ExecutorService originalExecutor = Executors.newFixedThreadPool(2);
        ExecutorService wrappedExecutor = TenantContextPropagatingExecutorService.wrap(originalExecutor, holder);

        holder.setTenantId("tenant-executor");

        AtomicReference<String> capturedTenantId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        wrappedExecutor.submit(() -> {
            capturedTenantId.set(holder.getTenantId());
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);
        wrappedExecutor.shutdown();

        assertThat(capturedTenantId.get()).isEqualTo("tenant-executor");

        holder.clear();
    }

    @Test
    void shouldPropagateTenantContextViaCompletableFuture() throws Exception {
        TenantContextHolder holder = new TenantContextHolder();
        ExecutorService wrappedExecutor = TenantContextPropagatingExecutorService.wrap(
                Executors.newFixedThreadPool(2), holder);

        holder.setTenantId("tenant-future");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return holder.getTenantId();
        }, wrappedExecutor);

        String result = future.get(1, TimeUnit.SECONDS);
        wrappedExecutor.shutdown();

        assertThat(result).isEqualTo("tenant-future");

        holder.clear();
    }

    @Test
    void shouldPropagateTenantContextWithCallable() throws Exception {
        TenantContextHolder holder = new TenantContextHolder();
        ExecutorService wrappedExecutor = TenantContextPropagatingExecutorService.wrap(
                Executors.newSingleThreadExecutor(), holder);

        holder.setTenantId("tenant-callable");

        String result = wrappedExecutor.submit(() -> holder.getTenantId()).get(1, TimeUnit.SECONDS);
        wrappedExecutor.shutdown();

        assertThat(result).isEqualTo("tenant-callable");

        holder.clear();
    }

    @Test
    void shouldNotLeakTenantContextBetweenTasks() throws Exception {
        TenantContextHolder holder = new TenantContextHolder();
        ExecutorService wrappedExecutor = TenantContextPropagatingExecutorService.wrap(
                Executors.newSingleThreadExecutor(), holder);

        AtomicReference<String> firstTaskTenantId = new AtomicReference<>();
        AtomicReference<String> secondTaskTenantId = new AtomicReference<>();
        CountDownLatch firstLatch = new CountDownLatch(1);
        CountDownLatch secondLatch = new CountDownLatch(1);

        // 第一个任务设置租户
        holder.setTenantId("tenant-first");
        wrappedExecutor.submit(() -> {
            firstTaskTenantId.set(holder.getTenantId());
            firstLatch.countDown();
        });
        firstLatch.await(1, TimeUnit.SECONDS);

        // 清除当前线程的租户，提交第二个任务
        holder.clear();
        wrappedExecutor.submit(() -> {
            secondTaskTenantId.set(holder.getTenantId());
            secondLatch.countDown();
        });
        secondLatch.await(1, TimeUnit.SECONDS);

        wrappedExecutor.shutdown();

        // 验证第一个任务有正确的租户，第二个任务没有（因为提交时没有租户）
        assertThat(firstTaskTenantId.get()).isEqualTo("tenant-first");
        assertThat(secondTaskTenantId.get()).isNull();
    }
}
