package io.github.afgprojects.framework.core.properties.health;

import java.time.Duration;

import lombok.Data;

/**
 * 深度检查配置。
 */
@Data
public class AfgCoreHealthDeepProperties {

    private Duration timeout = Duration.ofSeconds(10);
    private boolean externalServiceCheckEnabled = true;
    private boolean configCenterCheckEnabled = true;
    private Duration externalServiceTimeout = Duration.ofSeconds(5);
}
