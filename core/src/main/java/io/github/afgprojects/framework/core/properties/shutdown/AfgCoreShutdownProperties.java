package io.github.afgprojects.framework.core.properties.shutdown;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 优雅关闭配置。
 */
@Data
public class AfgCoreShutdownProperties {

    /**
     * 是否启用优雅关闭。
     */
    private boolean enabled = true;

    /**
     * 关闭超时时间。
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 是否启用同一阶段内相同 order 的回调并行执行。
     */
    private boolean parallelExecutionEnabled;

    /**
     * 关闭阶段配置。
     */
    private List<AfgCoreShutdownPhaseProperties> phases = new ArrayList<>();
}
