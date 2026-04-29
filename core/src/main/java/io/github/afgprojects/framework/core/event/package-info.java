/**
 * 事件包。
 *
 * <p>提供统一的事件驱动架构，支持本地、Kafka、RabbitMQ 等多种消息中间件。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.event.DomainEvent} - 领域事件接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.event.DomainEventPublisher} - 事件发布接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.event.EventHandler} - 事件处理器注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.event.LocalEventPublisher} - 本地事件发布实现</li>
 * </ul>
 *
 * <p>分布式事件发布实现由 impl 模块提供：
 * <ul>
 *   <li>impl:afg-kafka - Kafka 事件发布实现</li>
 *   <li>impl:afg-rabbitmq - RabbitMQ 事件发布实现</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.event;