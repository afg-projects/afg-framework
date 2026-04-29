package io.github.afgprojects.framework.integration.event.rabbitmq.health;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * RabbitMQ 健康检查配置属性
 * 配置 RabbitMQ 连接健康检查参数
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.health.rabbitmq")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class RabbitMQHealthProperties {

    /**
     * 是否启用 RabbitMQ 健康检查
     */
    private boolean enabled = true;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout = 3000;

    /**
     * 需要检查的队列列表
     * 如果队列不存在，将报告警告或失败
     */
    private Set<String> queuesToCheck = Set.of();

    /**
     * 需要检查的交换器列表
     */
    private Set<String> exchangesToCheck = Set.of();

    /**
     * 队列不存在时是否失败
     * true：队列不存在时健康检查失败
     * false：队列不存在时仅报告警告
     */
    private boolean failOnMissingQueues = false;

    /**
     * 响应时间警告阈值（毫秒）
     */
    private long responseTimeWarningThreshold = 1000;

    /**
     * 响应时间严重阈值（毫秒）
     */
    private long responseTimeCriticalThreshold = 3000;
}
