package io.github.afgprojects.framework.ai.core.properties.agent;

import lombok.Data;

/**
 * ReAct 执行器配置。
 */
@Data
public class ReActConfig {

    /**
     * 是否启用 ReAct 执行器。
     */
    private boolean enabled = true;

    /**
     * 最大推理步数。
     */
    private int maxSteps = 10;
}
