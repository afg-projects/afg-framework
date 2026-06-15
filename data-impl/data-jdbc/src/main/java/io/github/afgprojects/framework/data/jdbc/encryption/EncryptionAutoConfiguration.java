package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.encryption.FieldEncryptionKeyProvider;
import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 字段加密自动配置
 * <p>
 * 注册生产级 {@link AesGcmFieldEncryptor}、{@link ConfigFieldEncryptionKeyProvider}
 * 和 {@link BlindIndexProvider}。
 *
 * <p>该 AutoConfiguration 在 DataManagerAutoConfiguration 之前运行，
 * 确保 AesGcmFieldEncryptor 在 NoOpFieldEncryptor 之前注册为 FieldEncryptor。
 *
 * <h3>配置</h3>
 * <pre>
 * afg:
 *   data:
 *     encryption:
 *       enabled: true
 *       strict-mode: true
 *       default-key: "base64-encoded-32-byte-key"
 *       keys:
 *         user-key: "base64-encoded-32-byte-key"
 * </pre>
 */
@Slf4j
@AutoConfiguration(beforeName = {
    "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration"
})
@ConditionalOnProperty(prefix = "afg.data.encryption", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EncryptionProperties.class)
public class EncryptionAutoConfiguration {

    /**
     * 创建基于配置的密钥提供者
     *
     * @param properties 加密配置属性
     * @return ConfigFieldEncryptionKeyProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(FieldEncryptionKeyProvider.class)
    public ConfigFieldEncryptionKeyProvider configFieldEncryptionKeyProvider(EncryptionProperties properties) {
        log.info("Using ConfigFieldEncryptionKeyProvider for field encryption keys");
        return new ConfigFieldEncryptionKeyProvider(properties);
    }

    /**
     * 创建 AES-GCM 字段加密器（同时实现 FieldEncryptor 和 BlindIndexProvider）
     *
     * @param keyProvider 密钥提供者
     * @return AesGcmFieldEncryptor 实例
     */
    @Bean
    @ConditionalOnMissingBean(FieldEncryptor.class)
    public AesGcmFieldEncryptor aesGcmFieldEncryptor(FieldEncryptionKeyProvider keyProvider) {
        log.info("Using AesGcmFieldEncryptor for field encryption (AES-GCM + HMAC-SHA256 blind index)");
        return new AesGcmFieldEncryptor(keyProvider);
    }

    /**
     * 注册 BlindIndexProvider Bean（从 AesGcmFieldEncryptor 提取）
     * <p>
     * 如果已有自定义 BlindIndexProvider 则不注册。
     *
     * @param encryptor AES-GCM 加密器（同时实现 BlindIndexProvider）
     * @return BlindIndexProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(BlindIndexProvider.class)
    public BlindIndexProvider blindIndexProvider(AesGcmFieldEncryptor encryptor) {
        return encryptor;
    }
}
