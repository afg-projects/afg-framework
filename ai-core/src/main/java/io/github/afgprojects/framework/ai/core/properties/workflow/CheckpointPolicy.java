package io.github.afgprojects.framework.ai.core.properties.workflow;

/**
 * 检查点策略枚举。
 */
public enum CheckpointPolicy {
    EVERY_NODE,
    EVERY_STAGE,
    MANUAL,
    ON_INTERRUPT,
    NONE
}
