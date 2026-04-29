package io.github.afgprojects.framework.integration.kafka.health;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Kafka 健康检查配置属性
 * 配置 Kafka 连接健康检查参数
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "afg.health.kafka")
@SuppressWarnings({"PMD.UncommentedEmptyConstructor", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public class KafkaHealthProperties {

    /**
     * 是否启用 Kafka 健康检查
     */
    private boolean enabled = true;

    /**
     * 检查超时时间（毫秒）
     */
    private long timeout = TimeUnit.SECONDS.toMillis(5);

    /**
     * 需要检查的主题列表
     * 如果主题不存在，将报告警告或失败
     */
    private Set<String> topicsToCheck = Set.of();

    /**
     * 主题不存在时是否失败
     * true：主题不存在时健康检查失败
     * false：主题不存在时仅报告警告
     */
    private boolean failOnMissingTopics = false;

    /**
     * 响应时间警告阈值（毫秒）
     */
    private long responseTimeWarningThreshold = 1000;

    /**
     * 响应时间严重阈值（毫秒）
     */
    private long responseTimeCriticalThreshold = 3000;
}
