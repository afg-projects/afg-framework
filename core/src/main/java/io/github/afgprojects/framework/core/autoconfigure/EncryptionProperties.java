package io.github.afgprojects.framework.core.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 加密配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.encryption")
public class EncryptionProperties {

    /**
     * 是否启用配置加密
     */
    private boolean enabled;

    /**
     * 加密算法
     */
    private String algorithm = "AES-256-GCM";

    /**
     * 加密密钥
     * 推荐从环境变量读取: ${ENCRYPTION_KEY}
     */
    private @Nullable String secretKey;

    /**
     * 加密值前缀
     */
    private String prefix = "ENC(";

    /**
     * 加密值后缀
     */
    private String suffix = ")";
}
