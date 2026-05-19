package io.github.afgprojects.framework.data.core.exception;

/**
 * 元数据加载异常
 * <p>
 * 当实体元数据加载失败时抛出此异常。
 * 通常发生在反射加载元数据时，如类找不到、方法调用失败等情况。
 */
public class MetadataLoadException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 创建元数据加载异常
     *
     * @param message 错误消息
     */
    public MetadataLoadException(String message) {
        super(message);
    }

    /**
     * 创建元数据加载异常
     *
     * @param message         错误消息
     * @param entityClassName 实体类名
     */
    public MetadataLoadException(String message, String entityClassName) {
        super(message, entityClassName);
    }

    /**
     * 创建元数据加载异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public MetadataLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建元数据加载异常
     *
     * @param message         错误消息
     * @param entityClassName 实体类名
     * @param cause           原因
     */
    public MetadataLoadException(String message, String entityClassName, Throwable cause) {
        super(message, entityClassName, cause);
    }
}
