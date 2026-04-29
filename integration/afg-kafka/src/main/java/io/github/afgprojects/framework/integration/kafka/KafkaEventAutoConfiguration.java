package io.github.afgprojects.framework.integration.kafka;

import io.github.afgprojects.framework.core.api.event.EventPublisher;
import io.github.afgprojects.framework.integration.kafka.health.KafkaHealthIndicator;
import io.github.afgprojects.framework.integration.kafka.health.KafkaHealthProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 事件发布自动配置
 *
 * <p>自动配置以下功能：
 * <ul>
 *   <li>Kafka 事件发布器</li>
 *   <li>Kafka 健康检查指示器</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   kafka:
 *     event:
 *       enabled: true
 *       topic: afg-events
 *   health:
 *     kafka:
 *       enabled: true
 *       timeout: 5000
 *       topics-to-check:
 *         - afg-events
 *         - afg-notifications
 *       fail-on-missing-topics: false
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnBean(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "afg.kafka.event", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({KafkaEventProperties.class, KafkaHealthProperties.class})
public class KafkaEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public <T> KafkaEventPublisher<T> kafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaEventProperties properties) {
        return new KafkaEventPublisher<>(kafkaTemplate, properties);
    }

    /**
     * Kafka 健康检查指示器
     * 当存在 KafkaAdmin 时自动配置
     *
     * @param kafkaAdmin Kafka 管理客户端
     * @param properties 健康检查配置属性
     * @return Kafka 健康检查指示器实例
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(KafkaAdmin.class)
    @ConditionalOnMissingBean(name = "kafkaHealthIndicator")
    @ConditionalOnProperty(prefix = "afg.health.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KafkaHealthIndicator kafkaHealthIndicator(
            KafkaAdmin kafkaAdmin,
            KafkaHealthProperties properties) {
        return new KafkaHealthIndicator(kafkaAdmin, properties);
    }
}
