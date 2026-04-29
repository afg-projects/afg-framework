package io.github.afgprojects.framework.core.web.shutdown;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 优雅关闭钩子，管理分阶段关闭流程。
 * 实现 BeanPostProcessor 扫描并注册 @ShutdownOrder 注解的方法。
 * 实现 DisposableBean 在销毁时执行关闭回调。
 */
@EnableConfigurationProperties(ShutdownProperties.class)
@SuppressWarnings({
    "PMD.AvoidAccessibilityAlteration",
    "PMD.CommentDefaultAccessModifier",
    "PMD.AvoidUncheckedExceptionsInSignatures",
    "PMD.AvoidCatchingGenericException"
})
public class ShutdownHook implements DisposableBean, ApplicationContextAware, BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);

    private final ShutdownProperties properties;

    private final Map<String, List<ShutdownCallback>> callbacks = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    public ShutdownHook(ShutdownProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 扫描 Bean 中的 @ShutdownOrder 注解方法并注册为回调。
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, @NonNull String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            ShutdownOrder annotation = method.getAnnotation(ShutdownOrder.class);
            if (annotation != null) {
                method.setAccessible(true);
                String phase = annotation.phase();
                int order = annotation.order();
                Runnable callback = createCallback(bean, method, beanName, method.getName());
                addCallback(phase, order, callback);
                LOG.debug(
                        "Registered shutdown callback: bean={}, method={}, phase={}, order={}",
                        beanName,
                        method.getName(),
                        phase,
                        order);
            }
        }
        return bean;
    }

    private Runnable createCallback(Object bean, Method method, String beanName, String methodName) {
        return () -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                LOG.error("Failed to execute shutdown callback: bean={}, method={}", beanName, methodName, e);
            }
        };
    }

    /**
     * 为指定阶段添加关闭回调。
     */
    public void addCallback(String phase, int order, Runnable callback) {
        callbacks.computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>()).add(new ShutdownCallback(order, callback));
    }

    /**
     * 在 Bean 销毁时执行关闭回调。
     * 每个阶段按配置的超时时间执行。
     */
    @Override
    public void destroy() {
        LOG.info(
                "Starting graceful shutdown with {} phases",
                properties.getPhases().size());
        for (ShutdownProperties.Phase phase : properties.getPhases()) {
            executePhaseWithTimeout(phase.getName(), phase.getTimeout());
        }
        LOG.info("Graceful shutdown completed");
    }

    /**
     * 在超时时间内执行阶段。
     */
    void executePhaseWithTimeout(String phaseName, Duration timeout) {
        List<ShutdownCallback> phaseCallbacks = callbacks.get(phaseName);
        if (phaseCallbacks == null || phaseCallbacks.isEmpty()) {
            LOG.debug("Skipping phase '{}' - no callbacks registered", phaseName);
            return;
        }

        LOG.info("Executing shutdown phase '{}' with timeout {}ms", phaseName, timeout.toMillis());

        try {
            CompletableFuture.runAsync(() -> executePhase(phaseName)).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            LOG.info("Phase '{}' completed successfully", phaseName);
        } catch (TimeoutException e) {
            LOG.warn("Phase '{}' timed out after {}ms", phaseName, timeout.toMillis());
        } catch (Exception e) {
            LOG.error("Phase '{}' failed with error", phaseName, e);
        }
    }

    /**
     * 执行阶段中的回调，按 order 值升序排列。
     * 启用并行执行时，相同 order 的回调并行执行。
     */
    void executePhase(String phaseName) {
        List<ShutdownCallback> phaseCallbacks = callbacks.get(phaseName);
        if (phaseCallbacks == null) {
            return;
        }

        // 按 order 排序（升序）
        List<ShutdownCallback> sortedCallbacks = new ArrayList<>(phaseCallbacks);
        Collections.sort(sortedCallbacks);

        if (properties.isParallelExecutionEnabled()) {
            executePhaseInParallel(phaseName, sortedCallbacks);
        } else {
            executePhaseSequentially(phaseName, sortedCallbacks);
        }
    }

    /**
     * 顺序执行回调（默认行为）。
     */
    private void executePhaseSequentially(String phaseName, List<ShutdownCallback> sortedCallbacks) {
        for (ShutdownCallback callback : sortedCallbacks) {
            try {
                callback.runnable().run();
                LOG.debug("Executed shutdown callback in phase '{}' with order {}", phaseName, callback.order());
            } catch (Exception e) {
                LOG.error("Error executing shutdown callback in phase '{}'", phaseName, e);
            }
        }
    }

    /**
     * 对相同 order 的回调进行并行执行。
     * 相同 order 的回调并行执行，不同 order 的组按顺序执行。
     */
    private void executePhaseInParallel(String phaseName, List<ShutdownCallback> sortedCallbacks) {
        // 按 order 分组，相同 order 的回调并行执行，不同 order 的组按顺序执行
        int currentIndex = 0;
        while (currentIndex < sortedCallbacks.size()) {
            int currentOrder = sortedCallbacks.get(currentIndex).order();

            // 找出所有相同 order 的回调
            List<ShutdownCallback> sameOrderCallbacks = new ArrayList<>();
            while (currentIndex < sortedCallbacks.size()
                    && sortedCallbacks.get(currentIndex).order() == currentOrder) {
                sameOrderCallbacks.add(sortedCallbacks.get(currentIndex));
                currentIndex++;
            }

            // 执行相同 order 的回调（并行或单独执行）
            if (sameOrderCallbacks.size() == 1) {
                // 单个回调，直接执行
                try {
                    sameOrderCallbacks.get(0).runnable().run();
                    LOG.debug(
                            "Executed shutdown callback in phase '{}' with order {}",
                            phaseName,
                            currentOrder);
                } catch (Exception e) {
                    LOG.error("Error executing shutdown callback in phase '{}'", phaseName, e);
                }
            } else {
                // 多个回调，并行执行并等待所有完成
                CompletableFuture<?>[] futures = sameOrderCallbacks.stream()
                        .map(callback -> CompletableFuture.runAsync(() -> {
                            try {
                                callback.runnable().run();
                                LOG.debug(
                                        "Executed shutdown callback in phase '{}' with order {}",
                                        phaseName,
                                        callback.order());
                            } catch (Exception e) {
                                LOG.error("Error executing shutdown callback in phase '{}'", phaseName, e);
                            }
                        }))
                        .toArray(CompletableFuture[]::new);

                // 等待当前 order 组的所有回调完成
                CompletableFuture.allOf(futures).join();
            }
        }
    }

    /**
     * 返回回调映射（用于测试）。
     */
    Map<String, List<ShutdownCallback>> getCallbacks() {
        return callbacks;
    }

    /**
     * 表示关闭回调及其顺序的记录类。
     */
    record ShutdownCallback(int order, Runnable runnable) implements Comparable<ShutdownCallback> {
        @Override
        public int compareTo(ShutdownCallback other) {
            return Integer.compare(this.order, other.order);
        }
    }
}
