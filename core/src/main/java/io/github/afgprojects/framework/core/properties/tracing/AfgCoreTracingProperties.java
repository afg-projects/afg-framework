package io.github.afgprojects.framework.core.properties.tracing;

import lombok.Data;

/**
 * 追踪配置。
 */
@Data
public class AfgCoreTracingProperties {

    /**
     * 是否启用追踪功能。
     */
    private boolean enabled = true;

    /**
     * 注解相关配置。
     */
    private AfgCoreTracingAnnotationProperties annotations = new AfgCoreTracingAnnotationProperties();

    /**
     * 采样配置。
     */
    private AfgCoreTracingSamplingProperties sampling = new AfgCoreTracingSamplingProperties();

    /**
     * Baggage 配置。
     */
    private AfgCoreTracingBaggageProperties baggage = new AfgCoreTracingBaggageProperties();

    /**
     * 传播配置。
     */
    private AfgCoreTracingPropagationProperties propagation = new AfgCoreTracingPropagationProperties();

    /**
     * Zipkin 配置。
     */
    private AfgCoreTracingZipkinProperties zipkin = new AfgCoreTracingZipkinProperties();

    /**
     * Jaeger 配置。
     */
    private AfgCoreTracingJaegerProperties jaeger = new AfgCoreTracingJaegerProperties();
}
