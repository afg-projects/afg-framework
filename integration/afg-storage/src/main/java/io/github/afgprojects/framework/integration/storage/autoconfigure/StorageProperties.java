package io.github.afgprojects.framework.integration.storage.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.afgprojects.framework.core.api.storage.model.StorageType;

/**
 * 存储配置属性
 */
@ConfigurationProperties(prefix = "afg.storage")
public class StorageProperties {

    /**
     * 是否启用存储功能
     */
    private boolean enabled = true;

    /**
     * 默认存储名称
     */
    private String defaultStorage = "default";

    /**
     * 存储类型
     */
    private StorageType type = StorageType.LOCAL;

    /**
     * 本地存储配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * MinIO 存储配置
     */
    private MinioConfig minio = new MinioConfig();

    /**
     * AWS S3 存储配置
     */
    private S3Config s3 = new S3Config();

    /**
     * 阿里云 OSS 存储配置
     */
    private OssConfig oss = new OssConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultStorage() {
        return defaultStorage;
    }

    public void setDefaultStorage(String defaultStorage) {
        this.defaultStorage = defaultStorage;
    }

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
    }

    public LocalConfig getLocal() {
        return local;
    }

    public void setLocal(LocalConfig local) {
        this.local = local;
    }

    public MinioConfig getMinio() {
        return minio;
    }

    public void setMinio(MinioConfig minio) {
        this.minio = minio;
    }

    public S3Config getS3() {
        return s3;
    }

    public void setS3(S3Config s3) {
        this.s3 = s3;
    }

    public OssConfig getOss() {
        return oss;
    }

    public void setOss(OssConfig oss) {
        this.oss = oss;
    }

    /**
     * 本地存储配置
     */
    public static class LocalConfig {

        /**
         * 根目录路径
         */
        private String rootDir = "./storage";

        /**
         * 存储桶名称（用作子目录）
         */
        private String bucket = "default";

        /**
         * 基础 URL（用于生成访问 URL）
         */
        private String baseUrl;

        public String getRootDir() {
            return rootDir;
        }

        public void setRootDir(String rootDir) {
            this.rootDir = rootDir;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        @Nullable
        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * MinIO 存储配置
     */
    public static class MinioConfig {

        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * MinIO endpoint
         */
        private String endpoint;

        /**
         * Access Key
         */
        private String accessKey;

        /**
         * Secret Key
         */
        private String secretKey;

        /**
         * Region
         */
        private String region = "us-east-1";

        /**
         * 存储桶名称
         */
        private String bucket;

        /**
         * 是否使用 HTTPS
         */
        private boolean secure = false;

        /**
         * 是否使用路径样式访问
         */
        private boolean pathStyleAccess = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }

    /**
     * AWS S3 存储配置
     */
    public static class S3Config {

        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * AWS Region
         */
        private String region = "us-east-1";

        /**
         * Access Key ID（可选，使用默认凭证链）
         */
        private String accessKeyId;

        /**
         * Secret Access Key（可选，使用默认凭证链）
         */
        private String secretAccessKey;

        /**
         * 存储桶名称
         */
        private String bucket;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        @Nullable
        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        @Nullable
        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    /**
     * 阿里云 OSS 存储配置
     */
    public static class OssConfig {

        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * OSS endpoint
         */
        private String endpoint;

        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Access Key Secret
         */
        private String accessKeySecret;

        /**
         * Security Token（可选，用于临时凭证）
         */
        private String securityToken;

        /**
         * 存储桶名称
         */
        private String bucket;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        @Nullable
        public String getSecurityToken() {
            return securityToken;
        }

        public void setSecurityToken(String securityToken) {
            this.securityToken = securityToken;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}