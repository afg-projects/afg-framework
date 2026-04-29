package io.github.afgprojects.framework.core.cloud;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * 优雅停机生命周期
 *
 * <p>实现 Spring SmartLifecycle 接口，支持优雅停机
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持停机优先级</li>
 *   <li>支持停机超时</li>
 *   <li>支持停机回调</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class GracefulShutdownLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownLifecycle.class);

    private final String name;
    private final int phase;
    private final long timeoutMs;
    private final @Nullable Runnable shutdownCallback;
    private volatile boolean running = false;

    /**
     * 创建优雅停机生命周期
     *
     * @param name             名称
     * @param phase            阶段（数值越小越先停机）
     * @param timeoutMs        超时时间（毫秒）
     * @param shutdownCallback 停机回调
     */
    public GracefulShutdownLifecycle(
            @NonNull String name, int phase, long timeoutMs, @Nullable Runnable shutdownCallback) {
        this.name = name;
        this.phase = phase;
        this.timeoutMs = timeoutMs;
        this.shutdownCallback = shutdownCallback;
    }

    @Override
    public void start() {
        log.info("Starting lifecycle: {}", name);
        running = true;
    }

    @Override
    public void stop() {
        log.info("Stopping lifecycle: {}", name);
        try {
            if (shutdownCallback != null) {
                shutdownCallback.run();
            }
        } catch (Exception e) {
            log.error("Error during shutdown callback for: {}", name, e);
        }
        running = false;
        log.info("Stopped lifecycle: {}", name);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return phase;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(@NonNull Runnable callback) {
        log.info("Stopping lifecycle with callback: {}", name);
        long startTime = System.currentTimeMillis();

        try {
            if (shutdownCallback != null) {
                shutdownCallback.run();
            }
        } catch (Exception e) {
            log.error("Error during shutdown callback for: {}", name, e);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > timeoutMs) {
            log.warn("Shutdown exceeded timeout for {}: {}ms > {}ms", name, elapsed, timeoutMs);
        }

        running = false;
        callback.run();
        log.info("Stopped lifecycle with callback: {}", name);
    }

    /**
     * 获取名称
     *
     * @return 名称
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * 获取超时时间
     *
     * @return 超时时间（毫秒）
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}