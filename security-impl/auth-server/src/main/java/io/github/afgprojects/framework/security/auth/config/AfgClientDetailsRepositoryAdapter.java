package io.github.afgprojects.framework.security.auth.config;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import io.github.afgprojects.framework.security.auth.user.AfgClientDetails;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetailsService;

/**
 * AfgClientDetailsService 到 RegisteredClientRepository 的适配器
 *
 * <p>将业务系统提供的 {@link AfgClientDetailsService} 适配为
 * Spring Authorization Server 需要的 {@link RegisteredClientRepository}
 *
 * @since 1.0.0
 */
class AfgClientDetailsRepositoryAdapter implements RegisteredClientRepository {

    private final AfgClientDetailsService clientDetailsService;

    AfgClientDetailsRepositoryAdapter(AfgClientDetailsService clientDetailsService) {
        this.clientDetailsService = clientDetailsService;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        // 只读实现，不支持保存
        throw new UnsupportedOperationException(
                "AfgClientDetailsRepositoryAdapter is read-only. " +
                        "Please implement your own RegisteredClientRepository for write operations.");
    }

    @Override
    @Nullable
    public RegisteredClient findById(String id) {
        // 简化实现：使用 clientId 作为 id 查询
        return findByClientId(id);
    }

    @Override
    @Nullable
    public RegisteredClient findByClientId(String clientId) {
        AfgClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
        return clientDetails != null ? clientDetails.toRegisteredClient() : null;
    }
}