package io.github.afgprojects.framework.ai.core.api.multiagent.state;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatus {
    RUNNING,    // 执行中
    PAUSED,     // 已暂停（等待人机交互）
    COMPLETED,  // 已完成
    FAILED      // 已失败
}
