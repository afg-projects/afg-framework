package io.github.afgprojects.framework.core.batch;

/**
 * 批量操作异常
 * <p>
 * 当批量操作遇到无法恢复的错误时抛出
 * </p>
 */
public class BatchOperationException extends RuntimeException {

    /**
     * 创建批量操作异常
     *
     * @param message 错误消息
     */
    public BatchOperationException(String message) {
        super(message);
    }

    /**
     * 创建批量操作异常
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BatchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
