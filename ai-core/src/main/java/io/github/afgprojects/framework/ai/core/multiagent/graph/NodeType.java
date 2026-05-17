package io.github.afgprojects.framework.ai.core.multiagent.graph;

/**
 * 节点类型枚举
 */
public enum NodeType {
    AGENT,      // Agent 执行节点
    TOOL,       // 工具执行节点
    HUMAN,      // 人机交互节点
    ROUTER,     // 路由节点
    PARALLEL    // 并行执行节点
}
