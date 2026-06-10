package io.github.afgprojects.framework.security.auth.key;

import io.github.afgprojects.framework.core.annotation.IgnoreModuleContextPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWKS 端点控制器。
 *
 * <p>提供 OAuth 2.0 授权服务器元数据端点，用于资源服务器获取公钥验证 JWT 签名。
 *
 * <h3>端点</h3>
 * <ul>
 *   <li>GET /.well-known/jwks.json - 返回 JWK Set</li>
 * </ul>
 *
 * <p>使用 {@link IgnoreModuleContextPath} 排除模块 context-path 前缀，
 * 因为 JWKS 端点的标准路径是 /.well-known/jwks.json，不应带 /auth-api 前缀。
 *
 * <h3>使用示例</h3>
 * <pre>
 * # 获取 JWKS
 * curl https://auth.example.com/.well-known/jwks.json
 * </pre>
 *
 * @since 1.1.0
 */
@Slf4j
@RestController
@IgnoreModuleContextPath
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyPairManager keyPairManager;

    /**
     * 返回 JWK Set。
     *
     * @return JWK Set JSON
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getJwks() {
        log.debug("JWKS requested");
        return keyPairManager.getJwkSetJson();
    }
}
