/**
 * 限流模块
 * <p>
 * 提供基于 Redisson 的分布式限流能力，支持多种限流维度和算法。
 * </p>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.ratelimit.RateLimit} - 限流注解，支持多级限流</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.ratelimit.RateLimits} - 多级限流注解容器</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimiter} - 限流器核心实现</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.ratelimit.RateLimitInterceptor} - 限流切面</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.AfgCoreProperties.RateLimitConfig} - 配置属性（在 AfgCoreProperties 中）</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension} - 限流维度枚举</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimitAlgorithm} - 限流算法枚举</li>
 *   <li>{@link io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult} - 限流结果</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.ratelimit.RateLimitResponseHeaderFilter} - 响应头过滤器</li>
 * </ul>
 *
 * <h2>限流维度</h2>
 * <ul>
 *   <li><b>IP</b> - 基于客户端 IP 地址限流</li>
 *   <li><b>USER</b> - 基于用户 ID 限流</li>
 *   <li><b>TENANT</b> - 基于租户 ID 限流（多租户场景）</li>
 *   <li><b>API</b> - 接口级别限流（全局）</li>
 * </ul>
 *
 * <h2>限流算法</h2>
 * <ul>
 *   <li><b>TOKEN_BUCKET</b> - 令牌桶算法，允许突发流量</li>
 *   <li><b>SLIDING_WINDOW</b> - 滑动窗口算法，限流更精确</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 *
 * <h3>基本用法</h3>
 * <pre>{@code
 * // 基于IP限流，每秒10个请求
 * @RateLimit(key = "api.query", rate = 10, dimension = RateLimitDimension.IP)
 * public Result<User> queryUser(String id) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * <h3>多级限流</h3>
 * <pre>{@code
 * // 同时限制IP和用户
 * @RateLimit(key = "api.search", rate = 100, dimension = RateLimitDimension.IP)
 * @RateLimit(key = "api.search", rate = 50, dimension = RateLimitDimension.USER)
 * public Result<List<Item>> search(String keyword) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * <h3>滑动窗口算法</h3>
 * <pre>{@code
 * @RateLimit(key = "api.order", rate = 10, algorithm = RateLimitAlgorithm.SLIDING_WINDOW, windowSize = 60)
 * public Result<Order> createOrder(OrderRequest request) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * <h3>回退方法</h3>
 * <pre>{@code
 * @RateLimit(key = "api.upload", rate = 2, fallbackMethod = "uploadFallback")
 * public Result<Void> upload(File file) {
 *     // 业务逻辑
 * }
 *
 * public Result<Void> uploadFallback(File file) {
 *     return Results.fail("服务繁忙，请稍后再试");
 * }
 * }</pre>
 *
 * <h2>配置示例</h2>
 * <pre>
 * afg:
 *   core:
 *     rate-limit:
 *       enabled: true
 *       default-rate: 10
 *       default-burst: 20
 *       key-prefix: rateLimit
 *       default-algorithm: TOKEN_BUCKET
 *
 *       # 维度配置
 *       dimensions:
 *         ip:
 *           rate: 20
 *           burst: 40
 *         user:
 *           rate: 50
 *           burst: 100
 *
 *       # 白名单配置
 *       whitelist:
 *         enabled: true
 *         ips: 192.168.1.100,10.0.0.*
 *         user-ids: 1,2,3
 *         usernames: admin,system
 *
 *       # 响应头配置
 *       response-headers:
 *         enabled: true
 *
 *       # 本地限流配置（单机模式）
 *       local:
 *         enabled: false
 *         cache-size: 10000
 *         expire-after-seconds: 3600
 *
 *       # 回退配置
 *       fallback:
 *         enabled: true
 *         default-message: "请求过于频繁，请稍后再试"
 *         failure-mode: ALLOW
 * </pre>
 *
 * <h2>响应头</h2>
 * <p>限流响应会包含以下 HTTP 头：</p>
 * <ul>
 *   <li><b>X-RateLimit-Limit</b> - 限流阈值</li>
 *   <li><b>X-RateLimit-Remaining</b> - 剩余配额</li>
 *   <li><b>X-RateLimit-Reset</b> - 重置时间（Unix 时间戳）</li>
 *   <li><b>Retry-After</b> - 重试等待时间（仅限流时返回）</li>
 * </ul>
 */
package io.github.afgprojects.framework.core.web.ratelimit;
