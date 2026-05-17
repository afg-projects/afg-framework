package io.github.afgprojects.framework.ai.core.multiagent.state;

/**
 * 检查点策略
 */
public enum CheckpointPolicy {
    EVERY_NODE,      // 每个节点执行后
    EVERY_STAGE,     // 每个阶段完成后
    MANUAL,          // 手动调用
    ON_INTERRUPT,    // 仅在人机交互节点
    NONE             // 不持久化
}
