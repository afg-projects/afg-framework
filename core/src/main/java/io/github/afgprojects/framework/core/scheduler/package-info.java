/**
 * 任务调度模块
 *
 * <p>提供完整的分布式定时任务调度解决方案
 *
 * <h2>核心组件</h2>
 *
 * <h3>1. 任务调度器</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskScheduler} - 任务调度器接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.DistributedTaskScheduler} - 分布式任务调度器接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.LocalTaskScheduler} - 本地任务调度器实现</li>
 * </ul>
 *
 * <h3>2. 任务定义</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskDefinition} - 任务定义</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskStatus} - 任务状态</li>
 * </ul>
 *
 * <h3>3. 执行监控</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics} - 任务执行监控指标</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog} - 执行日志</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage} - 日志存储接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.InMemoryTaskExecutionLogStorage} - 内存存储实现</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.SchedulerHealthIndicator} - 调度器健康检查</li>
 * </ul>
 *
 * <h3>4. 动态配置</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource} - 动态任务配置源接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.ConfigCenterTaskConfigSource} - 配置中心实现</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.DynamicTaskManager} - 动态任务管理服务</li>
 * </ul>
 *
 * <h3>5. 注解切面</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.annotation.ScheduledTask} - 本地定时任务注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.annotation.DistributedTask} - 分布式定时任务注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.ScheduledTaskAspect} - 本地任务切面</li>
 *   <li>{@link io.github.afgprojects.framework.core.scheduler.DistributedTaskAspect} - 分布式任务切面</li>
 * </ul>
 *
 * <h3>6. 管理接口</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.scheduler.TaskManagementController} - 任务管理 REST API</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 *
 * <h3>声明式定时任务</h3>
 * <pre>{@code
 * @ScheduledTask(id = "my-task", cron = "0 0 5 * * ?")
 * public void myScheduledTask() {
 *     // 任务逻辑
 * }
 *
 * @DistributedTask(id = "distributed-task", cron = "0 0 0 * * ?", lockWaitTime = 5000)
 * public void myDistributedTask() {
 *     // 分布式任务逻辑（自动获取锁）
 * }
 * }</pre>
 *
 * <h3>编程式任务调度</h3>
 * <pre>{@code
 * @Autowired
 * private TaskScheduler taskScheduler;
 *
 * // 固定速率调度
 * taskScheduler.scheduleAtFixedRate("my-task", () -> {
 *     // 任务逻辑
 * }, Duration.ofMinutes(5));
 *
 * // Cron 表达式调度
 * taskScheduler.schedule("my-task", () -> {
 *     // 任务逻辑
 * }, "0 0 5 * * ?");
 *
 * // 一次性任务
 * taskScheduler.scheduleOnce("my-task", () -> {
 *     // 任务逻辑
 * }, Instant.now().plusSeconds(60));
 * }</pre>
 *
 * <h3>动态任务管理</h3>
 * <pre>{@code
 * @Autowired
 * private DynamicTaskManager taskManager;
 *
 * // 注册任务
 * TaskDefinition definition = TaskDefinition.ofCron("my-task", "0 0 5 * * ?");
 * taskManager.registerTask(definition, () -> executeTask());
 *
 * // 取消任务
 * taskManager.cancelTask("my-task");
 *
 * // 暂停/恢复任务
 * taskManager.pauseTask("my-task");
 * taskManager.resumeTask("my-task");
 *
 * // 更新任务配置
 * TaskDefinition newDefinition = TaskDefinition.ofFixedRate("my-task", 60000);
 * taskManager.updateTaskConfig(newDefinition);
 * }</pre>
 *
 * <h3>动态任务配置</h3>
 * <pre>{@code
 * @Autowired
 * private DynamicTaskConfigSource configSource;
 *
 * // 加载配置
 * List<TaskDefinition> tasks = configSource.loadAll();
 *
 * // 根据配置调度任务
 * for (TaskDefinition def : tasks) {
 *     localTaskScheduler.schedule(def, () -> executeTask(def));
 * }
 * }</pre>
 *
 * <h2>配置示例</h2>
 * <pre>
 * afg:
 *   scheduler:
 *     enabled: true
 *     default-timeout: 30m
 *     default-retry-attempts: 3
 *     thread-pool-size: 4
 *     log-storage:
 *       type: memory
 *       max-size: 10000
 *       retention: 7d
 *     metrics:
 *       enabled: true
 *       prefix: afg.scheduler
 *     annotations:
 *       enabled: true
 *     dynamic-task:
 *       enabled: true
 *       source-type: config-center
 *     health:
 *       enabled: true
 *     api:
 *       enabled: true
 *       base-path: /afg/scheduler
 * </pre>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.scheduler;