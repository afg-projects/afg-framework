package io.github.afgprojects.framework.core.trace;

/**
 * 采样策略枚举
 * <p>
 * 定义追踪采样的策略
 * </p>
 */
public enum SamplingStrategy {

    /**
     * 概率采样
     * <p>
     * 按照配置的概率进行采样，概率范围为 0.0 - 1.0
     * </p>
     */
    PROBABILITY,

    /**
     * 限流采样
     * <p>
     * 按照每秒最大请求数进行采样，超出限制的请求不会被追踪
     * </p>
     */
    RATE_LIMITING,

    /**
     * 全部采样
     * <p>
     * 所有请求都被追踪
     * </p>
     */
    ALWAYS,

    /**
     * 不采样
     * <p>
     * 所有请求都不被追踪
     * </p>
     */
    NEVER
}
