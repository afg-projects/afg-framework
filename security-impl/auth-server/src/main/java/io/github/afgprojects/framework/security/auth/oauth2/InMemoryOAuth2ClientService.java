package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 OAuth2 客户端服务实现。
 *
 * <p>适用于单机部署和测试环境，使用 ConcurrentHashMap 存储客户端信息。
 * 生产环境应使用 JDBC 或其他持久化存储实现。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * InMemoryOAuth2ClientService clientService = new InMemoryOAuth2ClientService();
 *
 * // 注册客户端
 * ClientDetails client = new ClientDetails(
 *     "client-001", "secret-123", "Test Client",
 *     Set.of("https://example.com/callback"),
 *     Set.of("read", "write"),
 *     Set.of("authorization_code", "refresh_token"),
 *     false, Duration.ofHours(1), Duration.ofDays(7)
 * );
 * clientService.saveClient(client);
 *
 * // 验证客户端
 * boolean valid = clientService.validateClientCredentials("client-001", "secret-123");
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class InMemoryOAuth2ClientService implements OAuth2ClientService {

    private final Map<String, ClientDetails> clients = new ConcurrentHashMap<>();

    /**
     * 默认构造函数。
     */
    public InMemoryOAuth2ClientService() {
    }

    /**
     * 使用初始客户端列表构造。
     *
     * @param initialClients 初始客户端列表
     */
    public InMemoryOAuth2ClientService(@NonNull Iterable<ClientDetails> initialClients) {
        for (ClientDetails client : initialClients) {
            clients.put(client.clientId(), client);
        }
    }

    @Override
    @Nullable
    public ClientDetails loadClientByClientId(@NonNull String clientId) {
        return clients.get(clientId);
    }

    @Override
    public boolean validateClientCredentials(@NonNull String clientId, @Nullable String clientSecret) {
        ClientDetails client = clients.get(clientId);
        if (client == null) {
            log.debug("Client not found: clientId={}", clientId);
            return false;
        }

        // 公共客户端不需要密钥验证
        if (client.getClientType() == ClientDetails.ClientType.PUBLIC) {
            log.debug("Public client does not require secret: clientId={}", clientId);
            return true;
        }

        // 机密客户端需要验证密钥
        if (clientSecret == null || clientSecret.isEmpty()) {
            log.debug("Client secret is required for confidential client: clientId={}", clientId);
            return false;
        }

        boolean valid = client.clientSecret().equals(clientSecret);
        if (!valid) {
            log.debug("Invalid client secret: clientId={}", clientId);
        }
        return valid;
    }

    @Override
    public boolean requiresPkce(@NonNull String clientId) {
        ClientDetails client = clients.get(clientId);
        if (client == null) {
            return false;
        }
        return client.requirePkce();
    }

    @Override
    public void saveClient(@NonNull ClientDetails clientDetails) {
        clients.put(clientDetails.clientId(), clientDetails);
        log.info("Saved OAuth2 client: clientId={}, name={}", clientDetails.clientId(), clientDetails.clientName());
    }

    @Override
    public void deleteClient(@NonNull String clientId) {
        ClientDetails removed = clients.remove(clientId);
        if (removed != null) {
            log.info("Deleted OAuth2 client: clientId={}", clientId);
        }
    }

    /**
     * 检查客户端是否存在。
     *
     * @param clientId 客户端 ID
     * @return 如果存在返回 true
     */
    public boolean exists(@NonNull String clientId) {
        return clients.containsKey(clientId);
    }

    /**
     * 获取客户端数量。
     *
     * @return 客户端数量
     */
    public int size() {
        return clients.size();
    }

    /**
     * 清空所有客户端（用于测试）。
     */
    public void clear() {
        clients.clear();
    }

    /**
     * 创建预配置的测试客户端。
     *
     * @return 测试客户端服务实例
     */
    public static InMemoryOAuth2ClientService createTestInstance() {
        InMemoryOAuth2ClientService service = new InMemoryOAuth2ClientService();

        // 添加测试用机密客户端
        service.saveClient(new ClientDetails(
                "test-client",
                "test-secret",
                "Test Confidential Client",
                java.util.Set.of("http://localhost:8080/callback"),
                java.util.Set.of("read", "write"),
                java.util.Set.of("authorization_code", "refresh_token", "client_credentials"),
                false,
                Duration.ofHours(1),
                Duration.ofDays(7)
        ));

        // 添加测试用公共客户端（需要 PKCE）
        service.saveClient(new ClientDetails(
                "test-public-client",
                null,
                "Test Public Client",
                java.util.Set.of("http://localhost:3000/callback"),
                java.util.Set.of("read"),
                java.util.Set.of("authorization_code", "refresh_token"),
                true,
                Duration.ofHours(1),
                Duration.ofDays(7)
        ));

        return service;
    }
}