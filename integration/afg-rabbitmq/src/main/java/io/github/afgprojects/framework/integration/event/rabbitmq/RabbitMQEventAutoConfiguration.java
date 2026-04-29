package io.github.afgprojects.framework.integration.event.rabbitmq;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import io.github.afgprojects.framework.core.api.event.EventPublisher;
import io.github.afgprojects.framework.integration.event.rabbitmq.health.RabbitMQHealthIndicator;
import io.github.afgprojects.framework.integration.event.rabbitmq.health.RabbitMQHealthProperties;

/**
 * RabbitMQ 事件发布自动配置
 *
 * <p>当满足以下条件时自动配置 RabbitMQ 事件发布器：
 * <ul>
 *   <li>classpath 中存在 {@link RabbitTemplate} 类</li>
 *   <li>配置属性 afg.rabbitmq.event.enabled 为 true（默认）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   rabbitmq:
 *     event:
 *       enabled: true
 *       exchange: afg-events
 *       default-routing-key: event.default
 *   health:
 *     rabbitmq:
 *       enabled: true
 *       queues-to-check:
 *         - afg-events-queue
 *       fail-on-missing-queues: false
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnBean(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "afg.rabbitmq.event", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({RabbitMQEventProperties.class, RabbitMQHealthProperties.class})
public class RabbitMQEventAutoConfiguration {

    /**
     * 创建 RabbitMQ 事件发布器
     *
     * @param rabbitTemplate RabbitTemplate 实例
     * @param properties     配置属性
     * @param <T>            事件负载类型
     * @return RabbitMQ 事件发布器实例
     */
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public <T> RabbitMQEventPublisher<T> rabbitMQEventPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMQEventProperties properties) {
        return new RabbitMQEventPublisher<>(rabbitTemplate, properties);
    }

    /**
     * RabbitMQ 健康检查指示器
     * 当存在 ConnectionFactory 时自动配置
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param properties        健康检查配置属性
     * @return RabbitMQ 健康检查指示器实例
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(ConnectionFactory.class)
    @ConditionalOnMissingBean(name = "rabbitMQHealthIndicator")
    @ConditionalOnProperty(prefix = "afg.health.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RabbitMQHealthIndicator rabbitMQHealthIndicator(
            ConnectionFactory connectionFactory,
            RabbitMQHealthProperties properties) {
        return new RabbitMQHealthIndicator(connectionFactory, properties);
    }
}