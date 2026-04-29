package io.github.afgprojects.framework.core.trace;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * 追踪上下文传播器
 * <p>
 * 支持跨线程传播追踪上下文，确保异步任务能够保持链路追踪连续性。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>跨线程传播 traceId 和 baggage</li>
 *   <li>支持 {@link Runnable} 和 {@link Callable} 包装</li>
 *   <li>支持线程池自动包装</li>
 *   <li>支持父子 Span 关联</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 包装 Runnable
 * Runnable task = () -> doSomething();
 * Runnable tracedTask = TraceContextPropagator.wrap(task);
 * executor.submit(tracedTask);
 *
 * // 包装 Callable
 * Callable<String> task = () -> computeResult();
 * Callable<String> tracedTask = TraceContextPropagator.wrap(task);
 * executor.submit(tracedTask);
 *
 * // 创建自动传播的线程池
 * ExecutorService executor = TraceContextPropagator.wrapExecutor(
 *     Executors.newFixedThreadPool(10)
 * );
 * }</pre>
 */
public final class TraceContextPropagator {

    private TraceContextPropagator() {}

    /**
     * 包装 Runnable，使其在执行时携带当前追踪上下文
     *
     * @param task 原始任务
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable task) {
        return wrap(task, null);
    }

    /**
     * 包装 Runnable，使其在执行时携带指定的追踪上下文
     *
     * @param task      原始任务
     * @param context   追踪上下文快照，如果为 null 则使用当前上下文
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable task, @Nullable TraceContextSnapshot context) {
        TraceContextSnapshot snapshot = context != null ? context : capture();
        return () -> {
            try (Restore ignored = restore(snapshot)) {
                task.run();
            }
        };
    }

    /**
     * 包装 Callable，使其在执行时携带当前追踪上下文
     *
     * @param task 原始任务
     * @param <T>  返回类型
     * @return 包装后的任务
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        return wrap(task, null);
    }

    /**
     * 包装 Callable，使其在执行时携带指定的追踪上下文
     *
     * @param task    原始任务
     * @param context 追踪上下文快照，如果为 null 则使用当前上下文
     * @param <T>     返回类型
     * @return 包装后的任务
     */
    public static <T> Callable<T> wrap(Callable<T> task, @Nullable TraceContextSnapshot context) {
        TraceContextSnapshot snapshot = context != null ? context : capture();
        return () -> {
            try (Restore ignored = restore(snapshot)) {
                return task.call();
            }
        };
    }

    /**
     * 包装 ExecutorService，使其自动传播追踪上下文
     *
     * @param executor 原始线程池
     * @return 包装后的线程池
     */
    public static ExecutorService wrapExecutor(ExecutorService executor) {
        return new TracingExecutorService(executor);
    }

    /**
     * 创建追踪上下文传播的线程工厂
     *
     * @param prefix 线程名称前缀
     * @return 线程工厂
     */
    public static ThreadFactory tracingThreadFactory(String prefix) {
        return new TracingThreadFactory(prefix);
    }

    /**
     * 捕获当前追踪上下文
     *
     * @return 追踪上下文快照
     */
    public static TraceContextSnapshot capture() {
        Tracer tracer = TraceContext.getTracer();
        Span currentSpan = tracer != null ? tracer.currentSpan() : null;

        String traceId = TraceContext.getTraceId();
        String requestId = TraceContext.getRequestId();
        Map<String, String> baggage = BaggageContext.getAll();

        return new TraceContextSnapshot(
                traceId, requestId, baggage, tracer, currentSpan);
    }

    /**
     * 恢复追踪上下文
     *
     * @param snapshot 追踪上下文快照
     * @return 可关闭的恢复对象，用于在作用域结束后清理上下文
     */
    public static Restore restore(TraceContextSnapshot snapshot) {
        Tracer tracer = snapshot.tracer();
        Span span = null;

        // 如果有 Tracer，创建新 Span 作为子 Span
        if (tracer != null) {
            Span parentSpan = snapshot.parentSpan();
            if (parentSpan != null) {
                span = tracer.spanBuilder()
                        .setParent(parentSpan.context())
                        .name("async-task")
                        .start();
            } else if (snapshot.traceId() != null) {
                // 只有 traceId，没有 parentSpan，创建新 Span 并关联 traceId
                span = tracer.nextSpan().name("async-task");
                span.start();
            }
        }

        // 恢复 Baggage
        Map<String, String> previousBaggage = BaggageContext.getAll();
        snapshot.baggage().forEach(BaggageContext::set);

        return new Restore(span, previousBaggage);
    }

    /**
     * 追踪上下文快照
     *
     * @param traceId     TraceId
     * @param requestId  RequestId
     * @param baggage     Baggage 数据
     * @param tracer      Micrometer Tracer
     * @param parentSpan  父 Span
     */
    public record TraceContextSnapshot(
            @Nullable String traceId,
            @Nullable String requestId,
            Map<String, String> baggage,
            @Nullable Tracer tracer,
            @Nullable Span parentSpan) {

        /**
         * 判断快照是否有效
         *
         * @return 是否有追踪信息
         */
        public boolean isValid() {
            return traceId != null || !baggage.isEmpty();
        }
    }

    /**
     * 追踪上下文恢复对象
     * <p>
     * 实现 AutoCloseable，支持 try-with-resources 语法
     * </p>
     */
    public static class Restore implements AutoCloseable {

        private final @Nullable Span span;
        private final Map<String, String> previousBaggage;

        Restore(@Nullable Span span, Map<String, String> previousBaggage) {
            this.span = span;
            this.previousBaggage = previousBaggage;
        }

        @Override
        public void close() {
            // 关闭 Span
            if (span != null) {
                span.end();
            }

            // 恢复之前的 Baggage
            BaggageContext.clear();
            previousBaggage.forEach(BaggageContext::set);
        }
    }

    /**
     * 追踪线程工厂
     */
    private static class TracingThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        TracingThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(wrap(r), prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * 追踪执行器服务
     * <p>
     * 包装所有提交的任务，自动传播追踪上下文
     * </p>
     */
    private static class TracingExecutorService implements ExecutorService {

        private final ExecutorService delegate;

        TracingExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(wrap(command));
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(Callable<T> task) {
            return delegate.submit(wrap(task));
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
            return delegate.submit(wrap(task), result);
        }

        @Override
        public java.util.concurrent.Future<?> submit(Runnable task) {
            return delegate.submit(wrap(task));
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(wrapCallables(tasks));
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks)
                throws InterruptedException, java.util.concurrent.ExecutionException {
            return delegate.invokeAny(wrapCallables(tasks));
        }

        @Override
        public <T> T invokeAny(
                java.util.Collection<? extends Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit)
                throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
            return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
        }

        private <T> java.util.Collection<Callable<T>> wrapCallables(
                java.util.Collection<? extends Callable<T>> tasks) {
            return tasks.stream().map(TraceContextPropagator::wrap).toList();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
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
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }
}