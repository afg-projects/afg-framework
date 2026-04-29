package io.github.afgprojects.framework.core.web.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.model.result.Results;

/**
 * 任务管理 REST API
 *
 * <p>提供任务调度管理的 HTTP 接口
 *
 * <h3>接口列表</h3>
 * <ul>
 *   <li>GET /afg/scheduler/tasks - 获取所有任务列表</li>
 *   <li>GET /afg/scheduler/tasks/{taskId} - 获取任务详情</li>
 *   <li>GET /afg/scheduler/tasks/{taskId}/status - 获取任务状态</li>
 *   <li>POST /afg/scheduler/tasks/{taskId}/pause - 暂停任务</li>
 *   <li>POST /afg/scheduler/tasks/{taskId}/resume - 恢复任务</li>
 *   <li>DELETE /afg/scheduler/tasks/{taskId} - 取消任务</li>
 *   <li>GET /afg/scheduler/tasks/{taskId}/history - 获取执行历史</li>
 *   <li>GET /afg/scheduler/tasks/{taskId}/statistics - 获取执行统计</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>
 * afg:
 *   scheduler:
 *     api:
 *       enabled: true
 *       base-path: /afg/scheduler
 * </pre>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("${afg.scheduler.api.base-path:/afg/scheduler}")
@ConditionalOnProperty(prefix = "afg.scheduler.api", name = "enabled", havingValue = "true")
public class TaskManagementController {

    private final TaskScheduler taskScheduler;
    private final TaskExecutionLogStorage logStorage;
    private final @Nullable DynamicTaskConfigSource configSource;

    /**
     * 创建任务管理控制器
     *
     * @param taskScheduler 任务调度器
     * @param logStorage    日志存储
     * @param configSource  动态配置源（可选）
     */
    public TaskManagementController(
            @NonNull TaskScheduler taskScheduler,
            @NonNull TaskExecutionLogStorage logStorage,
            @Nullable DynamicTaskConfigSource configSource) {
        this.taskScheduler = taskScheduler;
        this.logStorage = logStorage;
        this.configSource = configSource;
    }

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务 ID
     * @return 是否存在
     */
    @GetMapping("/tasks/{taskId}/exists")
    public Result<Boolean> exists(@PathVariable String taskId) {
        return Results.success(taskScheduler.hasTask(taskId));
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    @GetMapping("/tasks/{taskId}/status")
    public Result<TaskStatusInfo> getStatus(@PathVariable String taskId) {
        TaskStatus status = taskScheduler.getTaskStatus(taskId);
        if (status == null) {
            return Results.fail(404, "Task not found: " + taskId);
        }

        TaskStatusInfo info = new TaskStatusInfo(
            taskId,
            status,
            taskScheduler.hasTask(taskId),
            Instant.now()
        );

        return Results.success(info);
    }

    /**
     * 暂停任务
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{taskId}/pause")
    public Result<Void> pause(@PathVariable String taskId) {
        try {
            taskScheduler.pause(taskId);
            return Results.success();
        } catch (Exception e) {
            return Results.fail(500, "Failed to pause task: " + e.getMessage());
        }
    }

    /**
     * 恢复任务
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @PostMapping("/tasks/{taskId}/resume")
    public Result<Void> resume(@PathVariable String taskId) {
        try {
            taskScheduler.resume(taskId);
            return Results.success();
        } catch (Exception e) {
            return Results.fail(500, "Failed to resume task: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @DeleteMapping("/tasks/{taskId}")
    public Result<Boolean> cancel(@PathVariable String taskId) {
        boolean result = taskScheduler.cancel(taskId);
        if (result) {
            return Results.success(true);
        }
        return Results.fail(404, "Task not found: " + taskId);
    }

    /**
     * 获取执行历史
     *
     * @param taskId 任务 ID
     * @param limit  限制数量（默认 20）
     * @return 执行历史列表
     */
    @GetMapping("/tasks/{taskId}/history")
    public Result<List<TaskExecutionLog>> getHistory(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TaskExecutionLog> history = logStorage.findByTaskId(taskId, limit);
        return Results.success(history);
    }

    /**
     * 获取失败执行记录
     *
     * @param taskId 任务 ID
     * @param limit  限制数量（默认 20）
     * @return 失败执行列表
     */
    @GetMapping("/tasks/{taskId}/failures")
    public Result<List<TaskExecutionLog>> getFailures(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TaskExecutionLog> failures = logStorage.findFailedExecutions(taskId, limit);
        return Results.success(failures);
    }

    /**
     * 获取执行统计
     *
     * @param taskId 任务 ID
     * @return 执行统计
     */
    @GetMapping("/tasks/{taskId}/statistics")
    public Result<TaskStatistics> getStatistics(@PathVariable String taskId) {
        TaskStatistics statistics = new TaskStatistics(
            taskId,
            logStorage.countByTaskId(taskId),
            logStorage.countSuccessByTaskId(taskId),
            logStorage.countFailedByTaskId(taskId),
            logStorage.getAverageExecutionTime(taskId)
        );

        return Results.success(statistics);
    }

    /**
     * 获取动态任务配置
     *
     * @param taskId 任务 ID
     * @return 任务定义
     */
    @GetMapping("/tasks/{taskId}/config")
    public Result<TaskDefinition> getConfig(@PathVariable String taskId) {
        if (configSource == null) {
            return Results.fail(503, "Dynamic task config source not available");
        }

        return configSource.load(taskId)
            .map(Results::success)
            .orElseGet(() -> Results.fail(404, "Task config not found: " + taskId));
    }

    /**
     * 更新动态任务配置
     *
     * @param taskId     任务 ID
     * @param definition 任务定义
     * @return 操作结果
     */
    @PutMapping("/tasks/{taskId}/config")
    public Result<Void> updateConfig(
            @PathVariable String taskId,
            @RequestBody TaskDefinition definition) {
        if (configSource == null) {
            return Results.fail(503, "Dynamic task config source not available");
        }

        try {
            configSource.save(definition);
            return Results.success();
        } catch (Exception e) {
            return Results.fail(500, "Failed to update task config: " + e.getMessage());
        }
    }

    /**
     * 删除动态任务配置
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @DeleteMapping("/tasks/{taskId}/config")
    public Result<Void> deleteConfig(@PathVariable String taskId) {
        if (configSource == null) {
            return Results.fail(503, "Dynamic task config source not available");
        }

        try {
            configSource.delete(taskId);
            return Results.success();
        } catch (Exception e) {
            return Results.fail(500, "Failed to delete task config: " + e.getMessage());
        }
    }

    /**
     * 刷新动态配置
     *
     * @return 操作结果
     */
    @PostMapping("/refresh")
    public Result<Void> refresh() {
        if (configSource == null) {
            return Results.fail(503, "Dynamic task config source not available");
        }

        try {
            configSource.refresh();
            return Results.success();
        } catch (Exception e) {
            return Results.fail(500, "Failed to refresh config: " + e.getMessage());
        }
    }

    /**
     * 任务状态信息
     */
    public record TaskStatusInfo(
        String taskId,
        TaskStatus status,
        boolean exists,
        Instant checkedAt
    ) {}

    /**
     * 任务执行统计
     */
    public record TaskStatistics(
        String taskId,
        long totalExecutions,
        long successCount,
        long failureCount,
        double averageExecutionTimeMs
    ) {}
}
