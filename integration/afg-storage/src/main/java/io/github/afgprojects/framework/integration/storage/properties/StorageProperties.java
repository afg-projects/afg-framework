package io.github.afgprojects.framework.integration.storage.properties;

import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.integration.storage.properties.local.StorageLocalProperties;
import io.github.afgprojects.framework.integration.storage.properties.minio.StorageMinioProperties;
import io.github.afgprojects.framework.integration.storage.properties.oss.StorageOssProperties;
import io.github.afgprojects.framework.integration.storage.properties.s3.StorageS3Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 存储配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.storage")
public class StorageProperties {

    private boolean enabled = true;
    private String defaultStorage = "default";
    private StorageType type = StorageType.LOCAL;
    private StorageLocalProperties local = new StorageLocalProperties();
    private StorageMinioProperties minio = new StorageMinioProperties();
    private StorageS3Properties s3 = new StorageS3Properties();
    private StorageOssProperties oss = new StorageOssProperties();
}
