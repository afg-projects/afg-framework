package io.github.afgprojects.framework.integration.storage.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.FileStorageFactory;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.integration.storage.local.LocalFileStorage;
import io.github.afgprojects.framework.integration.storage.minio.MinioClientBuilder;
import io.github.afgprojects.framework.integration.storage.oss.OssClientBuilder;
import io.github.afgprojects.framework.integration.storage.s3.S3ClientBuilder;

/**
 * 存储自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "afg.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageAutoConfiguration.class);

    /**
     * 创建文件存储工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public FileStorageFactory fileStorageFactory(StorageProperties properties) {
        DefaultFileStorageFactory factory = new DefaultFileStorageFactory(properties.getDefaultStorage());

        // 根据配置类型创建默认存储
        StorageType type = properties.getType();
        FileStorage storage = createStorage(type, properties);

        if (storage != null) {
            factory.register(properties.getDefaultStorage(), storage);
        }

        // 额外注册其他启用的存储
        registerAdditionalStorages(factory, properties);

        return factory;
    }

    /**
     * 创建默认文件存储实例
     */
    @Bean
    @ConditionalOnMissingBean
    public FileStorage fileStorage(FileStorageFactory factory) {
        return factory.getDefaultStorage();
    }

    /**
     * 根据类型创建存储实例
     */
    private FileStorage createStorage(@NonNull StorageType type, @NonNull StorageProperties properties) {
        return switch (type) {
            case LOCAL -> createLocalStorage(properties);
            case MINIO -> createMinioStorage(properties);
            case S3 -> createS3Storage(properties);
            case OSS -> createOssStorage(properties);
        };
    }

    /**
     * 创建本地存储
     */
    private FileStorage createLocalStorage(StorageProperties properties) {
        StorageProperties.LocalConfig config = properties.getLocal();
        log.info("Creating local file storage: rootDir={}, bucket={}", config.getRootDir(), config.getBucket());
        return new LocalFileStorage(config.getBucket(), config.getRootDir(), config.getBaseUrl());
    }

    /**
     * 创建 MinIO 存储
     */
    private FileStorage createMinioStorage(StorageProperties properties) {
        StorageProperties.MinioConfig config = properties.getMinio();
        if (!config.isEnabled()) {
            log.warn("MinIO storage is not enabled, falling back to local storage");
            return createLocalStorage(properties);
        }

        log.info("Creating MinIO file storage: endpoint={}, bucket={}", config.getEndpoint(), config.getBucket());

        MinioClientBuilder builder = MinioClientBuilder.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .region(config.getRegion())
                .secure(config.isSecure())
                .pathStyleAccess(config.isPathStyleAccess());

        return builder.buildStorage(config.getBucket());
    }

    /**
     * 创建 AWS S3 存储
     */
    private FileStorage createS3Storage(StorageProperties properties) {
        StorageProperties.S3Config config = properties.getS3();
        if (!config.isEnabled()) {
            log.warn("S3 storage is not enabled, falling back to local storage");
            return createLocalStorage(properties);
        }

        log.info("Creating AWS S3 file storage: region={}, bucket={}", config.getRegion(), config.getBucket());

        S3ClientBuilder builder = S3ClientBuilder.builder()
                .region(config.getRegion());

        if (config.getAccessKeyId() != null && config.getSecretAccessKey() != null) {
            builder.credentials(config.getAccessKeyId(), config.getSecretAccessKey());
        } else {
            builder.useDefaultCredentials();
        }

        return builder.buildStorage(config.getBucket());
    }

    /**
     * 创建阿里云 OSS 存储
     */
    private FileStorage createOssStorage(StorageProperties properties) {
        StorageProperties.OssConfig config = properties.getOss();
        if (!config.isEnabled()) {
            log.warn("OSS storage is not enabled, falling back to local storage");
            return createLocalStorage(properties);
        }

        log.info("Creating Aliyun OSS file storage: endpoint={}, bucket={}", config.getEndpoint(), config.getBucket());

        OssClientBuilder builder = OssClientBuilder.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKeyId(), config.getAccessKeySecret());

        if (config.getSecurityToken() != null) {
            builder.securityToken(config.getSecurityToken());
        }

        return builder.buildStorage(config.getBucket());
    }

    /**
     * 注册额外的存储实例
     */
    private void registerAdditionalStorages(DefaultFileStorageFactory factory, StorageProperties properties) {
        // 如果默认存储不是本地存储，但本地存储有配置，则额外注册
        if (properties.getType() != StorageType.LOCAL && properties.getLocal().getBucket() != null) {
            factory.register("local", createLocalStorage(properties));
        }

        // 注册 MinIO 存储（如果启用且不是默认）
        if (properties.getType() != StorageType.MINIO && properties.getMinio().isEnabled()) {
            factory.register("minio", createMinioStorage(properties));
        }

        // 注册 S3 存储（如果启用且不是默认）
        if (properties.getType() != StorageType.S3 && properties.getS3().isEnabled()) {
            factory.register("s3", createS3Storage(properties));
        }

        // 注册 OSS 存储（如果启用且不是默认）
        if (properties.getType() != StorageType.OSS && properties.getOss().isEnabled()) {
            factory.register("oss", createOssStorage(properties));
        }
    }
}
