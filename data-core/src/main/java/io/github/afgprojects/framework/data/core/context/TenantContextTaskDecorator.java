package io.github.afgprojects.framework.data.core.context;

import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;

/**
 * 租户上下文任务装饰器
 * <p>
 * 实现 Spring 的 {@link TaskDecorator} 接口，自动将租户上下文传播到异步任务中。
 * <p>
 * 用于 @Async 注解、ThreadPoolTaskExecutor 等场景，确保子线程能够访问父线程的租户信息。
 *
 * <h3>配置示例</h3>
 * <pre>
 * &#064;Configuration
 * public class AsyncConfig {
 *     &#064;Bean
 *     public TaskExecutor taskExecutor(TenantContextHolder tenantContextHolder) {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setTaskDecorator(new TenantContextTaskDecorator(tenantContextHolder));
 *         return executor;
 *     }
 * }
 * </pre>
 *
 * @see TenantContextHolder
 * @see TaskDecorator
 */
public class TenantContextTaskDecorator implements TaskDecorator {

    private final TenantContextHolder tenantContextHolder;

    /**
     * 创建租户上下文任务装饰器
     *
     * @param tenantContextHolder 租户上下文持有者
     */
    public TenantContextTaskDecorator(@NonNull TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * 装饰 Runnable，使其在执行时携带当前线程的租户上下文
     *
     * @param runnable 原始任务
     * @return 装饰后的任务，会自动传播租户上下文
     */
    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        // 在父线程中捕获租户上下文快照
        TenantContextHolder.TenantContextSnapshot snapshot = tenantContextHolder.snapshot();

        // 返回包装后的 Runnable，在子线程中恢复租户上下文
        return () -> tenantContextHolder.runWithSnapshot(snapshot, runnable);
    }
}
