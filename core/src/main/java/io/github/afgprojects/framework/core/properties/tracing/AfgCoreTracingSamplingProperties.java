package io.github.afgprojects.framework.core.properties.tracing;

import lombok.Data;

/**
 * 追踪采样配置。
 */
@Data
public class AfgCoreTracingSamplingProperties {

    private SamplingStrategy strategy = SamplingStrategy.PROBABILITY;
    private double probability = 1.0;
    private int rate = 100;
}
