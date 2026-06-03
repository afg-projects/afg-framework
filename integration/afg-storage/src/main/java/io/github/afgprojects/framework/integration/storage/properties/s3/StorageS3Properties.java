package io.github.afgprojects.framework.integration.storage.properties.s3;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * AWS S3 存储配置。
 */
@Data
public class StorageS3Properties {

    private boolean enabled = false;
    private String region = "us-east-1";
    private @Nullable String accessKeyId;
    private @Nullable String secretAccessKey;
    private String bucket;
}
