package io.github.afgprojects.framework.core.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件处理器注解
 *
 * <p>用于标记事件处理方法，支持自动注册和配置。
 * 可以应用于方法上，配合 Spring 容器自动扫描注册。
 *
 * <p>示例：
 * <pre>{@code
 * @Component
 * public class UserEventHandler {
 *
 *     @EventHandler(
 *         topic = "user.created",
 *         groupId = "user-service",
 *         concurrency = 3,
 *         retryCount = 3
 *     )
 *     public void handleUserCreated(UserCreatedEvent event) {
 *         // 处理用户创建事件
 *         log.info("User created: {}", event.getUserId());
 *     }
 *
 *     @EventHandler(topic = "order.completed")
 *     public void handleOrderCompleted(OrderCompletedEvent event) {
 *         // 处理订单完成事件
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    /**
     * 订阅的主题名称
     *
     * <p>支持多个主题，使用逗号分隔。
     * 支持通配符模式（取决于消息中间件实现）：
     * <ul>
     *   <li>{@code user.*} - 匹配 user.created、user.deleted 等</li>
     *   <li>{@code order.*.completed} - 匹配 order.123.completed 等</li>
     * </ul>
     *
     * @return 主题名称数组
     */
    String[] topic() default {};

    /**
     * 消费者组 ID
     *
     * <p>用于消费者分组和负载均衡。
     * 同一组内的消费者共同分担消息处理，不同组独立消费。
     *
     * @return 消费者组 ID，默认为空（使用默认组）
     */
    String groupId() default "";

    /**
     * 并发消费线程数
     *
     * <p>指定处理事件的并发线程数，适用于高吞吐量场景。
     *
     * @return 并发数，默认为 1
     */
    int concurrency() default 1;

    /**
     * 重试次数
     *
     * <p>事件处理失败时的最大重试次数。
     * 重试次数耗尽后，事件将被发送到死信队列（如果配置）。
     *
     * @return 重试次数，默认为 3
     */
    int retryCount() default 3;

    /**
     * 重试间隔（毫秒）
     *
     * <p>每次重试之间的间隔时间。支持指数退避策略。
     *
     * @return 重试间隔，默认为 1000 毫秒
     */
    long retryInterval() default 1000;

    /**
     * 是否启用指数退避
     *
     * <p>启用后，重试间隔会按照指数增长：{@code interval * 2^retryCount}
     *
     * @return 是否启用指数退避，默认为 true
     */
    boolean exponentialBackoff() default true;

    /**
     * 死信队列主题
     *
     * <p>重试次数耗尽后，事件将被发送到此主题。
     * 留空则使用默认死信队列命名规则。
     *
     * @return 死信队列主题，默认为空
     */
    String deadLetterTopic() default "";

    /**
     * 是否自动确认
     *
     * <p>启用后，消息在接收后立即确认，即使处理失败。
     * 关闭后，只有处理成功才会确认消息。
     *
     * @return 是否自动确认，默认为 false
     */
    boolean autoAck() default false;

    /**
     * 事件类型过滤
     *
     * <p>只处理指定类型的事件。
     * 留空则处理所有类型的事件。
     *
     * @return 事件类型数组
     */
    String[] eventTypes() default {};
}
