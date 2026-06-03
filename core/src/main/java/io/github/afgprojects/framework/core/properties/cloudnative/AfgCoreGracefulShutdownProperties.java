package io.github.afgprojects.framework.core.properties.cloudnative;

import java.time.Duration;

import lombok.Data;

/**
 * 优雅停机配置。
 */
@Data
public class AfgCoreGracefulShutdownProperties {

    private boolean enabled = true;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean waitForRequests = true;
    private Duration requestWaitTimeout = Duration.ofSeconds(10);
}
