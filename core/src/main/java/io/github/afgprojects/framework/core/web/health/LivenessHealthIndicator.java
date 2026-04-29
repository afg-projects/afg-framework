package io.github.afgprojects.framework.core.web.health;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * 存活探针健康指示器
 * 用于 Kubernetes Liveness Probe，检查应用是否存活
 *
 * <p>检查内容：
 * <ul>
 *   <li>JVM 内存状态</li>
 *   <li>线程死锁检测</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class LivenessHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(LivenessHealthIndicator.class);

    private final HealthCheckProperties properties;

    /**
     * 构造函数
     *
     * @param properties 健康检查配置
     */
    public LivenessHealthIndicator(@NonNull HealthCheckProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // JVM 内存检查
        if (properties.getLiveness().isMemoryCheckEnabled()) {
            checkMemory(builder);
        }

        // 线程死锁检查
        if (properties.getLiveness().isDeadlockDetectionEnabled()) {
            checkDeadlock(builder);
        }

        return builder.build();
    }

    /**
     * 检查 JVM 内存状态
     */
    private void checkMemory(Health.Builder builder) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();

        // 避免除零
        int heapUsagePercent = heapMax > 0 ? (int) ((heapUsed * 100) / heapMax) : 0;

        builder.withDetail("heapUsed", formatBytes(heapUsed))
                .withDetail("heapMax", formatBytes(heapMax))
                .withDetail("heapUsagePercent", heapUsagePercent + "%")
                .withDetail("nonHeapUsed", formatBytes(nonHeapUsed));

        // 检查内存阈值
        int criticalThreshold = properties.getLiveness().getMemoryCriticalThreshold();
        if (heapUsagePercent >= criticalThreshold) {
            log.warn("内存使用率过高: {}%, 已达到严重阈值: {}%", heapUsagePercent, criticalThreshold);
            builder.status(Status.DOWN);
            builder.withDetail("memoryStatus", "CRITICAL");
        } else {
            int warningThreshold = properties.getLiveness().getMemoryWarningThreshold();
            if (heapUsagePercent >= warningThreshold) {
                log.warn("内存使用率较高: {}%, 已达到告警阈值: {}%", heapUsagePercent, warningThreshold);
                builder.withDetail("memoryStatus", "WARNING");
            } else {
                builder.withDetail("memoryStatus", "NORMAL");
            }
        }
    }

    /**
     * 检查线程死锁
     */
    private void checkDeadlock(Health.Builder builder) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        boolean hasDeadlock = deadlockedThreads != null && deadlockedThreads.length > 0;

        builder.withDetail("deadlockDetected", hasDeadlock);

        if (hasDeadlock) {
            log.error("检测到线程死锁，涉及 {} 个线程", deadlockedThreads.length);
            builder.status(Status.DOWN);
            builder.withDetail("deadlockedThreadCount", deadlockedThreads.length);

            // 记录死锁线程信息
            StringBuilder threadInfo = new StringBuilder();
            for (long threadId : deadlockedThreads) {
                threadInfo.append(threadId).append(",");
            }
            if (threadInfo.length() > 0) {
                threadInfo.setLength(threadInfo.length() - 1);
            }
            builder.withDetail("deadlockedThreads", threadInfo.toString());
        } else {
            builder.withDetail("deadlockedThreadCount", 0);
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        long kb = bytes / 1024;
        if (kb < 1024) {
            return kb + " KB";
        }
        long mb = kb / 1024;
        if (mb < 1024) {
            return mb + " MB";
        }
        long gb = mb / 1024;
        return gb + " GB";
    }
}
