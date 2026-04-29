/**
 * 数据源负载均衡包。
 *
 * <p>提供从库负载均衡策略和健康检查功能。
 *
 * <p>核心组件：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.datasource.lb.LoadBalanceStrategy} - 负载均衡策略接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.datasource.lb.RoundRobinStrategy} - 轮询策略</li>
 *   <li>{@link io.github.afgprojects.framework.core.datasource.lb.WeightedStrategy} - 权重策略</li>
 *   <li>{@link io.github.afgprojects.framework.core.datasource.lb.LeastConnectionsStrategy} - 最少连接策略</li>
 *   <li>{@link io.github.afgprojects.framework.core.datasource.lb.ReadDataSourceLoadBalancer} - 读数据源负载均衡器</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.datasource.lb;