package io.github.afgprojects.framework.integration.storage.properties.oss;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 阿里云 OSS 存储配置。
 */
@Data
public class StorageOssProperties {

    private boolean enabled = false;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private @Nullable String securityToken;
    private String bucket;
}
