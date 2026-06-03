package io.github.afgprojects.framework.core.properties.event;

import lombok.Data;

/**
 * 本地事件配置。
 */
@Data
public class AfgCoreEventLocalProperties {

    private boolean async;
    private int threadPoolSize = 4;
}
