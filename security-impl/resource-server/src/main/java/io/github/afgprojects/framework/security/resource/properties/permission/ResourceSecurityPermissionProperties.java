package io.github.afgprojects.framework.security.resource.properties.permission;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 权限校验配置。
 */
@Data
public class ResourceSecurityPermissionProperties {

    /**
     * 认证服务器地址。
     * 配置后将启用远程权限校验。
     */
    @Nullable
    private String authServerUrl;

    /**
     * 服务间调用密钥标识。
     * 用于签名验证，需与认证服务器配置的密钥标识一致。
     */
    @Nullable
    private String keyId;

    /**
     * 服务间调用密钥。
     * 用于生成签名，需与认证服务器配置的密钥一致。
     */
    @Nullable
    private String secret;
}
