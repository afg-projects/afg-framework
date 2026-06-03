package io.github.afgprojects.framework.core.properties.cloudnative;

import lombok.Data;

/**
 * 云原生配置。
 */
@Data
public class AfgCoreCloudNativeProperties {

    /**
     * Kubernetes 配置。
     */
    private AfgCoreKubernetesProperties kubernetes = new AfgCoreKubernetesProperties();

    /**
     * 优雅停机配置。
     */
    private AfgCoreGracefulShutdownProperties gracefulShutdown = new AfgCoreGracefulShutdownProperties();

    /**
     * 配置外部化。
     */
    private AfgCoreConfigExternalizationProperties configExternalization = new AfgCoreConfigExternalizationProperties();

    /**
     * 探针配置。
     */
    private AfgCoreProbeProperties probe = new AfgCoreProbeProperties();
}
