package io.github.afgprojects.framework.core.autoconfigure;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.event.DomainEventPublisher;
import io.github.afgprojects.framework.core.event.EventRetryHandler;
import io.github.afgprojects.framework.core.event.LocalEventPublisher;

/**
 * 事件驱动自动配置类
 *
 * <p>配置本地事件发布器：
 * <ul>
 *   <li>LOCAL - 基于 Spring ApplicationEventPublisher 的本地事件发布</li>
 * </ul>
 *
 * <p>分布式事件发布器由 impl 模块提供：
 * <ul>
 *   <li>KAFKA - 由 impl:afg-kafka 模块提供</li>
 *   <li>RABBITMQ - 由 impl:afg-rabbitmq 模块提供</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   core:
 *     event:
 *       enabled: true
 *       type: LOCAL  # 分布式类型由对应 impl 模块自动配置
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgCoreProperties.class)
@ConditionalOnProperty(prefix = "afg.core.event", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class EventAutoConfiguration {

    /**
     * 配置本地事件发布器
     *
     * <p>当事件类型为 LOCAL 时激活
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "afg.core.event", name = "type", havingValue = "LOCAL", matchIfMissing = true)
    static class LocalEventConfiguration {

        /**
         * 创建异步事件执行器
         */
        @Bean
        @ConditionalOnMissingBean(name = "eventAsyncExecutor")
        public Executor eventAsyncExecutor(AfgCoreProperties properties) {
            int threadPoolSize = properties.getEvent().getLocal().getThreadPoolSize();
            log.info("Creating event async executor with {} threads", threadPoolSize);
            return Executors.newFixedThreadPool(threadPoolSize, r -> {
                Thread thread = new Thread(r, "event-async-publisher");
                thread.setDaemon(true);
                return thread;
            });
        }

        /**
         * 创建本地事件发布器
         */
        @Bean
        @ConditionalOnMissingBean
        public DomainEventPublisher localEventPublisher(
                ApplicationEventPublisher applicationEventPublisher,
                @Qualifier("eventAsyncExecutor") Executor eventAsyncExecutor,
                AfgCoreProperties properties) {
            log.info("Initializing LocalEventPublisher");
            return new LocalEventPublisher(applicationEventPublisher, eventAsyncExecutor);
        }
    }

    /**
     * 创建事件重试处理器
     *
     * <p>提供事件处理失败后的重试逻辑
     */
    @Bean
    @ConditionalOnMissingBean
    public EventRetryHandler eventRetryHandler(
            AfgCoreProperties properties,
            DomainEventPublisher domainEventPublisher) {
        log.info("Initializing EventRetryHandler with maxAttempts={}", properties.getEvent().getRetry().getMaxAttempts());
        return new EventRetryHandler(properties, domainEventPublisher);
    }
}