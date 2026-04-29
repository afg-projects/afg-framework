package io.github.afgprojects.framework.data.core.autoconfigure;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.context.TenantContextTaskDecorator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * 租户上下文自动配置类
 * <p>
 * 自动配置租户上下文相关的组件，包括：
 * <ul>
 *   <li>{@link TenantContextHolder} - 租户上下文持有者</li>
 *   <li>{@link TenantContextTaskDecorator} - 异步任务租户上下文传播</li>
 * </ul>
 *
 * <h3>启用配置</h3>
 * <pre>
 * afg:
 *   tenant:
 *     context-propagation:
 *       enabled: true  # 默认启用
 * </pre>
 *
 * <h3>TaskDecorator 自动装配</h3>
 * <p>
 * 当启用租户上下文传播时，会自动创建 {@link TenantContextTaskDecorator}。
 * 如果使用 Spring Boot 的 @Async 注解，需要在异步执行器配置中设置此 TaskDecorator：
 *
 * <pre>
 * &#064;Configuration
 * public class AsyncConfig {
 *     &#064;Bean
 *     public TaskExecutor taskExecutor(TaskDecorator tenantContextTaskDecorator) {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setTaskDecorator(tenantContextTaskDecorator);
 *         return executor;
 *     }
 * }
 * </pre>
 *
 * @see TenantContextHolder
 * @see TenantContextTaskDecorator
 */
@AutoConfiguration
@ConditionalOnClass(TaskDecorator.class)
public class TenantContextAutoConfiguration {

    /**
     * 租户上下文持有者
     * <p>
     * 每个应用实例创建一个共享的 TenantContextHolder
     *
     * @return TenantContextHolder 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantContextHolder tenantContextHolder() {
        return new TenantContextHolder();
    }

    /**
     * 租户上下文任务装饰器
     * <p>
     * 用于将租户上下文传播到异步任务中
     *
     * @param tenantContextHolder 租户上下文持有者
     * @return TenantContextTaskDecorator 实例
     */
    @Bean("tenantContextTaskDecorator")
    @ConditionalOnProperty(
            prefix = "afg.tenant.context-propagation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean(name = "tenantContextTaskDecorator")
    public TaskDecorator tenantContextTaskDecorator(TenantContextHolder tenantContextHolder) {
        return new TenantContextTaskDecorator(tenantContextHolder);
    }
}
