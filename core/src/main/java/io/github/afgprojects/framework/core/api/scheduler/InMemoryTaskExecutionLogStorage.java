package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存执行日志存储
 *
 * <p>基于内存的执行日志存储实现，适用于单机环境或测试场景
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>基于 ConcurrentHashMap 和 ConcurrentLinkedDeque 实现线程安全</li>
 *   <li>支持最大容量限制，超出自动清理最旧记录</li>
 *   <li>支持按时间范围查询</li>
 *   <li>支持统计功能</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class InMemoryTaskExecutionLogStorage implements TaskExecutionLogStorage {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTaskExecutionLogStorage.class);

    private final int maxSize;
    private final ConcurrentMap<String, ConcurrentLinkedDeque<TaskExecutionLog>> logsByTaskId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskExecutionLog> logsByExecutionId = new ConcurrentHashMap<>();
    private final AtomicLong totalCount = new AtomicLong(0);

    /**
     * 创建内存执行日志存储
     */
    public InMemoryTaskExecutionLogStorage() {
        this(10000);
    }

    /**
     * 创建内存执行日志存储
     *
     * @param maxSize 最大存储数量
     */
    public InMemoryTaskExecutionLogStorage(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void save(@NonNull TaskExecutionLog logEntry) {
        // 检查容量限制
        if (totalCount.get() >= maxSize) {
            evictOldest();
        }

        // 按执行 ID 存储
        logsByExecutionId.put(logEntry.executionId(), logEntry);

        // 按任务 ID 存储
        logsByTaskId.computeIfAbsent(logEntry.taskId(), k -> new ConcurrentLinkedDeque<>())
            .addFirst(logEntry);

        totalCount.incrementAndGet();

        log.debug("Saved execution log: executionId={}, taskId={}", logEntry.executionId(), logEntry.taskId());
    }

    @Override
    public void update(@NonNull TaskExecutionLog logEntry) {
        // 更新按执行 ID 存储
        logsByExecutionId.put(logEntry.executionId(), logEntry);

        // 更新按任务 ID 存储
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(logEntry.taskId());
        if (taskLogs != null) {
            // 移除旧记录并添加新记录
            taskLogs.removeIf(l -> l.executionId().equals(logEntry.executionId()));
            taskLogs.addFirst(logEntry);
        }

        log.debug("Updated execution log: executionId={}, taskId={}", logEntry.executionId(), logEntry.taskId());
    }

    @Override
    @NonNull
    public Optional<TaskExecutionLog> findByExecutionId(@NonNull String executionId) {
        return Optional.ofNullable(logsByExecutionId.get(executionId));
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskId(@NonNull String taskId, int limit) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return taskLogs.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskGroup(@NonNull String taskGroup, int limit) {
        return logsByExecutionId.values().stream()
            .filter(log -> log.taskGroup().equals(taskGroup))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTimeRange(@NonNull String taskId,
                                                   @NonNull Instant from,
                                                   @NonNull Instant to) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return taskLogs.stream()
            .filter(log -> !log.startTime().isBefore(from) && !log.startTime().isAfter(to))
            .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findFailedExecutions(@NonNull String taskId, int limit) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return taskLogs.stream()
            .filter(TaskExecutionLog::isFailed)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public long countByTaskId(@NonNull String taskId) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        return taskLogs != null ? taskLogs.size() : 0;
    }

    @Override
    public long countSuccessByTaskId(@NonNull String taskId) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return 0;
        }

        return taskLogs.stream()
            .filter(TaskExecutionLog::isSuccess)
            .count();
    }

    @Override
    public long countFailedByTaskId(@NonNull String taskId) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return 0;
        }

        return taskLogs.stream()
            .filter(TaskExecutionLog::isFailed)
            .count();
    }

    @Override
    public double getAverageExecutionTime(@NonNull String taskId) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(taskId);
        if (taskLogs == null || taskLogs.isEmpty()) {
            return 0;
        }

        return taskLogs.stream()
            .filter(log -> log.endTime() != null)
            .mapToLong(log -> log.duration().toMillis())
            .average()
            .orElse(0);
    }

    @Override
    public long deleteBefore(@NonNull Instant before) {
        List<String> toRemove = new ArrayList<>();
        long removed = 0;

        for (Map.Entry<String, TaskExecutionLog> entry : logsByExecutionId.entrySet()) {
            if (entry.getValue().startTime().isBefore(before)) {
                toRemove.add(entry.getKey());
            }
        }

        for (String executionId : toRemove) {
            TaskExecutionLog removedLog = logsByExecutionId.remove(executionId);
            if (removedLog != null) {
                ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(removedLog.taskId());
                if (taskLogs != null) {
                    taskLogs.removeIf(l -> l.executionId().equals(executionId));
                }
                removed++;
            }
        }

        totalCount.addAndGet(-removed);
        log.info("Deleted {} execution logs before {}", removed, before);
        return removed;
    }

    @Override
    public void deleteByTaskId(@NonNull String taskId) {
        ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.remove(taskId);
        if (taskLogs != null) {
            for (TaskExecutionLog logEntry : taskLogs) {
                logsByExecutionId.remove(logEntry.executionId());
            }
            totalCount.addAndGet(-taskLogs.size());
        }
        log.info("Deleted all execution logs for task: {}", taskId);
    }

    /**
     * 清理最旧的记录
     */
    private void evictOldest() {
        // 找到最旧的记录
        @Nullable TaskExecutionLog oldest = null;
        for (TaskExecutionLog log : logsByExecutionId.values()) {
            if (oldest == null || log.startTime().isBefore(oldest.startTime())) {
                oldest = log;
            }
        }

        if (oldest != null) {
            logsByExecutionId.remove(oldest.executionId());
            ConcurrentLinkedDeque<TaskExecutionLog> taskLogs = logsByTaskId.get(oldest.taskId());
            if (taskLogs != null) {
                taskLogs.removeLast();
            }
            totalCount.decrementAndGet();
            log.debug("Evicted oldest execution log: executionId={}", oldest.executionId());
        }
    }

    /**
     * 获取当前存储数量
     */
    public long size() {
        return totalCount.get();
    }

    /**
     * 清空所有日志
     */
    public void clear() {
        logsByExecutionId.clear();
        logsByTaskId.clear();
        totalCount.set(0);
        log.info("Cleared all execution logs");
    }
}
