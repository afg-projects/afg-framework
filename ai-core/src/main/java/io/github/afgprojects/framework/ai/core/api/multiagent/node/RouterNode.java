package io.github.afgprojects.framework.ai.core.api.multiagent.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 路由节点 - 根据条件决定工作流走向。
 * <p>
 * 在 DAG 工作流中，路由节点通过评估条件来选择下一个执行分支。
 */
public interface RouterNode extends WorkflowNode {

    /**
     * 获取路由条件列表
     */
    List<RouteCondition> getConditions();

    /**
     * 获取默认路由目标（无条件匹配时使用）
     */
    @Nullable
    String getDefaultTarget();
}