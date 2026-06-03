package io.github.afgprojects.framework.core.properties.cloudnative;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Kubernetes 配置。
 */
@Data
public class AfgCoreKubernetesProperties {

    private boolean enabled = true;
    private @Nullable String namespace;
    private @Nullable String serviceAccount;
    private @Nullable String podName;
    private @Nullable String podIp;
    private @Nullable String nodeName;
}
