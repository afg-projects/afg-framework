package io.github.afgprojects.framework.integration.storage.oss;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import io.github.afgprojects.framework.core.api.storage.FileStorage;

/**
 * 阿里云 OSS 客户端构建器
 */
public class OssClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(OssClientBuilder.class);

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;

    public OssClientBuilder endpoint(@NonNull String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public OssClientBuilder credentials(@NonNull String accessKeyId, @NonNull String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    public OssClientBuilder securityToken(@Nullable String securityToken) {
        this.securityToken = securityToken;
        return this;
    }

    /**
     * 构建 OSS 客户端
     */
    @NonNull
    public OSS buildClient() {
        validate();

        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);
        log.info("Aliyun OSS client created: endpoint={}", endpoint);
        return client;
    }

    /**
     * 创建文件存储实例
     */
    @NonNull
    public FileStorage buildStorage(@NonNull String bucket) {
        OSS client = buildClient();
        return new OssFileStorage(bucket, client, endpoint);
    }

    private void validate() {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            throw new IllegalArgumentException("accessKeyId is required");
        }
        if (accessKeySecret == null || accessKeySecret.isEmpty()) {
            throw new IllegalArgumentException("accessKeySecret is required");
        }
    }

    /**
     * 创建构建器
     */
    @NonNull
    public static OssClientBuilder builder() {
        return new OssClientBuilder();
    }
}
