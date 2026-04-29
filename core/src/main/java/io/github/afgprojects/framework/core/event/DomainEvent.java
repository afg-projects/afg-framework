package io.github.afgprojects.framework.core.event;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 领域事件接口（DDD 风格）
 *
 * <p><strong>注意：</strong>此接口用于 DDD 领域事件，与 {@link io.github.afgprojects.framework.core.api.event.DomainEvent} 不同：
 * <ul>
 *   <li>此接口 - DDD 风格，需要实现 getEventId、getEventType、getTimestamp 等方法，用于本地事件总线</li>
 *   <li>{@code api.event.DomainEvent} - 简单 record，用于 Kafka/RabbitMQ 等消息队列</li>
 * </ul>
 *
 * <p>所有领域事件都需要实现此接口，提供事件的基本属性。
 * 领域事件是 DDD 中的重要概念，用于表达领域中发生的事实。
 *
 * <p>示例：
 * <pre>{@code
 * public record UserCreatedEvent(
 *     String eventId,
 *     String userId,
 *     String username,
 *     Instant timestamp
 * ) implements DomainEvent<UserData> {
 *
 *     @Override
 *     public String getEventId() { return eventId; }
 *
 *     @Override
 *     public String getEventType() { return "user.created"; }
 *
 *     @Override
 *     public Instant getTimestamp() { return timestamp; }
 *
 *     @Override
 *     public String getAggregateId() { return userId; }
 *
 *     @Override
 *     public UserData getPayload() { return new UserData(userId, username); }
 * }
 * }</pre>
 *
 * @param <T> 事件载荷类型
 * @since 1.0.0
 */
public interface DomainEvent<T> {

    /**
     * 获取事件唯一标识
     *
     * <p>用于事件去重和追踪，通常使用 UUID
     *
     * @return 事件 ID
     */
    @NonNull String getEventId();

    /**
     * 获取事件类型
     *
     * <p>用于事件路由和过滤，建议格式：{@code 聚合.动作}
     * 例如：{@code user.created}、{@code order.completed}
     *
     * @return 事件类型
     */
    @NonNull String getEventType();

    /**
     * 获取事件发生时间戳
     *
     * @return 时间戳
     */
    @NonNull Instant getTimestamp();

    /**
     * 获取聚合根 ID
     *
     * <p>聚合根是 DDD 中的概念，表示一组相关对象的边界。
     * 例如：订单事件的聚合根是订单 ID，用户事件的聚合根是用户 ID。
     *
     * @return 聚合根 ID，可能为空（对于全局事件）
     */
    @Nullable String getAggregateId();

    /**
     * 获取事件载荷
     *
     * <p>包含事件的具体数据，例如用户创建事件可能包含用户信息
     *
     * @return 事件载荷，可能为空
     */
    @Nullable T getPayload();

    /**
     * 获取事件版本
     *
     * <p>用于事件版本控制和向后兼容，默认为 1
     *
     * @return 事件版本
     */
    default int getVersion() {
        return 1;
    }

    /**
     * 获取事件来源
     *
     * <p>标识事件产生的服务或模块，用于分布式追踪
     *
     * @return 事件来源，默认为空
     */
    default @Nullable String getSource() {
        return null;
    }
}
