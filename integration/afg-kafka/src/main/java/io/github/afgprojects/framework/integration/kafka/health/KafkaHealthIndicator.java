package io.github.afgprojects.framework.integration.kafka.health;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.common.Node;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Kafka 健康检查指示器
 * 检查 Kafka 集群连接状态和主题可用性
 *
 * <p>检查内容：
 * <ul>
 *   <li>Kafka 集群是否可达</li>
 *   <li>集群控制器状态</li>
 *   <li>节点数量</li>
 *   <li>配置的主题是否存在</li>
 *   <li>响应时间</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    private final KafkaAdmin kafkaAdmin;
    private final KafkaHealthProperties properties;

    /**
     * 构造函数
     *
     * @param kafkaAdmin Kafka 管理客户端
     * @param properties 健康检查配置
     */
    public KafkaHealthIndicator(@NonNull KafkaAdmin kafkaAdmin, @NonNull KafkaHealthProperties properties) {
        this.kafkaAdmin = kafkaAdmin;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        long startTime = System.currentTimeMillis();

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 检查集群状态
            DescribeClusterResult clusterResult = adminClient.describeCluster();

            // 获取集群 ID
            String clusterId = clusterResult.clusterId().get(properties.getTimeout(), TimeUnit.MILLISECONDS);

            // 获取节点信息
            Collection<Node> nodes = clusterResult.nodes().get(properties.getTimeout(), TimeUnit.MILLISECONDS);
            Node controller = clusterResult.controller().get(properties.getTimeout(), TimeUnit.MILLISECONDS);

            long duration = System.currentTimeMillis() - startTime;

            builder.withDetail("kafka", "UP")
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodes.size())
                    .withDetail("controllerId", controller != null ? controller.id() : "unknown")
                    .withDetail("responseTime", duration + "ms");

            // 检查配置的主题是否存在
            if (!properties.getTopicsToCheck().isEmpty()) {
                checkTopics(adminClient, builder);
            }

            // 检查响应时间阈值
            evaluateResponseTime(builder, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Kafka 连接检查失败: {}", e.getMessage(), e);
            builder.status(Status.DOWN)
                    .withDetail("kafka", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTime", duration + "ms");
        }

        return builder.build();
    }

    /**
     * 检查主题是否存在
     */
    private void checkTopics(AdminClient adminClient, Health.Builder builder) {
        try {
            DescribeTopicsResult topicsResult = adminClient.describeTopics(properties.getTopicsToCheck());
            Set<String> existingTopics = topicsResult.topicNameValues().keySet();

            List<String> missingTopics = properties.getTopicsToCheck().stream()
                    .filter(topic -> !existingTopics.contains(topic))
                    .collect(Collectors.toList());

            if (missingTopics.isEmpty()) {
                builder.withDetail("topicsStatus", "ALL_PRESENT")
                        .withDetail("checkedTopics", properties.getTopicsToCheck());
            } else {
                builder.withDetail("topicsStatus", "MISSING_TOPICS")
                        .withDetail("missingTopics", missingTopics);

                if (properties.isFailOnMissingTopics()) {
                    builder.status(Status.DOWN);
                }
            }
        } catch (Exception e) {
            log.warn("检查 Kafka 主题失败: {}", e.getMessage());
            builder.withDetail("topicsStatus", "CHECK_FAILED")
                    .withDetail("topicsError", e.getMessage());
        }
    }

    /**
     * 评估响应时间并设置状态
     */
    private void evaluateResponseTime(Health.Builder builder, long duration) {
        if (duration >= properties.getResponseTimeCriticalThreshold()) {
            builder.status(new Status("WARNING", "Kafka response time is slow"));
            builder.withDetail("responseTimeStatus", "SLOW");
        } else if (duration >= properties.getResponseTimeWarningThreshold()) {
            builder.withDetail("responseTimeStatus", "MODERATE");
        } else {
            builder.withDetail("responseTimeStatus", "FAST");
        }
    }
}
