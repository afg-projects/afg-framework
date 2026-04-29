package io.github.afgprojects.framework.core.event;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

/**
 * 领域事件发布接口
 *
 * <p>提供统一的事件发布 API，支持同步和异步发布。
 * 实现类可以基于不同的消息中间件，如本地事件总线、Kafka、RabbitMQ 等。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private final DomainEventPublisher eventPublisher;
 *
 *     public void createUser(CreateUserRequest request) {
 *         User user = userRepository.save(new User(request));
 *
 *         // 发布领域事件
 *         UserCreatedEvent event = new UserCreatedEvent(
 *             UUID.randomUUID().toString(),
 *             user.getId(),
 *             user.getUsername(),
 *             Instant.now()
 *         );
 *         eventPublisher.publish(event);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DomainEventPublisher {

    /**
     * 同步发布事件
     *
     * <p>阻塞直到事件成功发送到消息中间件
     *
     * @param event 领域事件
     * @param <T> 事件载荷类型
     * @throws EventPublishException 事件发布失败时抛出
     */
    <T> void publish(@NonNull DomainEvent<T> event);

    /**
     * 异步发布事件
     *
     * <p>非阻塞方式发布事件，返回 CompletableFuture 以便追踪发布结果
     *
     * @param event 领域事件
     * @param <T> 事件载荷类型
     * @return 发布结果的 CompletableFuture
     */
    <T> CompletableFuture<Void> publishAsync(@NonNull DomainEvent<T> event);

    /**
     * 发布事件到指定主题
     *
     * <p>用于需要自定义主题名称的场景
     *
     * @param topic 主题名称
     * @param event 领域事件
     * @param <T> 事件载荷类型
     * @throws EventPublishException 事件发布失败时抛出
     */
    default <T> void publish(@NonNull String topic, @NonNull DomainEvent<T> event) {
        publish(event);
    }

    /**
     * 异步发布事件到指定主题
     *
     * @param topic 主题名称
     * @param event 领域事件
     * @param <T> 事件载荷类型
     * @return 发布结果的 CompletableFuture
     */
    default <T> CompletableFuture<Void> publishAsync(@NonNull String topic, @NonNull DomainEvent<T> event) {
        return publishAsync(event);
    }
}
