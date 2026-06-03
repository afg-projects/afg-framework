package io.github.afgprojects.framework.ai.core.properties.agent;

import lombok.Data;

/**
 * 智能体配置。
 */
@Data
public class AgentConfig {

    /**
     * 是否启用智能体。
     */
    private boolean enabled = true;

    /**
     * 最大迭代次数。
     */
    private int maxIterations = 10;

    /**
     * 超时时间（毫秒）。
     */
    private long timeoutMs = 30000L;

    /**
     * ReAct 执行器配置。
     */
    private ReActConfig reAct = new ReActConfig();
}
