package io.github.afgprojects.framework.ai.core.api.multiagent.node;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Agent 节点 - 将 Agent 适配为工作流节点。
 * <p>
 * 在 DAG 工作流中，Agent 作为可执行节点参与编排。
 */
public interface AgentNode extends WorkflowNode {

    /**
     * 获取关联的 Agent
     */
    Agent getAgent();

    /**
     * 获取 Agent 的角色描述
     */
    @Nullable
    default String getRoleDescription() {
        return null;
    }
}
