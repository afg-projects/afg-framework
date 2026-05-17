package io.github.afgprojects.framework.ai.core.multiagent.graph;

import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowInput;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 状态图定义接口
 *
 * <p>借鉴 LangGraph 设计，支持 DAG 工作流、条件分支、并行执行。
 */
public interface StateGraph {

    /**
     * 添加节点
     */
    @NonNull
    StateGraph addNode(@NonNull String id, @NonNull Node node);

    /**
     * 添加边（无条件转移）
     */
    @NonNull
    StateGraph addEdge(@NonNull String from, @NonNull String to);

    /**
     * 添加条件边
     *
     * @param from      源节点
     * @param condition 条件判断函数
     * @param branches  分支映射：条件结果 -> 目标节点ID
     */
    @NonNull
    StateGraph addConditionalEdge(@NonNull String from, @NonNull EdgeCondition condition, @NonNull Map<String, String> branches);

    /**
     * 设置入口节点
     */
    @NonNull
    StateGraph setEntryPoint(@NonNull String nodeId);

    /**
     * 设置结束节点
     */
    @NonNull
    StateGraph setFinishPoint(@NonNull String nodeId);

    /**
     * 执行工作流
     */
    @NonNull
    WorkflowState execute(@NonNull WorkflowInput input);

    /**
     * 从检查点恢复执行
     */
    @NonNull
    WorkflowState resumeFromCheckpoint(@NonNull String checkpointId);

    /**
     * 获取图名称
     */
    @NonNull
    String getName();
}
