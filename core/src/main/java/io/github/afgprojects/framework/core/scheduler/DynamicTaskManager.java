package io.github.afgprojects.framework.core.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.exception.SchedulerException;

/**
 * 动态任务管理服务
 *
 * <p>提供动态任务的注册、取消、更新和监控功能
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>动态注册和取消任务</li>
 *   <li>任务配置变更自动响应</li>
 *   <li>任务执行器映射管理</li>
 *   <li>任务状态监控</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Autowired
 * private DynamicTaskManager taskManager;
 *
 * // 注册任务
 * taskManager.registerTask(definition, () -> executeTask());
 *
 * // 取消任务
 * taskManager.cancelTask("my-task");
 *
 * // 更新任务配置
 * taskManager.updateTaskConfig(newDefinition);
 *
 * // 获取所有任务状态
 * Map<String, TaskStatus> statuses = taskManager.getAllTaskStatuses();
 * }</pre>
 *
 * @since 1.0.0
 */
@Service
public class DynamicTaskManager {

    private static final Logger log = LoggerFactory.getLogger(DynamicTaskManager.class);

    private final TaskScheduler taskScheduler;
    private final @Nullable DynamicTaskConfigSource configSource;

    private final ConcurrentMap<String, TaskRegistration> taskRegistrations = new ConcurrentHashMap<>();

    /**
     * 创建动态任务管理服务
     *
     * @param taskScheduler 任务调度器
     * @param configSource  动态配置源（可选）
     */
    public DynamicTaskManager(@NonNull TaskScheduler taskScheduler,
                               @Nullable DynamicTaskConfigSource configSource) {
        this.taskScheduler = taskScheduler;
        this.configSource = configSource;

        // 注册配置变更监听器
        if (configSource != null) {
            configSource.addChangeListener(this::handleConfigChange);
        }

        log.info("DynamicTaskManager initialized with configSource: {}",
            configSource != null ? configSource.getName() : "none");
    }

    /**
     * 注册任务
     *
     * @param definition 任务定义
     * @param executor   任务执行器
     * @return 调度句柄
     */
    public TaskScheduler.ScheduleHandle registerTask(@NonNull TaskDefinition definition,
                                                      @NonNull Runnable executor) {
        String taskId = definition.taskId();

        // 检查是否已存在
        if (taskRegistrations.containsKey(taskId)) {
            throw new SchedulerException(SchedulerException.JOB_ALREADY_EXISTS,
                "Task already registered: " + taskId);
        }

        // 调度任务
        TaskScheduler.ScheduleHandle handle;
        if (definition.isCronTask()) {
            handle = taskScheduler.schedule(taskId, executor, definition.cron());
        } else if (definition.isFixedRateTask()) {
            handle = taskScheduler.scheduleAtFixedRate(taskId, executor,
                java.time.Duration.ofMillis(definition.fixedRate()));
        } else if (definition.isFixedDelayTask()) {
            handle = taskScheduler.scheduleWithFixedDelay(taskId, executor,
                java.time.Duration.ofMillis(definition.fixedDelay()));
        } else {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Invalid task definition: " + taskId);
        }

        // 保存注册信息
        TaskRegistration registration = new TaskRegistration(definition, executor, handle);
        taskRegistrations.put(taskId, registration);

        log.info("Task registered: taskId={}, type={}",
            taskId, definition.isCronTask() ? "cron" : "fixed");

        return handle;
    }

    /**
     * 取消任务
     *
     * @param taskId 任务 ID
     * @return 是否成功取消
     */
    public boolean cancelTask(@NonNull String taskId) {
        TaskRegistration registration = taskRegistrations.remove(taskId);
        if (registration == null) {
            log.warn("Task not found for cancellation: {}", taskId);
            return false;
        }

        boolean cancelled = taskScheduler.cancel(taskId);
        if (cancelled) {
            log.info("Task cancelled: taskId={}", taskId);
        } else {
            log.warn("Failed to cancel task: taskId={}", taskId);
            // 恢复注册
            taskRegistrations.put(taskId, registration);
        }

        return cancelled;
    }

    /**
     * 暂停任务
     *
     * @param taskId 任务 ID
     */
    public void pauseTask(@NonNull String taskId) {
        TaskRegistration registration = taskRegistrations.get(taskId);
        if (registration == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND,
                "Task not found: " + taskId);
        }

