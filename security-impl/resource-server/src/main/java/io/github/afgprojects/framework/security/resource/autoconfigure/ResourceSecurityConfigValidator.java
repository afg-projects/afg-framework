package io.github.afgprojects.framework.security.resource.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 资源服务器配置验证器。
 *
 * <p>在应用启动时验证必填配置项，提供清晰的错误提示。
 *
 * @since 1.1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "afg.security.resource-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourceSecurityConfigValidator implements ApplicationRunner {

    private final ResourceSecurityProperties properties;

    public ResourceSecurityConfigValidator(ResourceSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating resource-server security configuration...");

        boolean hasAuthServer = isAuthServerPresent();

        ResourceSecurityProperties.JwtConfig jwtConfig = properties.getJwt();

        if (!hasAuthServer && jwtConfig.isEnabled()) {
            if ((jwtConfig.getJwkSetUri() == null || jwtConfig.getJwkSetUri().isBlank())
                    && (jwtConfig.getPublicKey() == null || jwtConfig.getPublicKey().isBlank())) {
                throw new IllegalStateException(
                    "资源服务器需要配置 JWT 验证参数。\n" +
                    "请配置 afg.security.resource-server.jwt.jwk-set-uri 或 public-key\n" +
                    "示例配置:\n" +
                    "afg:\n" +
                    "  security:\n" +
                    "    resource-server:\n" +
                    "      jwt:\n" +
                    "        jwk-set-uri: https://auth.example.com/.well-known/jwks.json\n" +
                    "或:\n" +
                    "      jwt:\n" +
                    "        public-key: |\n" +
                    "          -----BEGIN PUBLIC KEY-----\n" +
                    "          ...\n" +
                    "          -----END PUBLIC KEY-----"
                );
            }
        }

        if (hasAuthServer && jwtConfig.isEnabled()) {
            if ((jwtConfig.getJwkSetUri() == null || jwtConfig.getJwkSetUri().isBlank())
                    && (jwtConfig.getPublicKey() == null || jwtConfig.getPublicKey().isBlank())) {
                log.info("单体应用模式：使用 auth-server 的签名密钥验证 JWT");
            }
        }

        log.info("Resource-server configuration validated successfully");
    }

    private boolean isAuthServerPresent() {
        try {
            Class.forName("io.github.afgprojects.framework.security.auth.autoconfigure.AuthorizationServerAutoConfiguration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}