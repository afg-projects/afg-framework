package io.github.afgprojects.framework.core.autoconfigure;

import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.scheduler.DelayQueue;
import io.github.afgprojects.framework.core.api.scheduler.DistributedTaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.InMemoryTaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.NoOpDelayQueue;
import io.github.afgprojects.framework.core.api.scheduler.NoOpDistributedTaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.scheduler.DistributedTaskAspect;
import io.github.afgprojects.framework.core.scheduler.DynamicTaskManager;
import io.github.afgprojects.framework.core.scheduler.LocalTaskScheduler;
import io.github.afgprojects.framework.core.scheduler.ScheduledTaskAspect;
import io.github.afgprojects.framework.core.scheduler.SchedulerHealthIndicator;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 任务调度器自动配置
 *
 * <p>自动配置条件:
 * <ul>
 *   <li>afg.scheduler.enabled=true (默认为 true)</li>
 * </ul>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动配置本地任务调度器</li>
 *   <li>自动配置任务执行监控</li>
 *   <li>自动配置执行日志存储</li>
 *   <li>自动配置 @ScheduledTask 切面</li>
 *   <li>自动配置 @DistributedTask 切面（需要 DistributedLock）</li>
 *   <li>自动配置健康检查</li>
 *   <li>自动配置动态任务管理器</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   scheduler:
 *     enabled: true
 *     default-timeout: 30m
 *     thread-pool-size: 4
 *     log-storage:
 *       type: memory
 *       max-size: 10000
 *     metrics:
 *       enabled: true
 * </pre>
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class SchedulerAutoConfiguration {

    /**
     * NoOp 分布式任务调度器降级实现
     * <p>
     * 当没有 Redis 等调度后端时，提供本地降级。
     * 所有调度操作返回空句柄，查询返回 null/false。
     *
     * @return NoOp 分布式任务调度器实例
     */
    @Bean
    @ConditionalOnMissingBean(DistributedTaskScheduler.class)
    public DistributedTaskScheduler noOpDistributedTaskScheduler() {
        return new NoOpDistributedTaskScheduler();
    }

    /**
     * NoOp 延迟队列降级实现
     * <p>
     * 当没有 Redis 等延迟队列后端时，提供本地降级。
     * 所有入队操作返回 taskId（但不会执行），查询返回 0。
     *
     * @return NoOp 延迟队列实例
     */
    @Bean
    @ConditionalOnMissingBean(DelayQueue.class)
    public DelayQueue<Object> noOpDelayQueue() {
        return new NoOpDelayQueue<>();
    }

    /**
     * 配置任务执行日志存储（内存模式）
     *
     * @param properties 调度器配置
     * @return 日志存储实例
     */
    @Bean
    @ConditionalOnMissingBean(TaskExecutionLogStorage.class)
    @ConditionalOnProperty(prefix = "afg.core.scheduler.log-storage", name = "type", havingValue = "memory", matchIfMissing = true)
    public TaskExecutionLogStorage inMemoryTaskExecutionLogStorage(AfgCoreProperties properties) {
        return new InMemoryTaskExecutionLogStorage(properties.getScheduler().getLogStorage().getMaxSize());
    }

    /**
     * 配置任务执行监控
     *
     * @param meterRegistry Micrometer 注册表
     * @param properties    调度器配置
     * @return 执行监控实例
     */
    @Bean
    @ConditionalOnMissingBean(TaskExecutionMetrics.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "afg.core.scheduler.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TaskExecutionMetrics taskExecutionMetrics(MeterRegistry meterRegistry, AfgCoreProperties properties) {
        return new TaskExecutionMetrics(meterRegistry, properties.getScheduler().getMetrics());
    }

    /**
     * 配置本地任务调度器
     *
     * @param properties    调度器配置
     * @param metrics       执行监控
     * @param logStorage    日志存储
     * @return 本地任务调度器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalTaskScheduler localTaskScheduler(
            AfgCoreProperties properties,
            TaskExecutionMetrics metrics,
            TaskExecutionLogStorage logStorage) {
        return new LocalTaskScheduler(properties.getScheduler(), metrics, logStorage);
    }

    /**
     * 配置 @ScheduledTask 切面
     *
     * @param metrics    执行监控
     * @param logStorage 日志存储
     * @param properties 调度器配置
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean(ScheduledTaskAspect.class)
    @ConditionalOnProperty(
            prefix = "afg.core.scheduler.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public ScheduledTaskAspect scheduledTaskAspect(
            TaskExecutionMetrics metrics,
            TaskExecutionLogStorage logStorage,
            AfgCoreProperties properties) {
        return new ScheduledTaskAspect(metrics, logStorage, properties.getScheduler());
    }

    /**
     * 配置 @DistributedTask 切面
     *
     * <p>需要 DistributedLock Bean 支持
     *
     * @param distributedLock 分布式锁
     * @param metrics         执行监控
     * @param logStorage      日志存储
     * @param properties      调度器配置
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean(DistributedTaskAspect.class)
    @ConditionalOnBean(DistributedLock.class)
    @ConditionalOnProperty(
            prefix = "afg.core.scheduler.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public DistributedTaskAspect distributedTaskAspect(
            DistributedLock distributedLock,
            TaskExecutionMetrics metrics,
            TaskExecutionLogStorage logStorage,
            AfgCoreProperties properties) {
        return new DistributedTaskAspect(distributedLock, metrics, logStorage, properties.getScheduler());
    }

    /**
     * 配置调度器健康检查
     *
     * <p>需要 Spring Boot Actuator 依赖
     *
     * @param taskScheduler 任务调度器
     * @param logStorage    日志存储
     * @return 健康检查实例
     */
    @Bean
    @ConditionalOnMissingBean(SchedulerHealthIndicator.class)
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnProperty(
            prefix = "afg.core.scheduler.health",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public SchedulerHealthIndicator schedulerHealthIndicator(
            TaskScheduler taskScheduler,
            TaskExecutionLogStorage logStorage) {
        return new SchedulerHealthIndicator(taskScheduler, logStorage);
    }

    /**
     * 配置动态任务管理器
     *
     * @param taskScheduler 任务调度器
     * @return 动态任务管理器实例
     */
    @Bean
    @ConditionalOnMissingBean(DynamicTaskManager.class)
    @ConditionalOnProperty(
            prefix = "afg.core.scheduler.dynamic-task",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = false)
    public DynamicTaskManager dynamicTaskManager(
            TaskScheduler taskScheduler) {
        return new DynamicTaskManager(taskScheduler, null);
    }
}
