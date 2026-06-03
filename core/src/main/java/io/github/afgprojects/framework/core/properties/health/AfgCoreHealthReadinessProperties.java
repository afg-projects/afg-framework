package io.github.afgprojects.framework.core.properties.health;

import java.time.Duration;
import java.util.Set;

import lombok.Data;

/**
 * 就绪探针配置。
 */
@Data
public class AfgCoreHealthReadinessProperties {

    private boolean databaseCheckEnabled = true;
    private Duration databaseCheckTimeout = Duration.ofSeconds(3);
    private boolean redisCheckEnabled = true;
    private Duration redisCheckTimeout = Duration.ofSeconds(3);
    private boolean moduleCheckEnabled = true;
    private Set<String> requiredModules = Set.of();
}
