package io.github.afgprojects.framework.core.properties.shutdown;

import java.time.Duration;

import lombok.Data;

/**
 * 关闭阶段配置。
 */
@Data
public class AfgCoreShutdownPhaseProperties {

    private String name;
    private Duration timeout;

    public AfgCoreShutdownPhaseProperties() {}

    public AfgCoreShutdownPhaseProperties(String name, Duration timeout) {
        this.name = name;
        this.timeout = timeout;
    }
}
