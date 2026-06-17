package io.github.afgprojects.framework.security.auth.app.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.app.entity.AuthClient;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 JDBC 的 OAuth2 客户端服务实现。
 *
 * <p>使用 {@link DataManager} 操作 {@code auth_client} 表，替代
 * {@link io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService}。
 * 客户端数据持久化到数据库，重启后不丢失。
 *
 * <p>停用客户端（{@code status=0}）在 {@link #loadClientByClientId} 中返回 null，
 * 从而拒绝 OAuth2 授权请求。
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcOAuth2ClientService implements OAuth2ClientService {

    private final DataManager dataManager;

    public JdbcOAuth2ClientService(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    @Nullable
    public ClientDetails loadClientByClientId(@NonNull String clientId) {
        var client = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientId);
        if (client.isEmpty()) {
            log.debug("Client not found: clientId={}", clientId);
            return null;
        }
        AuthClient authClient = client.get();
        if (!authClient.isEnabled()) {
            log.debug("Client is disabled: clientId={}", clientId);
            return null;
        }
        return toClientDetails(authClient);
    }

    @Override
    public boolean validateClientCredentials(@NonNull String clientId, @Nullable String clientSecret) {
        var client = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientId);
        if (client.isEmpty()) {
            log.debug("Client not found: clientId={}", clientId);
            return false;
        }

        ClientDetails details = toClientDetails(client.get());

        // 公共客户端不需要密钥验证
        if (details.getClientType() == ClientDetails.ClientType.PUBLIC) {
            log.debug("Public client does not require secret: clientId={}", clientId);
            return true;
        }

        // 机密客户端需要验证密钥
        if (clientSecret == null || clientSecret.isEmpty()) {
            log.debug("Client secret is required for confidential client: clientId={}", clientId);
            return false;
        }

        boolean valid = details.clientSecret().equals(clientSecret);
        if (!valid) {
            log.debug("Invalid client secret: clientId={}", clientId);
        }
        return valid;
    }

    @Override
    public boolean requiresPkce(@NonNull String clientId) {
        var client = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientId);
        if (client.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(client.get().getRequirePkce());
    }

    @Override
    public void saveClient(@NonNull ClientDetails clientDetails) {
        var existing = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientDetails.clientId());
        AuthClient authClient;
        if (existing.isPresent()) {
            authClient = existing.get();
            updateFromClientDetails(authClient, clientDetails);
        } else {
            authClient = toAuthClient(clientDetails);
        }
        dataManager.save(AuthClient.class, authClient);
        log.info("Saved OAuth2 client: clientId={}, name={}", clientDetails.clientId(), clientDetails.clientName());
    }

    @Override
    public void deleteClient(@NonNull String clientId) {
        var existing = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientId);
        if (existing.isPresent()) {
            dataManager.deleteById(AuthClient.class, existing.get().getId());
            log.info("Deleted OAuth2 client: clientId={}", clientId);
        }
    }

    /**
     * 获取所有客户端列表（含停用的）。
     *
     * @return 客户端详情列表
     */
    public List<ClientDetails> findAll() {
        return dataManager.findAll(AuthClient.class).stream()
                .map(this::toClientDetails)
                .collect(Collectors.toList());
    }

    /**
     * 获取启用的客户端列表。
     *
     * @return 启用的客户端详情列表
     */
    public List<ClientDetails> findAllEnabled() {
        return dataManager.findList(AuthClient.class,
                        Conditions.builder(AuthClient.class)
                                .eq(AuthClient::getStatus, 1)
                                .build()).stream()
                .map(this::toClientDetails)
                .collect(Collectors.toList());
    }

    /**
     * 从 YAML 配置初始化种子数据到数据库。
     * 仅在数据库为空时执行。
     *
     * @param seedClients 种子客户端列表
     */
    public void initSeedDataIfEmpty(@NonNull Iterable<ClientDetails> seedClients) {
        long count = dataManager.findAll(AuthClient.class).size();
        if (count > 0) {
            log.debug("auth_client table already has {} records, skipping seed data", count);
            return;
        }
        log.info("Initializing auth_client seed data...");
        for (ClientDetails client : seedClients) {
            saveClient(client);
        }
        log.info("Initialized {} seed clients", dataManager.findAll(AuthClient.class).size());
    }

    // ========== 实体与模型转换 ==========

    /**
     * 将 {@link AuthClient} 实体转换为 {@link ClientDetails} record。
     */
    ClientDetails toClientDetails(@NonNull AuthClient authClient) {
        return new ClientDetails(
                authClient.getClientId(),
                authClient.getClientSecret(),
                authClient.getClientName(),
                parseSet(authClient.getRedirectUris()),
                parseSet(authClient.getScopes()),
                parseSet(authClient.getGrantTypes()),
                Boolean.TRUE.equals(authClient.getRequirePkce()),
                Duration.ofSeconds(authClient.getAccessTokenTtl() != null ? authClient.getAccessTokenTtl() : 7200),
                Duration.ofSeconds(authClient.getRefreshTokenTtl() != null ? authClient.getRefreshTokenTtl() : 604800)
        );
    }

    /**
     * 将 {@link ClientDetails} record 转换为新的 {@link AuthClient} 实体。
     */
    AuthClient toAuthClient(@NonNull ClientDetails details) {
        AuthClient authClient = new AuthClient();
        updateFromClientDetails(authClient, details);
        return authClient;
    }

    /**
     * 用 {@link ClientDetails} 的数据更新已有 {@link AuthClient} 实体。
     */
    void updateFromClientDetails(@NonNull AuthClient authClient, @NonNull ClientDetails details) {
        authClient.setClientId(details.clientId());
        authClient.setClientSecret(details.clientSecret());
        authClient.setClientName(details.clientName());
        authClient.setRedirectUris(joinSet(details.redirectUris()));
        authClient.setScopes(joinSet(details.scopes()));
        authClient.setGrantTypes(joinSet(details.grantTypes()));
        authClient.setRequirePkce(details.requirePkce());
        authClient.setAccessTokenTtl(details.accessTokenTtl().getSeconds());
        authClient.setRefreshTokenTtl(details.refreshTokenTtl().getSeconds());
    }

    /**
     * 将逗号分隔字符串解析为 Set。
     */
    Set<String> parseSet(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 将 Set 合并为逗号分隔字符串。
     */
    @Nullable
    String joinSet(@NonNull Set<String> set) {
        if (set.isEmpty()) {
            return null;
        }
        return String.join(",", set);
    }
}
