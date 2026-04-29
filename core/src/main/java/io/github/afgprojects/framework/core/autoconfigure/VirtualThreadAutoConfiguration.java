package io.github.afgprojects.framework.core.autoconfigure;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import io.github.afgprojects.framework.core.web.context.RequestContext;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

/**
 * 虚拟线程自动配置类
 *
 * <p>为 Java 25 虚拟线程提供自动配置支持，与 Spring Boot 虚拟线程配置协同工作。
 *
 * <h3>配置方式</h3>
 * <p>推荐与 Spring Boot 的虚拟线程配置保持一致：
 * <pre>
 * spring:
 *   threads:
 *     virtual:
 *       enabled: true  # Spring Boot 原生配置（控制 Tomcat、@Async 等）
 *
 * afg:
 *   virtual-thread:
 *     enabled: true    # AFG 配置（应与 spring.threads.virtual.enabled 保持一致）
 *     name-prefix: afg-vt-
 * </pre>
 *
 * <h3>Spring Boot 虚拟线程配置控制</h3>
 * <p>{@code spring.threads.virtual.enabled=true} 会自动配置：
 * <ul>
 *   <li>TOMCAT: 使用虚拟线程处理 HTTP 请求</li>
 *   <li>ASYNC: @Async 注解使用虚拟线程</li>
 *   <li>EXECUTOR: 异步任务使用虚拟线程</li>
 * </ul>
 *
 * <h3>本配置提供</h3>
 * <ul>
 *   <li>{@code virtualThreadFactory}: 自定义名称前缀的虚拟线程工厂</li>
 *   <li>{@code afgVirtualThreadExecutor}: 通用虚拟线程执行器，可用于 HttpClient、CompletableFuture 等</li>
 * </ul>
 *
 * <h3>上下文传播</h3>
 * <p>虚拟线程会自动继承创建时的线程上下文，包括：
 * <ul>
 *   <li>{@link RequestContext} - 请求上下文</li>
 *   <li>{@link org.slf4j.MDC} - 日志上下文</li>
 *   <li>{@link org.springframework.web.context.request.RequestContextHolder} - Spring 请求上下文</li>
 * </ul>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>虚拟线程适合 I/O 密集型任务，对于 CPU 密集型任务建议使用平台线程</li>
 *   <li>建议 {@code afg.virtual-thread.enabled} 与 {@code spring.threads.virtual.enabled} 保持一致</li>
 * </ul>
 *
 * @see VirtualThreadProperties
 */
@AutoConfiguration
@ConditionalOnClass(name = "java.lang.VirtualThread")
@EnableConfigurationProperties(VirtualThreadProperties.class)
public class VirtualThreadAutoConfiguration {

    /**
     * 虚拟线程工厂
     * <p>
     * 创建具有指定名称前缀的虚拟线程
     * <p>
     * 仅在 Spring Boot 虚拟线程启用时生效（与 spring.threads.virtual.enabled 保持一致）
     *
     * @param properties 虚拟线程配置属性
     * @return 虚拟线程工厂
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.threads.virtual", name = "enabled", havingValue = "true")
    @ConditionalOnProperty(prefix = "afg.virtual-thread", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ThreadFactory virtualThreadFactory(VirtualThreadProperties properties) {
        final String prefix = properties.getNamePrefix();
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    /**
     * 通用虚拟线程执行器
     * <p>
     * 可用于 HttpClient、CompletableFuture 等场景
     * <p>
     * 仅在 Spring Boot 虚拟线程启用时生效（与 spring.threads.virtual.enabled 保持一致）
     * <p>
     * 注意：@Async 注解由 Spring Boot 自动配置，无需此 Bean
     *
     * @return 虚拟线程执行器
     */
    @Bean("afgVirtualThreadExecutor")
    @ConditionalOnProperty(prefix = "spring.threads.virtual", name = "enabled", havingValue = "true")
    @ConditionalOnProperty(prefix = "afg.virtual-thread", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "afgVirtualThreadExecutor")
    public Executor afgVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 虚拟线程异步任务执行器适配器
     * <p>
     * 包装虚拟线程执行器，提供 AsyncTaskExecutor 接口
     * <p>
     * 仅在需要自定义配置时使用，Spring Boot 默认已提供虚拟线程执行器
     */
    public static class VirtualThreadAsyncTaskExecutor extends TaskExecutorAdapter {

        /**
         * 创建虚拟线程异步任务执行器
         *
         * @param threadFactory 虚拟线程工厂
         */
        public VirtualThreadAsyncTaskExecutor(@NonNull ThreadFactory threadFactory) {
            super(Executors.newVirtualThreadPerTaskExecutor());
        }
    }
}
