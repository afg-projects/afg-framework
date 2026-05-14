/**
 * 限流 API 包
 * <p>
 * 提供限流的核心接口和类型定义，支持多种存储后端实现。
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimitStorage} - 存储后端 SPI 接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.WhitelistStrategy} - 白名单策略接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.DimensionResolver} - 维度解析策略接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimiter} - 限流器入口</li>
 * </ul>
 */
package io.github.afgprojects.framework.core.api.ratelimit;
