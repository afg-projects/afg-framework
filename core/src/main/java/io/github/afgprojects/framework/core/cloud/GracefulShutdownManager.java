package io.github.afgprojects.framework.core.cloud;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.cloud.CloudNativeProperties.GracefulShutdownConfig;

/**
 * 优雅停机管理器
 *
 * <p>管理应用的优雅停机流程
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持注册停机回调</li>
 *   <li>支持停机优先级</li>
 *   <li>支持停机超时</li>
 *   <li>支持停机状态追踪</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class GracefulShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownManager.class);

    private final GracefulShutdownConfig config;
    private final List<ShutdownHook> shutdownHooks = new CopyOnWriteArrayList<>();
    private final Map<String, ShutdownStatus> shutdownStatus = new ConcurrentHashMap<>();
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private volatile boolean shuttingDown = false;

    /**
     * 创建优雅停机管理器
     *
     * @param config 配置
     */
    public GracefulShutdownManager(@NonNull GracefulShutdownConfig config) {
        this.config = config;
    }

    /**
     * 注册停机回调
     *
     * @param name     名称
     * @param phase    阶段（数值越小越先执行）
     * @param callback 回调
     */
    public void register(@NonNull String name, int phase, @NonNull Runnable callback) {
        shutdownHooks.add(new ShutdownHook(name, phase, callback));
        log.info("Registered shutdown hook: {} (phase={})", name, phase);
    }

    /**
     * 注册停机回调（默认阶段）
     *
     * @param name     名称
     * @param callback 回调
     */
    public void register(@NonNull String name, @NonNull Runnable callback) {
        register(name, 0, callback);
    }

    /**
     * 执行优雅停机
     */
    public void shutdown() {
        if (shuttingDown) {
            log.warn("Shutdown already in progress");
            return;
        }

        shuttingDown = true;
        log.info("Starting graceful shutdown, timeout: {}ms", config.getTimeout().toMillis());

        long startTime = System.currentTimeMillis();

        // 等待活跃请求完成
        if (config.isWaitForRequests()) {
            waitForActiveRequests();
        }

        // 按阶段排序执行停机回调
        List<ShutdownHook> sortedHooks = shutdownHooks.stream()
                .sorted(Comparator.comparingInt(ShutdownHook::phase))
                .toList();

        for (ShutdownHook hook : sortedHooks) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = config.getTimeout().toMillis() - elapsed;

            if (remaining <= 0) {
                log.warn("Shutdown timeout exceeded, skipping remaining hooks");
                break;
            }

            executeHook(hook, remaining);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Graceful shutdown completed in {}ms", totalTime);
    }

    /**
     * 增加活跃请求计数
     */
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
    }

    /**
     * 减少活跃请求计数
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    /**
     * 获取活跃请求数
     *
     * @return 活跃请求数
     */
    public int getActiveRequests() {
        return activeRequests.get();
    }

    /**
     * 是否正在停机
     *
     * @return 是否正在停机
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * 获取停机状态
     *
     * @param name 名称
     * @return 停机状态
     */
    public @Nullable ShutdownStatus getShutdownStatus(@NonNull String name) {
        return shutdownStatus.get(name);
    }

    private void waitForActiveRequests() {
        long startTime = System.currentTimeMillis();
        long timeout = config.getRequestWaitTimeout().toMillis();

        while (activeRequests.get() > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
                log.warn("Wait for active requests timed out, remaining: {}", activeRequests.get());
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("Active requests cleared, count: {}", activeRequests.get());
    }

    private void executeHook(ShutdownHook hook, long remainingMs) {
        String name = hook.name();
        log.info("Executing shutdown hook: {}", name);

        long startTime = System.currentTimeMillis();
        shutdownStatus.put(name, ShutdownStatus.RUNNING);

        try {
            hook.callback().run();
            shutdownStatus.put(name, ShutdownStatus.COMPLETED);
            log.info("Shutdown hook completed: {} ({}ms)", name, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            shutdownStatus.put(name, ShutdownStatus.FAILED);
            log.error("Shutdown hook failed: {}", name, e);
        }
    }

    /**
     * 停机钩子
     */
    public record ShutdownHook(String name, int phase, Runnable callback) {}

    /**
     * 停机状态
     */
    public enum ShutdownStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}