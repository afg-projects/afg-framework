package io.github.afgprojects.framework.ai.core.multiagent.graph;

/**
 * 节点执行状态
 */
public enum NodeStatus {
    SUCCESS,        // 成功
    FAILURE,        // 失败
    NEEDS_INPUT,    // 需要输入（人机交互）
    PARALLEL_WAIT   // 等待并行节点完成
}
