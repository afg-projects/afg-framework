package io.github.afgprojects.framework.core.api.storage.model;

import org.jspecify.annotations.Nullable;

/**
 * 存储类型枚举
 */
public enum StorageType {

    /**
     * 本地文件系统存储
     */
    LOCAL("local"),

    /**
     * MinIO 对象存储
     */
    MINIO("minio"),

    /**
     * 阿里云 OSS 对象存储
     */
    OSS("oss"),

    /**
     * AWS S3 对象存储
     */
    S3("s3");

    private final String code;

    StorageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取存储类型
     */
    @Nullable
    public static StorageType fromCode(String code) {
        for (StorageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
