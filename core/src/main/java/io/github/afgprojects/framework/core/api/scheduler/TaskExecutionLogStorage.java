package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 任务执行日志存储接口
 *
 * <p>提供任务执行日志的持久化存储，支持多种实现（内存、Redis、数据库等）
 */
public interface TaskExecutionLogStorage {

    /**
     * 保存执行日志
     *
     * @param log 执行日志
     */
    void save(@NonNull TaskExecutionLog log);

    /**
     * 更新执行日志
     *
     * @param log 执行日志
     */
    void update(@NonNull TaskExecutionLog log);

    /**
     * 根据执行 ID 查询
     *
     * @param executionId 执行 ID
     * @return 执行日志
     */
    @NonNull Optional<TaskExecutionLog> findByExecutionId(@NonNull String executionId);

    /**
     * 查询任务的执行历史
     *
     * @param taskId 任务 ID
     * @param limit  限制数量
     * @return 执行日志列表
     */
    @NonNull List<TaskExecutionLog> findByTaskId(@NonNull String taskId, int limit);

    /**
     * 查询任务分组的执行历史
     *
     * @param taskGroup 任务分组
     * @param limit     限制数量
     * @return 执行日志列表
     */
    @NonNull List<TaskExecutionLog> findByTaskGroup(@NonNull String taskGroup, int limit);

    /**
     * 查询指定时间范围内的执行历史
     *
     * @param taskId  任务 ID
     * @param from    开始时间
     * @param to      结束时间
     * @return 执行日志列表
     */
    @NonNull List<TaskExecutionLog> findByTimeRange(@NonNull String taskId,
                                                     @NonNull Instant from,
                                                     @NonNull Instant to);

    /**
     * 查询失败的任务执行
     *
     * @param taskId 任务 ID
     * @param limit  限制数量
     * @return 执行日志列表
     */
    @NonNull List<TaskExecutionLog> findFailedExecutions(@NonNull String taskId, int limit);

    /**
     * 统计任务执行次数
     *
     * @param taskId 任务 ID
     * @return 执行次数
     */
    long countByTaskId(@NonNull String taskId);

    /**
     * 统计任务成功次数
     *
     * @param taskId 任务 ID
     * @return 成功次数
     */
    long countSuccessByTaskId(@NonNull String taskId);

    /**
     * 统计任务失败次数
     *
     * @param taskId 任务 ID
     * @return 失败次数
     */
    long countFailedByTaskId(@NonNull String taskId);

    /**
     * 获取平均执行时间
     *
     * @param taskId 任务 ID
     * @return 平均执行时间（毫秒），无记录返回 0
     */
    double getAverageExecutionTime(@NonNull String taskId);

    /**
     * 删除指定时间之前的执行日志
     *
     * @param before 时间点
     * @return 删除的记录数
     */
    long deleteBefore(@NonNull Instant before);

    /**
     * 清除任务的所有执行日志
     *
     * @param taskId 任务 ID
     */
    void deleteByTaskId(@NonNull String taskId);
}
