package io.github.afgprojects.framework.core.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 *   event:
 *     enabled: true
 *     type: LOCAL  # 分布式类型由对应 impl 模块自动配置
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(EventProperties.class)
@ConditionalOnProperty(prefix = "afg.event", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EventAutoConfiguration.class);

    /**
     * 配置本地事件发布器
     *
     * <p>当事件类型为 LOCAL 时激活
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "afg.event", name = "type", havingValue = "LOCAL", matchIfMissing = true)
    static class LocalEventConfiguration {

        /**
         * 创建异步事件执行器
         */
        @Bean
        @ConditionalOnMissingBean(name = "eventAsyncExecutor")
        public Executor eventAsyncExecutor(EventProperties properties) {
            int threadPoolSize = properties.getLocal().getThreadPoolSize();
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
                EventProperties properties) {
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
            EventProperties properties,
            DomainEventPublisher domainEventPublisher) {
        log.info("Initializing EventRetryHandler with maxAttempts={}", properties.getRetry().getMaxAttempts());
        return new EventRetryHandler(properties, domainEventPublisher);
    }
}