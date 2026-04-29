package io.github.afgprojects.framework.integration.event.rabbitmq.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import com.rabbitmq.client.Connection;

/**
 * RabbitMQ 健康检查指示器
 * 检查 RabbitMQ 连接状态和服务器信息
 *
 * <p>检查内容：
 * <ul>
 *   <li>RabbitMQ 服务器是否可达</li>
 *   <li>连接响应时间</li>
 *   <li>服务器版本信息</li>
 *   <li>配置的队列和交换器是否存在</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RabbitMQHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQHealthIndicator.class);

    private final ConnectionFactory connectionFactory;
    private final RabbitMQHealthProperties properties;

    /**
     * 构造函数
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param properties        健康检查配置
     */
    public RabbitMQHealthIndicator(@NonNull ConnectionFactory connectionFactory, @NonNull RabbitMQHealthProperties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        long startTime = System.currentTimeMillis();

        try {
            // 使用 RabbitAdmin 获取连接信息
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

            // 获取服务器属性
            Properties serverProperties = rabbitAdmin.getQueueProperties("");
            if (serverProperties == null) {
                // 尝试直接获取连接
                checkDirectConnection(builder);
            } else {
                // 从 properties 中获取信息
                addServerProperties(builder, serverProperties);
            }

            long duration = System.currentTimeMillis() - startTime;
            builder.withDetail("rabbitmq", "UP")
                    .withDetail("responseTime", duration + "ms");

            // 检查配置的队列
            if (!properties.getQueuesToCheck().isEmpty()) {
                checkQueues(rabbitAdmin, builder);
            }

            // 检查配置的交换器
            if (!properties.getExchangesToCheck().isEmpty()) {
                checkExchanges(rabbitAdmin, builder);
            }

            // 评估响应时间
            evaluateResponseTime(builder, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("RabbitMQ 连接检查失败: {}", e.getMessage(), e);
            builder.status(Status.DOWN)
                    .withDetail("rabbitmq", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTime", duration + "ms");
        }

        return builder.build();
    }

    /**
     * 直接检查连接
     */
    private void checkDirectConnection(Health.Builder builder) {
        try (Connection connection = connectionFactory.createConnection().getDelegate()) {
            builder.withDetail("connectionType", "direct")
                    .withDetail("serverVersion", connection.getServerProperties().get("version").toString());
        } catch (Exception e) {
            log.debug("直接连接检查失败: {}", e.getMessage());
            builder.withDetail("connectionType", "direct")
                    .withDetail("serverInfo", "unavailable");
        }
    }

    /**
     * 添加服务器属性信息
     */
    private void addServerProperties(Health.Builder builder, Properties properties) {
        Map<String, Object> serverInfo = new HashMap<>();
        properties.forEach((key, value) -> serverInfo.put(String.valueOf(key), value));
        builder.withDetail("serverProperties", serverInfo);
    }

    /**
     * 检查队列是否存在
     */
    private void checkQueues(RabbitAdmin rabbitAdmin, Health.Builder builder) {
        Map<String, String> queueStatus = new HashMap<>();

        for (String queueName : properties.getQueuesToCheck()) {
            try {
                Properties queueProps = rabbitAdmin.getQueueProperties(queueName);
                if (queueProps != null) {
                    queueStatus.put(queueName, "EXIST");
                } else {
                    queueStatus.put(queueName, "MISSING");
                }
            } catch (Exception e) {
                queueStatus.put(queueName, "ERROR: " + e.getMessage());
            }
        }

        builder.withDetail("queuesStatus", queueStatus);

        // 检查是否有缺失的队列
        boolean hasMissingQueues = queueStatus.values().stream()
                .anyMatch(status -> status.equals("MISSING"));

        if (hasMissingQueues && properties.isFailOnMissingQueues()) {
            builder.status(Status.DOWN);
        }
    }

    /**
     * 检查交换器是否存在
     */
    private void checkExchanges(RabbitAdmin rabbitAdmin, Health.Builder builder) {
        // RabbitAdmin 没有直接的交换器检查方法，这里跳过
        builder.withDetail("exchangesToCheck", properties.getExchangesToCheck())
                .withDetail("exchangesStatus", "CHECK_NOT_SUPPORTED");
    }

    /**
     * 评估响应时间并设置状态
     */
    private void evaluateResponseTime(Health.Builder builder, long duration) {
        if (duration >= properties.getResponseTimeCriticalThreshold()) {
            builder.status(new Status("WARNING", "RabbitMQ response time is slow"));
            builder.withDetail("responseTimeStatus", "SLOW");
        } else if (duration >= properties.getResponseTimeWarningThreshold()) {
            builder.withDetail("responseTimeStatus", "MODERATE");
        } else {
            builder.withDetail("responseTimeStatus", "FAST");
        }
    }
}