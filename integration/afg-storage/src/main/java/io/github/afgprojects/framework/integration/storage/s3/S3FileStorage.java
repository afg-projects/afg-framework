package io.github.afgprojects.framework.integration.storage.s3;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.PresignedUrlOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;
import io.github.afgprojects.framework.integration.storage.model.StorageErrorCode;
import io.github.afgprojects.framework.integration.storage.model.StorageException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * AWS S3 文件存储实现
 */
public class S3FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorage.class);

    private final String bucket;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String region;

    /**
     * 构造 S3 存储
     *
     * @param bucket    存储桶名称
     * @param s3Client  S3 客户端
     * @param presigner 预签名器
     * @param region    AWS 区域
     */
    public S3FileStorage(@NonNull String bucket, @NonNull S3Client s3Client,
                         @Nullable S3Presigner presigner, @Nullable String region) {
        this.bucket = bucket;
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.region = region != null ? region : "us-east-1";

        log.info("AWS S3 storage initialized: bucket={}, region={}", bucket, this.region);
    }

    @Override
    @NonNull
    public StorageType getStorageType() {
        return StorageType.S3;
    }

    @Override
    @NonNull
    public String getBucket() {
        return bucket;
    }

    @Override
    @NonNull
    public StorageObject upload(@NonNull UploadRequest request) {
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(request.key())
                    .contentLength(request.size() > 0 ? request.size() : null);

            if (request.contentType() != null) {
                requestBuilder.contentType(request.contentType());
            }

            if (request.metadata() != null && !request.metadata().isEmpty()) {
                requestBuilder.metadata(request.metadata().getAll());
            }

            PutObjectRequest putRequest = requestBuilder.build();
            RequestBody requestBody = request.size() > 0
                    ? RequestBody.fromInputStream(request.inputStream(), request.size())
                    : RequestBody.fromInputStream(request.inputStream(), Long.MAX_VALUE);

            s3Client.putObject(putRequest, requestBody);

            log.debug("File uploaded to S3: {}", request.key());
            return get(request.key());
        } catch (Exception e) {
            throw StorageException.uploadFailed(request.key(), e);
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("PMD.CloseResource") // InputStream 由调用者通过 DownloadResult.close() 关闭
    public DownloadResult download(@NonNull String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            // 只调用一次 getObject，复用响应对象避免资源泄漏
            var responseInputStream = s3Client.getObject(getRequest);
            GetObjectResponse response = responseInputStream.response();
            InputStream inputStream = responseInputStream;

            return new DownloadResult(
                    inputStream,
                    response.contentLength(),
                    response.contentType(),
                    response.eTag()
            );
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw StorageException.fileNotFound(key, e);
        } catch (Exception e) {
            throw StorageException.downloadFailed(key, e);
        }
    }

    @Override
    public boolean delete(@NonNull String key) {
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
            log.debug("File deleted from S3: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", key, e);
            return false;
        }
    }

    @Override
    public int deleteBatch(@NonNull Iterable<String> keys) {
        List<ObjectIdentifier> identifiers = new ArrayList<>();
        for (String key : keys) {
            identifiers.add(ObjectIdentifier.builder().key(key).build());
        }

        if (identifiers.isEmpty()) {
            return 0;
        }

        try {
            Delete delete = Delete.builder().objects(identifiers).build();
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete)
                    .build();

            var response = s3Client.deleteObjects(request);
            return response.deleted().size();
        } catch (Exception e) {
            log.error("Failed to batch delete files from S3", e);
            return 0;
        }
    }

    @Override
    public boolean exists(@NonNull String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check file existence in S3: {}", key, e);
            return false;
        }
    }

    @Override
    @Nullable
    public StorageObject get(@NonNull String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            HeadObjectResponse response = s3Client.headObject(request);

            return new StorageObject(
                    key,
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified(),
                    convertMetadata(response.metadata())
            );
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            log.error("Failed to get file info from S3: {}", key, e);
            return null;
        }
    }

    @Override
    @NonNull
    public ListResult list(@NonNull ListOptions options) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .maxKeys(options.maxKeys());

            if (options.prefix() != null) {
                requestBuilder.prefix(options.prefix());
            }
            if (options.delimiter() != null) {
                requestBuilder.delimiter(options.delimiter());
            }
            if (options.marker() != null) {
                requestBuilder.continuationToken(options.marker());
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<StorageObject> objects = response.contents().stream()
                    .map(this::convertS3Object)
                    .toList();

            List<String> commonPrefixes = response.commonPrefixes().stream()
                    .map(prefix -> prefix.prefix())
                    .toList();

            return new ListResult(
                    objects,
                    commonPrefixes,
                    response.isTruncated(),
                    response.nextContinuationToken()
            );
        } catch (Exception e) {
            log.error("Failed to list files from S3", e);
            return ListResult.empty();
        }
    }

    @Override
    @NonNull
    public String getUrl(@NonNull String key) {
        if (!exists(key)) {
            throw StorageException.fileNotFound(key);
        }
        // 返回 S3 标准格式 URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    @Override
    @NonNull
    public String getPresignedUrl(@NonNull String key, @NonNull PresignedUrlOptions options) {
        if (presigner == null) {
            throw new StorageException(StorageErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "预签名器未配置");
        }

        try {
            if ("GET".equalsIgnoreCase(options.method())) {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(java.time.Duration.between(Instant.now(), options.expiration()))
                        .getObjectRequest(getRequest)
                        .build();

                PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
                return presigned.url().toString();
            } else if ("PUT".equalsIgnoreCase(options.method())) {
                PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key);

                if (options.contentType() != null) {
                    putBuilder.contentType(options.contentType());
                }

                PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                        .signatureDuration(java.time.Duration.between(Instant.now(), options.expiration()))
                        .putObjectRequest(putBuilder.build())
                        .build();

                PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
                return presigned.url().toString();
            } else {
                throw new StorageException(StorageErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                        "不支持的方法: " + options.method());
            }
        } catch (Exception e) {
            throw new StorageException(StorageErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "生成预签名URL失败: " + key, e);
        }
    }

    @Override
    @NonNull
    public StorageObject updateMetadata(@NonNull String key, @NonNull StorageMetadata metadata) {
        try {
            s3Client.copyObject(builder -> builder
                    .sourceBucket(bucket)
                    .sourceKey(key)
                    .destinationBucket(bucket)
                    .destinationKey(key)
                    .metadata(metadata.getAll()));

            return get(key);
        } catch (Exception e) {
            throw new StorageException(StorageErrorCode.METADATA_UPDATE_FAILED,
                    "更新元数据失败: " + key, e);
        }
    }

    @Override
    @NonNull
    public StorageObject copy(@NonNull String sourceKey, @NonNull String targetKey) {
        try {
            s3Client.copyObject(builder -> builder
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(targetKey));

            log.debug("File copied in S3: {} -> {}", sourceKey, targetKey);
            return get(targetKey);
        } catch (Exception e) {
            throw new StorageException(StorageErrorCode.FILE_COPY_FAILED,
                    "文件复制失败: " + sourceKey + " -> " + targetKey, e);
        }
    }

    private StorageObject convertS3Object(S3Object s3Object) {
        return new StorageObject(
                s3Object.key(),
                s3Object.size(),
                null,
                s3Object.eTag(),
                s3Object.lastModified(),
                null
        );
    }

    @Nullable
    private StorageMetadata convertMetadata(@Nullable Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return new StorageMetadata(metadata);
    }
}
