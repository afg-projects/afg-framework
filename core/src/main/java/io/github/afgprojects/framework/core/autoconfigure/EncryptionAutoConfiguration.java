package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AesConfigEncryptor;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.config.ConfigEncryptor;

/**
 * 配置加密自动配置类
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@EnableConfigurationProperties(AfgCoreProperties.class)
@ConditionalOnProperty(prefix = "afg.core.encryption", name = "enabled", havingValue = "true", matchIfMissing = false)
public class EncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigEncryptor configEncryptor(AfgCoreProperties properties) {
        String secretKey = properties.getEncryption().getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Encryption is enabled but no secret key is configured. "
                    + "Please set afg.core.encryption.secret-key or ENCRYPTION_KEY environment variable.");
        }
        return new AesConfigEncryptor(secretKey);
    }
}
