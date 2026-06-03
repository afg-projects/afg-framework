package io.github.afgprojects.framework.core.properties.cloudnative;

import lombok.Data;

/**
 * 探针配置。
 */
@Data
public class AfgCoreProbeProperties {

    private boolean enabled = true;
    private AfgCoreProbeDetailProperties liveness = new AfgCoreProbeDetailProperties();
    private AfgCoreProbeDetailProperties readiness = new AfgCoreProbeDetailProperties();
    private AfgCoreProbeDetailProperties startup = new AfgCoreProbeDetailProperties();
}
