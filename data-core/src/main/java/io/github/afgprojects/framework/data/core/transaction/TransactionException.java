package io.github.afgprojects.framework.data.core.transaction;

/**
 * 事务异常
 * <p>
 * 表示事务操作过程中发生的错误。
 */
public class TransactionException extends RuntimeException {

    /**
     * 创建事务异常
     *
     * @param message 错误消息
     */
    public TransactionException(String message) {
        super(message);
    }

    /**
     * 创建事务异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}