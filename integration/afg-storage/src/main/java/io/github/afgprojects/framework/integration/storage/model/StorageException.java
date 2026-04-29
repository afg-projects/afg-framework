package io.github.afgprojects.framework.integration.storage.model;

import java.io.Serial;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 存储异常
 */
public class StorageException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StorageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public StorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建文件不存在异常
     */
    public static StorageException fileNotFound(String key) {
        return new StorageException(StorageErrorCode.FILE_NOT_FOUND, "文件不存在: " + key);
    }

    /**
     * 创建文件不存在异常（带原始异常）
     */
    public static StorageException fileNotFound(String key, Throwable cause) {
        return new StorageException(StorageErrorCode.FILE_NOT_FOUND, "文件不存在: " + key, cause);
    }

    /**
     * 创建上传失败异常
     */
    public static StorageException uploadFailed(String key, Throwable cause) {
        return new StorageException(StorageErrorCode.FILE_UPLOAD_FAILED,
                "文件上传失败: " + key, cause);
    }

    /**
     * 创建下载失败异常
     */
    public static StorageException downloadFailed(String key, Throwable cause) {
        return new StorageException(StorageErrorCode.FILE_DOWNLOAD_FAILED,
                "文件下载失败: " + key, cause);
    }

    /**
     * 创建删除失败异常
     */
    public static StorageException deleteFailed(String key, Throwable cause) {
        return new StorageException(StorageErrorCode.FILE_DELETE_FAILED,
                "文件删除失败: " + key, cause);
    }

    /**
     * 创建存储未配置异常
     */
    public static StorageException notConfigured() {
        return new StorageException(StorageErrorCode.STORAGE_NOT_CONFIGURED);
    }

    /**
     * 创建存储类型不支持异常
     */
    public static StorageException typeNotSupported(String type) {
        return new StorageException(StorageErrorCode.STORAGE_TYPE_NOT_SUPPORTED,
                "不支持的存储类型: " + type);
    }

    /**
     * 创建连接失败异常
     */
    public static StorageException connectionFailed(String message, Throwable cause) {
        return new StorageException(StorageErrorCode.STORAGE_CONNECTION_FAILED, message, cause);
    }
}