        taskScheduler.pause(taskId);
        log.info("Task paused: taskId={}", taskId);
    }

    /**
     * 恢复任务
     *
     * @param taskId 任务 ID
     */
    public void resumeTask(@NonNull String taskId) {
        TaskRegistration registration = taskRegistrations.get(taskId);
        if (registration == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND,
                "Task not found: " + taskId);
        }

        // 恢复需要重新调度
        TaskDefinition definition = registration.definition;
        Runnable executor = registration.executor;

        // 先取消旧的调度
        taskScheduler.cancel(taskId);

        // 重新调度
        TaskScheduler.ScheduleHandle handle;
        if (definition.isCronTask()) {
            handle = taskScheduler.schedule(taskId, executor, definition.cron());
        } else if (definition.isFixedRateTask()) {
            handle = taskScheduler.scheduleAtFixedRate(taskId, executor,
                java.time.Duration.ofMillis(definition.fixedRate()));
        } else if (definition.isFixedDelayTask()) {
            handle = taskScheduler.scheduleWithFixedDelay(taskId, executor,
                java.time.Duration.ofMillis(definition.fixedDelay()));
        } else {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Invalid task definition: " + taskId);
        }

        // 更新注册
        taskRegistrations.put(taskId, new TaskRegistration(definition, executor, handle));
        log.info("Task resumed: taskId={}", taskId);
    }

    /**
     * 更新任务配置
     *
     * @param newDefinition 新的任务定义
     */
    public void updateTaskConfig(@NonNull TaskDefinition newDefinition) {
        String taskId = newDefinition.taskId();
        TaskRegistration oldRegistration = taskRegistrations.get(taskId);

        if (oldRegistration == null) {
            log.warn("Task not found for update: {}, will register as new", taskId);
            // 如果有执行器，需要外部重新注册
            return;
        }

        // 取消旧的调度
        taskScheduler.cancel(taskId);

        // 重新调度
        Runnable executor = oldRegistration.executor;
        TaskScheduler.ScheduleHandle handle;
        if (newDefinition.isCronTask()) {
            handle = taskScheduler.schedule(taskId, executor, newDefinition.cron());
        } else if (newDefinition.isFixedRateTask()) {
            handle = taskScheduler.scheduleAtFixedRate(taskId, executor,
                java.time.Duration.ofMillis(newDefinition.fixedRate()));
        } else if (newDefinition.isFixedDelayTask()) {
            handle = taskScheduler.scheduleWithFixedDelay(taskId, executor,
                java.time.Duration.ofMillis(newDefinition.fixedDelay()));
        } else {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Invalid task definition: " + taskId);
        }

        // 更新注册
        taskRegistrations.put(taskId, new TaskRegistration(newDefinition, executor, handle));

        // 保存配置到配置源
        if (configSource != null) {
            configSource.save(newDefinition);
        }

        log.info("Task config updated: taskId={}", taskId);
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    public @Nullable TaskStatus getTaskStatus(@NonNull String taskId) {
        return taskScheduler.getTaskStatus(taskId);
    }

    /**
     * 获取所有任务状态
     *
     * @return 任务状态映射
     */
    @NonNull
    public Map<String, TaskStatus> getAllTaskStatuses() {
        Map<String, TaskStatus> statuses = new ConcurrentHashMap<>();
        for (String taskId : taskRegistrations.keySet()) {
            TaskStatus status = taskScheduler.getTaskStatus(taskId);
            if (status != null) {
                statuses.put(taskId, status);
            }
        }
        return statuses;
    }

    /**
     * 获取任务定义
     *
     * @param taskId 任务 ID
     * @return 任务定义
     */
    @Nullable
    public TaskDefinition getTaskDefinition(@NonNull String taskId) {
        TaskRegistration registration = taskRegistrations.get(taskId);
        return registration != null ? registration.definition : null;
    }

    /**
     * 获取所有任务定义
     *
     * @return 任务定义列表
     */
    @NonNull
    public List<TaskDefinition> getAllTaskDefinitions() {
        return taskRegistrations.values().stream()
            .map(r -> r.definition)
            .toList();
    }

    /**
     * 检查任务是否存在
     *
     * @param taskId 任务 ID
     * @return 是否存在
     */
    public boolean hasTask(@NonNull String taskId) {
        return taskRegistrations.containsKey(taskId);
    }

    /**
     * 从配置源加载所有任务
     *
     * @param executorFactory 执行器工厂
     */
    public void loadTasksFromConfig(@NonNull TaskExecutorFactory executorFactory) {
        if (configSource == null) {
            log.warn("No config source available for loading tasks");
            return;
        }

        List<TaskDefinition> definitions = configSource.loadAll();
        for (TaskDefinition definition : definitions) {
            if (!definition.enabled()) {
                log.debug("Task {} is disabled, skipping", definition.taskId());
                continue;
            }

            try {
                Runnable executor = executorFactory.createExecutor(definition);
                if (executor != null) {
                    registerTask(definition, executor);
                } else {
                    log.warn("No executor created for task: {}", definition.taskId());
                }
            } catch (Exception e) {
                log.error("Failed to register task {}: {}", definition.taskId(), e.getMessage());
            }
        }

        log.info("Loaded {} tasks from config source", definitions.size());
    }

    /**
     * 处理配置变更
     */
    private void handleConfigChange(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent event,
                                     @Nullable TaskDefinition definition) {
        if (definition == null) {
            log.debug("Config change event with null definition: {}", event);
            return;
        }

        String taskId = definition.taskId();
        log.info("Handling config change for task {}: {}", taskId, event);

        switch (event) {
            case CREATED -> {
                // 新任务创建，需要外部提供执行器
                log.info("New task config created: {}, waiting for executor registration", taskId);
            }
            case UPDATED -> {
                // 任务更新
                try {
                    updateTaskConfig(definition);
                } catch (Exception e) {
                    log.error("Failed to update task {}: {}", taskId, e.getMessage());
                }
            }
            case DELETED -> {
                // 任务删除
                cancelTask(taskId);
            }
            case REFRESHED -> {
                // 配置刷新，重新加载所有任务
                log.info("Config refreshed, current registered tasks: {}", taskRegistrations.size());
            }
        }
    }

    /**
     * 任务注册信息
     */
    private record TaskRegistration(
        TaskDefinition definition,
        Runnable executor,
        TaskScheduler.ScheduleHandle handle
    ) {}

    /**
     * 任务执行器工厂接口
     */
    @FunctionalInterface
    public interface TaskExecutorFactory {
        /**
         * 根据任务定义创建执行器
         *
         * @param definition 任务定义
         * @return 执行器，返回 null 表示无法创建
         */
        @Nullable Runnable createExecutor(@NonNull TaskDefinition definition);
    }
}