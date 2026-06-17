package io.github.afgprojects.framework.security.auth.app.controller;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.security.auth.app.service.JdbcOAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * OAuth2 客户端管理 API。
 *
 * <p>提供 OAuth2 客户端的独立 CRUD 操作。通常通过
 * {@link AppController} 的应用管理接口间接操作客户端，
 * 本接口用于高级场景（如独立管理机器对机器的 client_credentials 客户端）。
 *
 * <p>通过 {@code @Bean} 注册而非组件扫描，路径前缀需包含 {@code /auth-api}。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/oauth2-clients")
public class OAuth2ClientController {

    private final JdbcOAuth2ClientService clientService;

    public OAuth2ClientController(JdbcOAuth2ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * 获取客户端列表。
     */
    @GetMapping
    public Result<List<ClientDetails>> list() {
        return Result.success(clientService.findAll());
    }

    /**
     * 获取客户端详情。
     */
    @GetMapping("/{clientId}")
    public Result<ClientDetails> getByClientId(@PathVariable String clientId) {
        ClientDetails details = clientService.loadClientByClientId(clientId);
        return Result.success(details);
    }

    /**
     * 创建客户端。
     */
    @PostMapping
    public Result<ClientDetails> create(@RequestBody OAuth2ClientCreateRequest request) {
        ClientDetails clientDetails = request.toClientDetails();
        clientService.saveClient(clientDetails);
        return Result.success(clientDetails);
    }

    /**
     * 更新客户端。
     */
    @PutMapping("/{clientId}")
    public Result<ClientDetails> update(@PathVariable String clientId, @RequestBody OAuth2ClientCreateRequest request) {
        ClientDetails clientDetails = request.toClientDetails();
        clientService.saveClient(clientDetails);
        return Result.success(clientDetails);
    }

    /**
     * 删除客户端。
     */
    @DeleteMapping("/{clientId}")
    public Result<Boolean> delete(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return Result.success(true);
    }

    /**
     * OAuth2 客户端创建/更新请求 DTO。
     *
     * <p>使用 Long 类型的 TTL 值（秒），避免 Duration 类型的 Jackson 反序列化问题。
     */
    public record OAuth2ClientCreateRequest(
            String clientId,
            String clientSecret,
            String clientName,
            Set<String> redirectUris,
            Set<String> scopes,
            Set<String> grantTypes,
            Boolean requirePkce,
            Long accessTokenTtl,
            Long refreshTokenTtl
    ) {
        public ClientDetails toClientDetails() {
            return new ClientDetails(
                    clientId,
                    clientSecret,
                    clientName,
                    redirectUris != null ? redirectUris : Set.of(),
                    scopes != null ? scopes : Set.of(),
                    grantTypes != null ? grantTypes : Set.of(),
                    requirePkce != null ? requirePkce : false,
                    accessTokenTtl != null ? Duration.ofSeconds(accessTokenTtl) : Duration.ofHours(2),
                    refreshTokenTtl != null ? Duration.ofSeconds(refreshTokenTtl) : Duration.ofDays(7)
            );
        }
    }
}
