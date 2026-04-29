package io.github.afgprojects.framework.core.web.ratelimit;

/**
 * 限流算法类型
 * <p>
 * 支持两种主流限流算法
 * </p>
 */
public enum RateLimitAlgorithm {

    /**
     * 令牌桶算法
     * <p>
     * 以固定速率向桶中添加令牌，请求到达时尝试获取令牌。
     * 特点：允许一定程度的突发流量，平滑限流效果更好。
     * </p>
     */
    TOKEN_BUCKET,

    /**
     * 滑动窗口算法
     * <p>
     * 基于时间窗口的计数限流，窗口随时间滑动。
     * 特点：限流更精确，但突发流量控制不如令牌桶。
     * </p>
     */
    SLIDING_WINDOW
}
