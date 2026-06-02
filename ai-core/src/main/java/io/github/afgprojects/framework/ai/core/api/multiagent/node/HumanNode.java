package io.github.afgprojects.framework.ai.core.api.multiagent.node;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 人工干预节点 - 将 HumanInteraction 适配为工作流节点。
 * <p>
 * 在 DAG 工作流中，人工决策作为可执行节点参与编排，
 * 当执行到此节点时暂停，等待人工输入后继续。
 */
public interface HumanNode extends WorkflowNode {

    /**
     * 获取人工交互处理器
     */
    HumanInteraction getHumanInteraction();

    /**
     * 获取提示信息
     */
    @Nullable
    default String getPrompt() {
        return null;
    }
}