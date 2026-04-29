package io.github.afgprojects.framework.integration.storage.oss;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;

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

/**
 * 阿里云 OSS 文件存储实现
 */
public class OssFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorage.class);

    private final String bucket;
    private final OSS ossClient;
    private final String endpoint;

    /**
     * 构造 OSS 存储
     *
     * @param bucket    存储桶名称
     * @param ossClient OSS 客户端
     * @param endpoint  OSS endpoint
     */
    public OssFileStorage(@NonNull String bucket, @NonNull OSS ossClient, @NonNull String endpoint) {
        this.bucket = bucket;
        this.ossClient = ossClient;
        this.endpoint = endpoint;

        log.info("Aliyun OSS storage initialized: bucket={}, endpoint={}", bucket, endpoint);
    }

    @Override
    @NonNull
    public StorageType getStorageType() {
        return StorageType.OSS;
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
            ObjectMetadata metadata = new ObjectMetadata();
            if (request.size() > 0) {
                metadata.setContentLength(request.size());
            }
            if (request.contentType() != null) {
                metadata.setContentType(request.contentType());
            }
            if (request.metadata() != null && !request.metadata().isEmpty()) {
                metadata.setUserMetadata(request.metadata().getAll());
            }

            PutObjectRequest putRequest = new PutObjectRequest(
                    bucket, request.key(), request.inputStream(), metadata);
            ossClient.putObject(putRequest);

            log.debug("File uploaded to OSS: {}", request.key());
            return get(request.key());
        } catch (com.aliyun.oss.OSSException e) {
            throw StorageException.uploadFailed(request.key(), e);
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("PMD.CloseResource") // OSSObject 输入流由 DownloadResult 管理
    public DownloadResult download(@NonNull String key) {
        try {
            GetObjectRequest getRequest = new GetObjectRequest(bucket, key);
            OSSObject ossObject = ossClient.getObject(getRequest);
            // OSSObject 的输入流会在 DownloadResult.close() 中关闭
            return new DownloadResult(
                    ossObject.getObjectContent(),
                    ossObject.getObjectMetadata().getContentLength(),
                    ossObject.getObjectMetadata().getContentType(),
                    ossObject.getObjectMetadata().getETag()
            );
        } catch (com.aliyun.oss.OSSException e) {
            if (e.getErrorCode() != null && e.getErrorCode().contains("NoSuchKey")) {
                throw StorageException.fileNotFound(key, e);
            }
            throw StorageException.downloadFailed(key, e);
        }
    }

    @Override
    public boolean delete(@NonNull String key) {
        try {
            ossClient.deleteObject(bucket, key);
            log.debug("File deleted from OSS: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from OSS: {}", key, e);
            return false;
        }
    }

    @Override
    public int deleteBatch(@NonNull Iterable<String> keys) {
        List<String> keyList = new ArrayList<>();
        keys.forEach(keyList::add);

        if (keyList.isEmpty()) {
            return 0;
        }

        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(keyList);
            DeleteObjectsResult result = ossClient.deleteObjects(request);
            return result.getDeletedObjects().size();
        } catch (Exception e) {
            log.error("Failed to batch delete files from OSS", e);
            return 0;
        }
    }

    @Override
    public boolean exists(@NonNull String key) {
        try {
            return ossClient.doesObjectExist(bucket, key);
        } catch (Exception e) {
            log.error("Failed to check file existence in OSS: {}", key, e);
            return false;
        }
    }

    @Override
    @Nullable
    public StorageObject get(@NonNull String key) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucket, key);

            return new StorageObject(
                    key,
                    metadata.getContentLength(),
                    metadata.getContentType(),
                    metadata.getETag(),
                    metadata.getLastModified() != null
                            ? metadata.getLastModified().toInstant()
                            : Instant.now(),
                    convertMetadata(metadata.getUserMetadata())
            );
        } catch (com.aliyun.oss.OSSException e) {
            if (e.getErrorCode() != null && e.getErrorCode().contains("NoSuchKey")) {
                return null;
            }
            log.error("Failed to get file info from OSS: {}", key, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to get file info from OSS: {}", key, e);
            return null;
        }
    }

    @Override
    @NonNull
    public ListResult list(@NonNull ListOptions options) {
        try {
            com.aliyun.oss.model.ListObjectsRequest listRequest =
                    new com.aliyun.oss.model.ListObjectsRequest(bucket);

            if (options.prefix() != null) {
                listRequest.setPrefix(options.prefix());
            }
            if (options.delimiter() != null) {
                listRequest.setDelimiter(options.delimiter());
            }
            if (options.marker() != null) {
                listRequest.setMarker(options.marker());
            }
            listRequest.setMaxKeys(options.maxKeys());

            ObjectListing listing = ossClient.listObjects(listRequest);

            List<StorageObject> objects = listing.getObjectSummaries().stream()
                    .map(this::convertOssObject)
                    .toList();

            List<String> commonPrefixes = new ArrayList<>(listing.getCommonPrefixes());

            return new ListResult(
                    objects,
                    commonPrefixes,
                    listing.isTruncated(),
                    listing.getNextMarker()
            );
        } catch (Exception e) {
            log.error("Failed to list files from OSS", e);
            return ListResult.empty();
        }
    }

    @Override
    @NonNull
    public String getUrl(@NonNull String key) {
        if (!exists(key)) {
            throw StorageException.fileNotFound(key);
        }
        // 返回 OSS 标准格式 URL
        return String.format("https://%s.%s/%s", bucket, endpoint, key);
    }

    @Override
    @NonNull
    @SuppressWarnings("PMD.ReplaceJavaUtilDate") // OSS SDK 要求使用 java.util.Date
    public String getPresignedUrl(@NonNull String key, @NonNull PresignedUrlOptions options) {
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);

            // 计算 expiration 时间点
            java.util.Date expirationDate = java.util.Date.from(options.expiration());
            request.setExpiration(expirationDate);

            if ("PUT".equalsIgnoreCase(options.method())) {
                request.setMethod(com.aliyun.oss.HttpMethod.PUT);
                if (options.contentType() != null) {
                    request.setContentType(options.contentType());
                }
            } else {
                request.setMethod(com.aliyun.oss.HttpMethod.GET);
            }

            java.net.URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            throw new StorageException(StorageErrorCode.PRESIGNED_URL_GENERATION_FAILED,
                    "生成预签名URL失败: " + key, e);
        }
    }

    @Override
    @NonNull
    public StorageObject updateMetadata(@NonNull String key, @NonNull StorageMetadata metadata) {
        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucket, key, bucket, key);
            ObjectMetadata newMetadata = new ObjectMetadata();
            newMetadata.setUserMetadata(metadata.getAll());
            copyRequest.setNewObjectMetadata(newMetadata);

            ossClient.copyObject(copyRequest);
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
            ossClient.copyObject(bucket, sourceKey, bucket, targetKey);
            log.debug("File copied in OSS: {} -> {}", sourceKey, targetKey);
            return get(targetKey);
        } catch (Exception e) {
            throw new StorageException(StorageErrorCode.FILE_COPY_FAILED,
                    "文件复制失败: " + sourceKey + " -> " + targetKey, e);
        }
    }

    private StorageObject convertOssObject(OSSObjectSummary summary) {
        return new StorageObject(
                summary.getKey(),
                summary.getSize(),
                null,
                summary.getETag(),
                summary.getLastModified() != null
                        ? summary.getLastModified().toInstant()
                        : Instant.now(),
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
