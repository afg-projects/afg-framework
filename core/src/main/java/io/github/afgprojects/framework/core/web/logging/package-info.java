/**
 * 日志增强包。
 *
 * <p>提供结构化日志能力，支持 MDC 上下文和 JSON 格式输出。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.logging.MdcFilter} - MDC 过滤器，自动注入 traceId 等信息</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.logging.StructuredLogbackLayout} - 结构化 Logback 编码器（JSON 格式）</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.logging.LoggingProperties} - 日志配置属性</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.logging;
