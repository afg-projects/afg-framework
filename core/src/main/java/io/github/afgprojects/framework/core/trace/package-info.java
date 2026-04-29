/**
 * 分布式追踪包。
 *
 * <p>提供分布式链路追踪能力，支持跨服务调用链路传播。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link Traced} - 标记需要追踪的方法注解</li>
 *   <li>{@link TracedAspect} - 追踪切面，自动创建 Span</li>
 *   <li>{@link TracingProperties} - 追踪配置属性</li>
 *   <li>{@link TraceContextPropagator} - 跨线程追踪上下文传播器</li>
 *   <li>{@link BaggageContext} - Baggage 上下文管理</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.trace;
