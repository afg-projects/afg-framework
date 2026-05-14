package io.github.afgprojects.framework.core.api.ratelimit;

/**
 * 白名单策略接口
 * <p>
 * 检查请求是否在白名单中，白名单内的请求不受限流控制。
 * 业务可以实现此接口提供自定义的白名单检查逻辑。
 */
public interface WhitelistStrategy {

    /**
     * 检查当前请求是否在白名单中
     *
     * @param dimension 限流维度
     * @return 是否在白名单中
     */
    boolean isInWhitelist(RateLimitDimension dimension);
}
