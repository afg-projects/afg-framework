package io.github.afgprojects.framework.integration.storage.s3;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 客户端构建器
 */
public class S3ClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(S3ClientBuilder.class);

    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private boolean useDefaultCredentials = true;

    public S3ClientBuilder region(@NonNull String region) {
        this.region = region;
        return this;
    }

    public S3ClientBuilder credentials(@NonNull String accessKeyId, @NonNull String secretAccessKey) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.useDefaultCredentials = false;
        return this;
    }

    public S3ClientBuilder useDefaultCredentials() {
        this.useDefaultCredentials = true;
        return this;
    }

    /**
     * 构建 S3 客户端
     */
    @NonNull
    public S3Client buildClient() {
        Region awsRegion = region != null ? Region.of(region) : Region.US_EAST_1;

        var builder = S3Client.builder().region(awsRegion);

        if (!useDefaultCredentials && accessKeyId != null && secretAccessKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        S3Client client = builder.build();
        log.info("AWS S3 client created: region={}", awsRegion);
        return client;
    }

    /**
     * 构建预签名器
     */
    @NonNull
    public S3Presigner buildPresigner() {
        Region awsRegion = region != null ? Region.of(region) : Region.US_EAST_1;

        S3Presigner.Builder builder = S3Presigner.builder().region(awsRegion);

        if (!useDefaultCredentials && accessKeyId != null && secretAccessKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        S3Presigner presigner = builder.build();
        log.info("AWS S3 presigner created: region={}", awsRegion);
        return presigner;
    }

    /**
     * 创建文件存储实例
     */
    @NonNull
    public FileStorage buildStorage(@NonNull String bucket) {
        S3Client client = buildClient();
        S3Presigner presigner = buildPresigner();
        return new S3FileStorage(bucket, client, presigner, region);
    }

    /**
     * 创建构建器
     */
    @NonNull
    public static S3ClientBuilder builder() {
        return new S3ClientBuilder();
    }
}
