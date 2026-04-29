package io.github.afgprojects.framework.integration.storage.model;

import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import io.github.afgprojects.framework.core.model.exception.ErrorCode;

/**
 * 存储模块错误码
 * <p>
 * 错误码范围：40000-40999
 */
public enum StorageErrorCode implements ErrorCode {

    // ==================== 文件操作错误 (40000-40099) ====================
    FILE_NOT_FOUND(40000, "文件不存在", ErrorCategory.BUSINESS),
    FILE_ALREADY_EXISTS(40001, "文件已存在", ErrorCategory.BUSINESS),
    FILE_UPLOAD_FAILED(40002, "文件上传失败", ErrorCategory.SYSTEM),
    FILE_DOWNLOAD_FAILED(40003, "文件下载失败", ErrorCategory.SYSTEM),
    FILE_DELETE_FAILED(40004, "文件删除失败", ErrorCategory.SYSTEM),
    FILE_COPY_FAILED(40005, "文件复制失败", ErrorCategory.SYSTEM),
    FILE_MOVE_FAILED(40006, "文件移动失败", ErrorCategory.SYSTEM),

    // ==================== 文件校验错误 (40100-40199) ====================
    FILE_TYPE_NOT_ALLOWED(40100, "文件类型不允许", ErrorCategory.BUSINESS),
    FILE_SIZE_EXCEEDED(40101, "文件大小超限", ErrorCategory.BUSINESS),
    FILE_NAME_INVALID(40102, "文件名无效", ErrorCategory.BUSINESS),
    FILE_PATH_INVALID(40103, "文件路径无效", ErrorCategory.BUSINESS),
    FILE_CONTENT_INVALID(40104, "文件内容无效", ErrorCategory.BUSINESS),

    // ==================== 存储配置错误 (40200-40299) ====================
    STORAGE_NOT_CONFIGURED(40200, "存储未配置", ErrorCategory.SYSTEM),
    STORAGE_TYPE_NOT_SUPPORTED(40201, "存储类型不支持", ErrorCategory.SYSTEM),
    STORAGE_BUCKET_NOT_FOUND(40202, "存储桶不存在", ErrorCategory.SYSTEM),
    STORAGE_BUCKET_ALREADY_EXISTS(40203, "存储桶已存在", ErrorCategory.SYSTEM),
    STORAGE_ACCESS_DENIED(40204, "存储访问被拒绝", ErrorCategory.SECURITY),
    STORAGE_CONNECTION_FAILED(40205, "存储连接失败", ErrorCategory.NETWORK),

    // ==================== 存储空间错误 (40300-40399) ====================
    STORAGE_FULL(40300, "存储空间不足", ErrorCategory.SYSTEM),
    STORAGE_QUOTA_EXCEEDED(40301, "存储配额超限", ErrorCategory.BUSINESS),

    // ==================== 预签名 URL 错误 (40400-40499) ====================
    PRESIGNED_URL_GENERATION_FAILED(40400, "预签名URL生成失败", ErrorCategory.SYSTEM),
    PRESIGNED_URL_EXPIRED(40401, "预签名URL已过期", ErrorCategory.BUSINESS),

    // ==================== 元数据错误 (40500-40599) ====================
    METADATA_UPDATE_FAILED(40500, "元数据更新失败", ErrorCategory.SYSTEM),
    METADATA_TOO_LARGE(40501, "元数据过大", ErrorCategory.BUSINESS);

    private final int code;
    private final String message;
    private final ErrorCategory category;

    StorageErrorCode(int code, String message, ErrorCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCategory getCategory() {
        return category;
    }
}
