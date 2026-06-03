package io.github.afgprojects.framework.security.auth.properties.oauth2;

import java.time.Duration;
import java.util.Set;

import lombok.Data;
import org.jspecify.annotations.NonNull;

/**
 * OAuth2 配置。
 */
@Data
public class OAuth2Config {

    /**
     * 是否启用 OAuth2 授权服务器。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * 授权码有效期。
     * 默认 5 分钟。
     */
    private Duration authorizationCodeTtl = Duration.ofMinutes(5);

    /**
     * 预配置的客户端列表。
     */
    private Set<OAuth2ClientConfig> clients = Set.of();
}
