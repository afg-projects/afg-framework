package io.github.afgprojects.framework.data.core.context;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 租户上下文传播执行器服务
 * <p>
 * 包装 {@link ExecutorService}，自动将租户上下文传播到所有提交的任务中。
 * <p>
 * 适用于手动创建线程池、CompletableFuture 等场景。
 *
 * <h3>使用示例</h3>
 * <pre>
 * TenantContextHolder holder = new TenantContextHolder();
 * ExecutorService originalExecutor = Executors.newFixedThreadPool(4);
 * ExecutorService propagatingExecutor = TenantContextPropagatingExecutorService.wrap(originalExecutor, holder);
 *
 * // 使用 propagatingExecutor 提交任务，租户上下文会自动传播
 * holder.setTenantId("tenant-001");
 * propagatingExecutor.submit(() -&gt; {
 *     // 这里的 holder.getTenantId() 返回 "tenant-001"
 * });
 * </pre>
 *
 * @see TenantContextHolder
 * @see ExecutorService
 */
public class TenantContextPropagatingExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final TenantContextHolder tenantContextHolder;

    private TenantContextPropagatingExecutorService(
            @NonNull ExecutorService delegate,
            @NonNull TenantContextHolder tenantContextHolder) {
        this.delegate = delegate;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * 包装 ExecutorService，使其支持租户上下文传播
     *
     * @param executor           原始 ExecutorService
     * @param tenantContextHolder 租户上下文持有者
     * @return 支持租户上下文传播的 ExecutorService
     */
    public static @NonNull ExecutorService wrap(
            @NonNull ExecutorService executor,
            @NonNull TenantContextHolder tenantContextHolder) {
        return new TenantContextPropagatingExecutorService(executor, tenantContextHolder);
    }

    /**
     * 包装 ScheduledExecutorService，使其支持租户上下文传播
     *
     * @param executor           原始 ScheduledExecutorService
     * @param tenantContextHolder 租户上下文持有者
     * @return 支持租户上下文传播的 ScheduledExecutorService
     */
    public static @NonNull ScheduledExecutorService wrap(
            @NonNull ScheduledExecutorService executor,
            @NonNull TenantContextHolder tenantContextHolder) {
        return new TenantContextPropagatingScheduledExecutorService(executor, tenantContextHolder);
    }

    /**
     * 包装 Runnable，使其携带租户上下文
     */
    private static Runnable wrapRunnable(Runnable task, TenantContextHolder holder) {
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();
        return () -> holder.runWithSnapshot(snapshot, task);
    }

    /**
     * 包装 Callable，使其携带租户上下文
     */
    private static <T> Callable<T> wrapCallable(Callable<T> task, TenantContextHolder holder) {
        TenantContextHolder.TenantContextSnapshot snapshot = holder.snapshot();
        return () -> {
            holder.restore(snapshot);
            try {
                return task.call();
            } finally {
                holder.clear();
            }
        };
    }

    /**
     * 包装 Runnable，使其携带租户上下文
     */
    private Runnable wrapRunnable(Runnable task) {
        return wrapRunnable(task, tenantContextHolder);
    }

    /**
     * 包装 Callable，使其携带租户上下文
     */
    private <T> Callable<T> wrapCallable(Callable<T> task) {
        return wrapCallable(task, tenantContextHolder);
    }

    // ==================== ExecutorService 委托方法 ====================

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public @NonNull List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> @NonNull Future<T> submit(@NonNull Callable<T> task) {
        return delegate.submit(wrapCallable(task));
    }

    @Override
    public <T> @NonNull Future<T> submit(@NonNull Runnable task, T result) {
        return delegate.submit(wrapRunnable(task), result);
    }

    @Override
    public @NonNull Future<?> submit(@NonNull Runnable task) {
        return delegate.submit(wrapRunnable(task));
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.@NonNull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks));
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(
            java.util.@NonNull Collection<? extends Callable<T>> tasks,
            long timeout,
            @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(java.util.@NonNull Collection<? extends Callable<T>> tasks)
            throws java.util.concurrent.ExecutionException, InterruptedException {
        return delegate.invokeAny(wrapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(
            java.util.@NonNull Collection<? extends Callable<T>> tasks,
            long timeout,
            @NonNull TimeUnit unit)
            throws java.util.concurrent.ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
        return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(wrapRunnable(command));
    }

    private <T> java.util.List<Callable<T>> wrapCallables(java.util.Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
                .map(this::wrapCallable)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 租户上下文传播的定时执行器服务
     */
    private static class TenantContextPropagatingScheduledExecutorService
            extends TenantContextPropagatingExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService scheduledDelegate;
        private final TenantContextHolder holder;

        TenantContextPropagatingScheduledExecutorService(
                @NonNull ScheduledExecutorService delegate,
                @NonNull TenantContextHolder tenantContextHolder) {
            super(delegate, tenantContextHolder);
            this.scheduledDelegate = delegate;
            this.holder = tenantContextHolder;
        }

        @Override
        public @NonNull ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
            return scheduledDelegate.schedule(wrapRunnable(command, holder), delay, unit);
        }

        @Override
        public <V> @NonNull ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
            return scheduledDelegate.schedule(wrapCallable(callable, holder), delay, unit);
        }

        @Override
        public @NonNull ScheduledFuture<?> scheduleAtFixedRate(
                @NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
            return scheduledDelegate.scheduleAtFixedRate(wrapRunnable(command, holder), initialDelay, period, unit);
        }

        @Override
        public @NonNull ScheduledFuture<?> scheduleWithFixedDelay(
                @NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
            return scheduledDelegate.scheduleWithFixedDelay(wrapRunnable(command, holder), initialDelay, delay, unit);
        }
    }
}