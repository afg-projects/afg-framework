package io.github.afgprojects.framework.core.autoconfigure;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.context.CompositeContextTaskDecorator;
import io.github.afgprojects.framework.core.context.ContextSnapshotProvider;
import io.github.afgprojects.framework.core.context.ThreadLocalContextPropagator;
import io.github.afgprojects.framework.core.invocation.InvocationContextTaskDecorator;

/**
 * 上下文传播自动配置。
 * <p>
 * 创建 {@link ThreadLocalContextPropagator} bean 并自动注册所有 {@link ContextSnapshotProvider} beans，
 * 实现统一管理 ThreadLocal 上下文的异步传播。
 *
 * <h3>传播的上下文</h3>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.security.datascope.DataScopeContext} — 数据权限上下文</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.context.RequestContext} — 请求上下文</li>
 *   <li>{@link io.github.afgprojects.framework.core.trace.BaggageContext} — Baggage 上下文</li>
 * </ul>
 *
 * @see ThreadLocalContextPropagator
 * @see CompositeContextTaskDecorator
 * @see ContextSnapshotProvider
 */
@Slf4j
@AutoConfiguration(after = AfgAutoConfiguration.class)
public class ContextAutoConfiguration {

    /**
     * 创建 ThreadLocal 上下文传播器。
     * <p>
     * 自动注册所有 {@link ContextSnapshotProvider} beans。
     *
     * @param providers 所有已注册的 ContextSnapshotProvider beans
     * @return 已注册提供者的传播器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadLocalContextPropagator threadLocalContextPropagator(
            @Autowired(required = false) List<ContextSnapshotProvider> providers) {
        ThreadLocalContextPropagator propagator = new ThreadLocalContextPropagator();
        if (providers != null) {
            for (ContextSnapshotProvider provider : providers) {
                propagator.register(provider);
                log.debug("Registered context snapshot provider: {}", provider.getClass().getSimpleName());
            }
        }
        log.info("ThreadLocalContextPropagator registered with {} providers", propagator.getProviderCount());
        return propagator;
    }

    /**
     * 创建组合上下文 TaskDecorator。
     *
     * @param propagator 传播器
     * @return 组合装饰器实例
     */
    @Bean
    @ConditionalOnMissingBean(CompositeContextTaskDecorator.class)
    public CompositeContextTaskDecorator compositeContextTaskDecorator(ThreadLocalContextPropagator propagator) {
        return new CompositeContextTaskDecorator(propagator);
    }

    /**
     * 创建 InvocationContextTaskDecorator（向后兼容）。
     * <p>
     * 委托给 {@link CompositeContextTaskDecorator} 实现上下文传播。
     *
     * @param propagator 传播器
     * @return 装饰器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public InvocationContextTaskDecorator invocationContextTaskDecorator(ThreadLocalContextPropagator propagator) {
        return new InvocationContextTaskDecorator(propagator);
    }

    /**
     * 创建数据权限上下文快照提供者。
     *
     * @return 数据权限上下文快照提供者
     */
    @Bean
    @ConditionalOnMissingBean(name = "dataScopeContextSnapshotProvider")
    public io.github.afgprojects.framework.core.security.datascope.DataScopeContextSnapshotProvider dataScopeContextSnapshotProvider() {
        return new io.github.afgprojects.framework.core.security.datascope.DataScopeContextSnapshotProvider();
    }

    /**
     * 创建请求上下文快照提供者。
     *
     * @return 请求上下文快照提供者
     */
    @Bean
    @ConditionalOnMissingBean(name = "requestContextSnapshotProvider")
    public io.github.afgprojects.framework.core.web.context.RequestContextSnapshotProvider requestContextSnapshotProvider() {
        return new io.github.afgprojects.framework.core.web.context.RequestContextSnapshotProvider();
    }

    /**
     * 创建 Baggage 上下文快照提供者。
     *
     * @return Baggage 上下文快照提供者
     */
    @Bean
    @ConditionalOnMissingBean(name = "baggageContextSnapshotProvider")
    public io.github.afgprojects.framework.core.trace.BaggageContextSnapshotProvider baggageContextSnapshotProvider() {
        return new io.github.afgprojects.framework.core.trace.BaggageContextSnapshotProvider();
    }
}
