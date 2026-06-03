package io.github.afgprojects.framework.integration.storage.properties.minio;

import lombok.Data;

/**
 * MinIO 存储配置。
 */
@Data
public class StorageMinioProperties {

    private boolean enabled = false;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
    private String bucket;
    private boolean secure = false;
    private boolean pathStyleAccess = true;
}
