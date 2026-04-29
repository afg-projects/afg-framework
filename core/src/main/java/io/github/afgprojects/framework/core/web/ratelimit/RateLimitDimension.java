package io.github.afgprojects.framework.core.web.ratelimit;

/**
 * 限流维度枚举
 * <p>
 * 定义不同的限流维度，支持多级别限流控制
 * </p>
 */
public enum RateLimitDimension {

    /**
     * IP 级别限流
     * <p>
     * 根据客户端 IP 地址进行限流
     * </p>
     */
    IP,

    /**
     * 用户级别限流
     * <p>
     * 根据用户 ID 进行限流，适用于已登录用户
     * </p>
     */
    USER,

    /**
     * 租户级别限流
     * <p>
     * 根据租户 ID 进行限流，适用于多租户场景
     * </p>
     */
    TENANT,

    /**
     * 接口级别限流
     * <p>
     * 对整个接口进行限流，不限用户/IP
     * </p>
     */
    API
}
