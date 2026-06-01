package io.github.afgprojects.framework.ai.agent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Agent 配置属性
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.agent")
public class AiAgentProperties {

    private ReAct reAct = new ReAct();
    private ToolExecution toolExecution = new ToolExecution();

    public ReAct getReAct() {
        return reAct;
    }

    public void setReAct(ReAct reAct) {
        this.reAct = reAct;
    }

    public ToolExecution getToolExecution() {
        return toolExecution;
    }

    public void setToolExecution(ToolExecution toolExecution) {
        this.toolExecution = toolExecution;
    }

    /**
     * ReAct 推理配置
     */
    public static class ReAct {
        private int maxSteps = 10;

        public int getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
        }
    }

    /**
     * 工具执行配置
     */
    public static class ToolExecution {
        private int maxIterations = 5;
        private long timeoutMs = 30000;

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}