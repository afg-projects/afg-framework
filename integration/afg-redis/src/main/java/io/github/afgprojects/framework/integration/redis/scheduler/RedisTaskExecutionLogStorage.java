package io.github.afgprojects.framework.integration.redis.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;

/**
 * Redis 执行日志存储
 *
 * <p>基于 Redis 的执行日志存储实现，适用于分布式环境
 *
 * <h3>数据结构</h3>
 * <ul>
 *   <li>执行日志存储: Hash (Key: afg:scheduler:log:{executionId})</li>
 *   <li>任务索引: Sorted Set (Key: afg:scheduler:task:{taskId}, Score: startTime)</li>
 *   <li>失败索引: Sorted Set (Key: afg:scheduler:task:{taskId}:failed)</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>
 * afg:
 *   scheduler:
 *     log-storage:
 *       type: redis
 *       redis:
 *         key-prefix: afg:scheduler
 *         retention-days: 7
 * </pre>
 *
 * @since 1.0.0
 */
public class RedisTaskExecutionLogStorage implements TaskExecutionLogStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisTaskExecutionLogStorage.class);

    private static final String DEFAULT_KEY_PREFIX = "afg:scheduler";
    private static final String LOG_KEY_PATTERN = "%s:log:%s";
    private static final String TASK_INDEX_KEY_PATTERN = "%s:task:%s";
    private static final String TASK_FAILED_INDEX_KEY_PATTERN = "%s:task:%s:failed";

    private final RedissonClient redissonClient;
    private final String keyPrefix;
    private final long retentionMs;

    /**
     * 创建 Redis 执行日志存储
     *
     * @param redissonClient Redisson 客户端
     */
    public RedisTaskExecutionLogStorage(@NonNull RedissonClient redissonClient) {
        this(redissonClient, DEFAULT_KEY_PREFIX, 7);
    }

    /**
     * 创建 Redis 执行日志存储
     *
     * @param redissonClient Redisson 客户端
     * @param keyPrefix      Redis 键前缀
     * @param retentionDays  保留天数
     */
    public RedisTaskExecutionLogStorage(@NonNull RedissonClient redissonClient,
                                         @NonNull String keyPrefix,
                                         int retentionDays) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.retentionMs = retentionDays * 24L * 60 * 60 * 1000;

        log.info("RedisTaskExecutionLogStorage initialized with keyPrefix={}, retentionDays={}",
            keyPrefix, retentionDays);
    }

    @Override
    public void save(@NonNull TaskExecutionLog logEntry) {
        String logKey = String.format(LOG_KEY_PATTERN, keyPrefix, logEntry.executionId());
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, logEntry.taskId());

        try {
            // 保存执行日志
            RMap<String, String> logMap = redissonClient.getMap(logKey);
            logMap.put("executionId", logEntry.executionId());
            logMap.put("taskId", logEntry.taskId());
            logMap.put("taskGroup", logEntry.taskGroup());
            logMap.put("nodeId", logEntry.nodeId());
            logMap.put("startTime", logEntry.startTime().toString());
            if (logEntry.endTime() != null) {
                logMap.put("endTime", logEntry.endTime().toString());
            }
            logMap.put("status", logEntry.status().name());
            if (logEntry.errorMessage() != null) {
                logMap.put("errorMessage", logEntry.errorMessage());
            }
            if (logEntry.errorStack() != null) {
                logMap.put("errorStack", logEntry.errorStack());
            }
            logMap.put("retried", String.valueOf(logEntry.retried()));

            // 设置过期时间
            logMap.expire(retentionMs, TimeUnit.MILLISECONDS);

            // 添加到任务索引
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);
            taskIndex.add(logEntry.startTime().toEpochMilli(), logEntry.executionId());
            taskIndex.expire(retentionMs, TimeUnit.MILLISECONDS);

            // 如果是失败，添加到失败索引
            if (logEntry.isFailed()) {
                String failedIndexKey = String.format(TASK_FAILED_INDEX_KEY_PATTERN, keyPrefix, logEntry.taskId());
                RScoredSortedSet<String> failedIndex = redissonClient.getScoredSortedSet(failedIndexKey);
                failedIndex.add(logEntry.startTime().toEpochMilli(), logEntry.executionId());
                failedIndex.expire(retentionMs, TimeUnit.MILLISECONDS);
            }

            log.debug("Saved execution log to Redis: executionId={}", logEntry.executionId());

        } catch (Exception e) {
            log.error("Failed to save execution log: {}", e.getMessage());
        }
    }

    @Override
    public void update(@NonNull TaskExecutionLog logEntry) {
        // Redis 存储使用覆盖式更新
        save(logEntry);
    }

    @Override
    @NonNull
    public Optional<TaskExecutionLog> findByExecutionId(@NonNull String executionId) {
        String logKey = String.format(LOG_KEY_PATTERN, keyPrefix, executionId);

        try {
            RMap<String, String> logMap = redissonClient.getMap(logKey);
            if (logMap.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(parseLog(logMap));

        } catch (Exception e) {
            log.error("Failed to find execution log by executionId: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskId(@NonNull String taskId, int limit) {
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);

            // 获取最近的执行 ID（按时间倒序）
            Collection<String> executionIds = taskIndex.valueRangeReversed(0, limit - 1);

            List<TaskExecutionLog> logs = new ArrayList<>();
            for (String executionId : executionIds) {
                findByExecutionId(executionId).ifPresent(logs::add);
            }

            return logs;

        } catch (Exception e) {
            log.error("Failed to find execution logs by taskId: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskGroup(@NonNull String taskGroup, int limit) {
        // 需要遍历所有任务索引，性能较低
        // 生产环境建议添加分组索引
        try {
            Iterator<String> allKeys = redissonClient.getKeys().getKeysByPattern(keyPrefix + ":task:*").iterator();
            List<TaskExecutionLog> logs = new ArrayList<>();

            while (allKeys.hasNext() && logs.size() < limit) {
                String key = allKeys.next();
                if (key.endsWith(":failed")) {
                    continue;
                }
                RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(key);
                for (String executionId : taskIndex.valueRangeReversed(0, limit - 1)) {
                    findByExecutionId(executionId).ifPresent(logEntry -> {
                        if (logEntry.taskGroup().equals(taskGroup)) {
                            logs.add(logEntry);
                        }
                    });
                    if (logs.size() >= limit) {
                        break;
                    }
                }
            }

            return logs;

        } catch (Exception e) {
            log.error("Failed to find execution logs by taskGroup: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTimeRange(@NonNull String taskId,
                                                   @NonNull Instant from,
                                                   @NonNull Instant to) {
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);

            // 获取时间范围内的执行 ID
            Collection<String> executionIds = taskIndex.valueRange(
                from.toEpochMilli(), true, to.toEpochMilli(), true
            );

            List<TaskExecutionLog> logs = new ArrayList<>();
            for (String executionId : executionIds) {
                findByExecutionId(executionId).ifPresent(logs::add);
            }

            return logs;

        } catch (Exception e) {
            log.error("Failed to find execution logs by time range: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findFailedExecutions(@NonNull String taskId, int limit) {
        String failedIndexKey = String.format(TASK_FAILED_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> failedIndex = redissonClient.getScoredSortedSet(failedIndexKey);

            Collection<String> executionIds = failedIndex.valueRangeReversed(0, limit - 1);

            List<TaskExecutionLog> logs = new ArrayList<>();
            for (String executionId : executionIds) {
                findByExecutionId(executionId).ifPresent(logs::add);
            }

            return logs;

        } catch (Exception e) {
            log.error("Failed to find failed executions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long countByTaskId(@NonNull String taskId) {
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);
            return taskIndex.size();

        } catch (Exception e) {
            log.error("Failed to count by taskId: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long countSuccessByTaskId(@NonNull String taskId) {
        return countByTaskId(taskId) - countFailedByTaskId(taskId);
    }

    @Override
    public long countFailedByTaskId(@NonNull String taskId) {
        String failedIndexKey = String.format(TASK_FAILED_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> failedIndex = redissonClient.getScoredSortedSet(failedIndexKey);
            return failedIndex.size();

        } catch (Exception e) {
            log.error("Failed to count failed by taskId: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public double getAverageExecutionTime(@NonNull String taskId) {
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);

            if (taskIndex.isEmpty()) {
                return 0;
            }

            long totalDuration = 0;
            int count = 0;

            // 采样计算（最多100条）
            int sampleSize = Math.min(100, taskIndex.size());
            Collection<String> executionIds = taskIndex.valueRangeReversed(0, sampleSize - 1);

            for (String executionId : executionIds) {
                Optional<TaskExecutionLog> logOpt = findByExecutionId(executionId);
                if (logOpt.isPresent()) {
                    TaskExecutionLog logEntry = logOpt.get();
                    if (logEntry.endTime() != null) {
                        totalDuration += logEntry.duration().toMillis();
                        count++;
                    }
                }
            }

            return count > 0 ? (double) totalDuration / count : 0;

        } catch (Exception e) {
            log.error("Failed to get average execution time: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long deleteBefore(@NonNull Instant before) {
        long deleted = 0;

        try {
            // 遍历所有任务索引
            Iterator<String> allKeys = redissonClient.getKeys().getKeysByPattern(keyPrefix + ":task:*").iterator();

            while (allKeys.hasNext()) {
                String key = allKeys.next();
                if (key.endsWith(":failed")) {
                    continue;
                }

                RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(key);

                // 获取指定时间之前的执行 ID
                Collection<String> oldIds = taskIndex.valueRange(0, true, before.toEpochMilli(), true);

                for (String executionId : oldIds) {
                    // 删除执行日志
                    String logKey = String.format(LOG_KEY_PATTERN, keyPrefix, executionId);
                    redissonClient.getMap(logKey).delete();
                    taskIndex.remove(executionId);
                    deleted++;
                }
            }

            log.info("Deleted {} execution logs before {}", deleted, before);

        } catch (Exception e) {
            log.error("Failed to delete before {}: {}", before, e.getMessage());
        }

        return deleted;
    }

    @Override
    public void deleteByTaskId(@NonNull String taskId) {
        String taskIndexKey = String.format(TASK_INDEX_KEY_PATTERN, keyPrefix, taskId);
        String failedIndexKey = String.format(TASK_FAILED_INDEX_KEY_PATTERN, keyPrefix, taskId);

        try {
            RScoredSortedSet<String> taskIndex = redissonClient.getScoredSortedSet(taskIndexKey);

            // 删除所有执行日志
            for (String executionId : taskIndex) {
                String logKey = String.format(LOG_KEY_PATTERN, keyPrefix, executionId);
                redissonClient.getMap(logKey).delete();
            }

            // 删除索引
            taskIndex.delete();
            redissonClient.getScoredSortedSet(failedIndexKey).delete();

            log.info("Deleted all execution logs for task: {}", taskId);

        } catch (Exception e) {
            log.error("Failed to delete by taskId {}: {}", taskId, e.getMessage());
        }
    }

    /**
     * 解析执行日志
     */
    private TaskExecutionLog parseLog(RMap<String, String> logMap) {
        return new TaskExecutionLog(
            logMap.get("executionId"),
            logMap.get("taskId"),
            logMap.get("taskGroup"),
            logMap.get("nodeId"),
            Instant.parse(logMap.get("startTime")),
            logMap.get("endTime") != null ? Instant.parse(logMap.get("endTime")) : null,
            TaskExecutionLog.ExecutionStatus.valueOf(logMap.get("status")),
            logMap.get("errorMessage"),
            logMap.get("errorStack"),
            Integer.parseInt(logMap.getOrDefault("retried", "0"))
        );
    }
}