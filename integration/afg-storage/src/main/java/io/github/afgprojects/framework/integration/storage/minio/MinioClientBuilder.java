package io.github.afgprojects.framework.integration.storage.minio;

import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * MinIO 客户端构建器
 */
public class MinioClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(MinioClientBuilder.class);

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
    private boolean pathStyleAccess = true;
    private boolean secure = false;

    public MinioClientBuilder endpoint(@NonNull String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public MinioClientBuilder credentials(@NonNull String accessKey, @NonNull String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        return this;
    }

    public MinioClientBuilder region(@Nullable String region) {
        if (region != null) {
            this.region = region;
        }
        return this;
    }

    public MinioClientBuilder pathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
        return this;
    }

    public MinioClientBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * 构建 S3 客户端
     */
    @NonNull
    public S3Client buildClient() {
        validate();

        String protocol = secure ? "https://" : "http://";
        String endpointUrl = endpoint.startsWith("http") ? endpoint : protocol + endpoint;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(s3Config)
                .build();

        log.info("MinIO S3 client created: endpoint={}", endpointUrl);
        return client;
    }

    /**
     * 构建预签名器
     */
    @NonNull
    public S3Presigner buildPresigner() {
        validate();

        String protocol = secure ? "https://" : "http://";
        String endpointUrl = endpoint.startsWith("http") ? endpoint : protocol + endpoint;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(s3Config)
                .build();

        log.info("MinIO presigner created: endpoint={}", endpointUrl);
        return presigner;
    }

    /**
     * 创建文件存储实例
     */
    @NonNull
    public FileStorage buildStorage(@NonNull String bucket) {
        S3Client client = buildClient();
        S3Presigner presigner = buildPresigner();
        return new MinioFileStorage(bucket, client, presigner);
    }

    private void validate() {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (accessKey == null || accessKey.isEmpty()) {
            throw new IllegalArgumentException("accessKey is required");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("secretKey is required");
        }
    }

    /**
     * 创建构建器
     */
    @NonNull
    public static MinioClientBuilder builder() {
        return new MinioClientBuilder();
    }
}
