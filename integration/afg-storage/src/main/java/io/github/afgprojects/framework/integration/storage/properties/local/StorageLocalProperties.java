package io.github.afgprojects.framework.integration.storage.properties.local;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 本地存储配置。
 */
@Data
public class StorageLocalProperties {

    private String rootDir = "./storage";
    private String bucket = "default";
    private @Nullable String baseUrl;
}
