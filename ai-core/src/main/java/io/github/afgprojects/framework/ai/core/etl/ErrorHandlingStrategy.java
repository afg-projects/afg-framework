package io.github.afgprojects.framework.ai.core.etl;

/**
 * 错误处理策略枚举。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public enum ErrorHandlingStrategy {
    /**
     * 快速失败 - 遇到错误立即停止处理。
     */
    FAIL_FAST,

    /**
     * 继续处理 - 忽略错误继续处理其他文档。
     */
    CONTINUE,

    /**
     * 跳过并记录 - 跳过失败的文档并记录日志。
     */
    SKIP_AND_LOG,

    /**
     * 重试 - 重试失败的操作后继续。
     */
    RETRY
}