package io.github.afgprojects.framework.core.event;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 事件配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   event:
 *     enabled: true
 *     type: LOCAL  # LOCAL, KAFKA, RABBITMQ
 *     local:
 *       async: true
 *     kafka:
 *       bootstrap-servers: localhost:9092
 *       producer:
 *         acks: all
 *         retries: 3
 *       consumer:
 *         auto-offset-reset: earliest
 *     rabbitmq:
 *       host: localhost
 *       port: 5672
 *       username: guest
 *       password: guest
 *       exchange: afg.events
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.event")
public class EventProperties {

    /**
     * 是否启用事件驱动
     */
    private boolean enabled = true;

    /**
     * 事件发布类型
     */
    private EventType type = EventType.LOCAL;

    /**
     * 默认主题名称
     *
     * <p>当事件未指定主题时使用
     */
    private String defaultTopic = "afg.events";

    /**
     * 本地事件配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * Kafka 配置
     */
    private KafkaConfig kafka = new KafkaConfig();

    /**
     * RabbitMQ 配置
     */
    private RabbitMQConfig rabbitmq = new RabbitMQConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 死信队列配置
     */
    private DeadLetterConfig deadLetter = new DeadLetterConfig();

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 本地事件（基于 Spring ApplicationEventPublisher）
         */
        LOCAL,

        /**
         * Kafka 消息队列
         */
        KAFKA,

        /**
         * RabbitMQ 消息队列
         */
        RABBITMQ
    }

    /**
     * 本地事件配置
     */
    @Data
    public static class LocalConfig {
        /**
         * 是否启用异步发布
         */
        private boolean async;

        /**
         * 异步线程池大小
         */
        private int threadPoolSize = 4;
    }

    /**
     * Kafka 配置
     */
    @Data
    public static class KafkaConfig {
        /**
         * Kafka 服务器地址
         */
        private @Nullable String bootstrapServers;

        /**
         * Producer 配置
         */
        private Map<String, Object> producer = new HashMap<>();

        /**
         * Consumer 配置
         */
        private Map<String, Object> consumer = new HashMap<>();

        /**
         * 是否自动创建主题
         */
        private boolean autoCreateTopics = true;

        /**
         * 主题分区数
         */
        private int partitions = 3;

        /**
         * 主题副本数
         */
        private short replicationFactor = 1;
    }

    /**
     * RabbitMQ 配置
     */
    @Data
    public static class RabbitMQConfig {
        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口号
         */
        private int port = 5672;

        /**
         * 用户名
         */
        private String username = "guest";

        /**
         * 密码
         */
        private String password = "guest";

        /**
         * 虚拟主机
         */
        private String virtualHost = "/";

        /**
         * 默认交换机名称
         */
        private String exchange = "afg.events";

        /**
         * 默认队列名称前缀
         */
        private String queuePrefix = "afg.queue.";

        /**
         * 消息确认模式
         */
        private AckMode ackMode = AckMode.AUTO;

        /**
         * 预取数量
         */
        private int prefetchCount = 10;

        /**
         * 是否自动创建队列和交换机
         */
        private boolean autoDeclare = true;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始重试间隔（毫秒）
         */
        private long initialInterval = 1000;

        /**
         * 重试间隔乘数
         */
        private double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxInterval = 30000;
    }

    /**
     * 死信队列配置
     */
    @Data
    public static class DeadLetterConfig {
        /**
         * 是否启用死信队列
         */
        private boolean enabled = true;

        /**
         * 死信队列主题前缀
         */
        private String topicPrefix = "dlq.";

        /**
         * 死信队列保留时间（毫秒），0 表示永久保留
         */
        private long retentionMs;
    }

    /**
     * 消息确认模式
     */
    public enum AckMode {
        /**
         * 自动确认
         */
        AUTO,

        /**
         * 手动确认
         */
        MANUAL,

        /**
         * 批量确认
         */
        BATCH
    }
}
