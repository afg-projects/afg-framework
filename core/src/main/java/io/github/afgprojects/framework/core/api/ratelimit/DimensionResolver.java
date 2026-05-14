package io.github.afgprojects.framework.core.api.ratelimit;

/**
 * 维度解析策略接口
 * <p>
 * 解析限流维度的值，如 IP、用户 ID、租户 ID 等。
 * 业务可以实现此接口提供自定义的维度解析逻辑。
 */
public interface DimensionResolver {

    /**
     * 解析维度值
     *
     * @param dimension 限流维度
     * @return 维度值
     */
    String resolve(RateLimitDimension dimension);
}
