package io.github.afgprojects.framework.core.properties.tracing;

import lombok.Data;

/**
 * 追踪传播配置。
 */
@Data
public class AfgCoreTracingPropagationProperties {

    private boolean enabled = true;
    private boolean threadPoolEnabled = true;
}
