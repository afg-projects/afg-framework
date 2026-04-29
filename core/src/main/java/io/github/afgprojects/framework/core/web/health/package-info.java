/**
 * 健康检查包。
 *
 * <p>提供多级别的健康检查能力，支持 Kubernetes Liveness/Readiness 探针。
 *
 * <p>健康检查级别：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.HealthCheckLevel#LIVENESS} - 存活检查</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.HealthCheckLevel#READINESS} - 就绪检查</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.HealthCheckLevel#DEEP} - 深度检查</li>
 * </ul>
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.LivenessHealthIndicator} - 存活探针健康指示器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.ReadinessHealthIndicator} - 就绪探针健康指示器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.ModuleHealthIndicator} - 模块健康指示器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.DataSourceHealthIndicator} - 数据源健康指示器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.HealthCheckProperties} - 健康检查配置属性</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.DataSourceHealthProperties} - 数据源健康检查配置属性</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.health.HealthCheckLevel} - 健康检查级别枚举</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.health;
