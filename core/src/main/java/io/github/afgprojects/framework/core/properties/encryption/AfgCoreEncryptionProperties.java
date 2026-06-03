package io.github.afgprojects.framework.core.properties.encryption;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 加密配置。
 */
@Data
public class AfgCoreEncryptionProperties {

    /**
     * 是否启用配置加密。
     */
    private boolean enabled;

    /**
     * 加密算法。
     */
    private String algorithm = "AES-256-GCM";

    /**
     * 加密密钥。
     */
    private @Nullable String secretKey;

    /**
     * 加密值前缀。
     */
    private String prefix = "ENC(";

    /**
     * 加密值后缀。
     */
    private String suffix = ")";
}
