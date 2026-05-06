package io.github.afgprojects.framework.data.core.exception;

/**
 * 数据访问基础异常
 * <p>
 * 所有数据访问相关异常的基类，提供统一的异常层次结构。
 */
public abstract class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 实体类名
     */
    private final String entityClassName;

    /**
     * 创建数据访问异常
     *
     * @param message 错误消息
     */
    protected DataAccessException(String message) {
        super(message);
        this.entityClassName = null;
    }

    /**
     * 创建数据访问异常
     *
     * @param message         错误消息
     * @param entityClassName 实体类名
     */
    protected DataAccessException(String message, String entityClassName) {
        super(message);
        this.entityClassName = entityClassName;
    }

    /**
     * 创建数据访问异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    protected DataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.entityClassName = null;
    }

    /**
     * 创建数据访问异常
     *
     * @param message         错误消息
     * @param entityClassName 实体类名
     * @param cause           原因
     */
    protected DataAccessException(String message, String entityClassName, Throwable cause) {
        super(message, cause);
        this.entityClassName = entityClassName;
    }

    /**
     * 获取实体类名
     *
     * @return 实体类名，可能为 null
     */
    public String getEntityClassName() {
        return entityClassName;
    }
}