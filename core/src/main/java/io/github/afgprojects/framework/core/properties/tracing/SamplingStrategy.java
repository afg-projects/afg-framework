package io.github.afgprojects.framework.core.properties.tracing;

/**
 * 采样策略枚举。
 */
public enum SamplingStrategy {
    PROBABILITY,
    RATE_LIMITING,
    ALWAYS,
    NEVER
}
