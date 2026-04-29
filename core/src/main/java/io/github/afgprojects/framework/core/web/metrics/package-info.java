/**
 * 指标监控包。
 *
 * <p>提供方法级别的指标监控能力，基于 Micrometer 实现。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.metrics.TimedMetric} - 方法耗时指标注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.metrics.CountedMetric} - 方法计数指标注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.metrics.MetricsAspect} - 指标切面</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.metrics.MetricsProperties} - 指标配置属性</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.metrics;
