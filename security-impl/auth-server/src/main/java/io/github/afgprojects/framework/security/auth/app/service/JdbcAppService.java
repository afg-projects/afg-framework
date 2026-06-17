package io.github.afgprojects.framework.security.auth.app.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.app.entity.AuthApp;
import io.github.afgprojects.framework.security.auth.app.entity.AuthClient;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 应用管理服务。
 *
 * <p>使用 {@link DataManager} 操作 {@code auth_app} 表，提供应用的 CRUD、
 * 启停控制和微前端列表查询。创建/删除应用时联动 OAuth2 客户端。
 *
 * <p>应用管理查询需要同时获取全局应用（tenant_id IS NULL）和租户应用，
 * 因此查询时临时清除自动租户过滤，改为 Java 层面按租户过滤。
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcAppService {

    private final DataManager dataManager;
    private final JdbcOAuth2ClientService clientService;
    private final TenantContextHolder tenantContextHolder;

    public JdbcAppService(@NonNull DataManager dataManager,
                          @NonNull JdbcOAuth2ClientService clientService,
                          @NonNull TenantContextHolder tenantContextHolder) {
        this.dataManager = dataManager;
        this.clientService = clientService;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * 创建应用。
     *
     * <p>如果提供了 {@code clientDetails}，先创建 OAuth2 客户端，再关联到应用。
     *
     * @param app           应用信息
     * @param clientDetails 可选的 OAuth2 客户端详情，为 null 时不创建客户端
     * @return 保存后的应用
     */
    @Transactional
    public AuthApp create(@NonNull AuthApp app, @Nullable ClientDetails clientDetails) {
        if (clientDetails != null) {
            clientService.saveClient(clientDetails);
            app.setClientId(clientDetails.clientId());
        }
        AuthApp saved = dataManager.save(AuthApp.class, app);
        log.info("Created app: name={}, clientId={}", app.getName(), app.getClientId());
        return saved;
    }

    /**
     * 更新应用信息。
     *
     * <p>先从数据库加载已有数据，再合并请求中非 null 的字段，避免部分更新时将未传字段置 null。
     *
     * @param app 应用信息（必须包含 id，仅非 null 字段会被更新）
     * @return 更新后的应用
     */
    public AuthApp update(@NonNull AuthApp app) {
        var existing = dataManager.findById(AuthApp.class, app.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("App not found: id=" + app.getId());
        }
        AuthApp target = existing.get();
        mergeNonNull(target, app);
        AuthApp saved = dataManager.save(AuthApp.class, target);
        log.info("Updated app: id={}, name={}", app.getId(), target.getName());
        return saved;
    }

    /**
     * 更新应用及其关联的 OAuth2 客户端。
     *
     * @param app           应用信息
     * @param clientDetails OAuth2 客户端详情，为 null 时不更新客户端
     * @return 更新后的应用
     */
    @Transactional
    public AuthApp updateWithClient(@NonNull AuthApp app, @Nullable ClientDetails clientDetails) {
        if (clientDetails != null) {
            clientService.saveClient(clientDetails);
        }
        return update(app);
    }

    /**
     * 根据 ID 查找应用。
     *
     * @param id 应用 ID
     * @return 应用信息，如果不存在则返回 empty
     */
    public Optional<AuthApp> findById(@NonNull Long id) {
        return dataManager.findById(AuthApp.class, id);
    }

    /**
     * 获取应用列表，支持租户过滤。
     *
     * @param tenantId 租户 ID，为 null 时返回全部应用
     * @return 应用列表
     */
    public List<AuthApp> findAll(@Nullable String tenantId) {
        // 临时清除租户上下文，避免 DataManager 自动加 tenant_id 条件
        // 应用管理需要同时查看全局应用（tenant_id IS NULL）和租户应用
        try (var scope = tenantContextHolder.scope(null)) {
            List<AuthApp> all = dataManager.findAll(AuthApp.class);
            if (tenantId != null) {
                return all.stream()
                        .filter(app -> app.getTenantId() == null || tenantId.equals(app.getTenantId()))
                        .toList();
            }
            return all;
        }
    }

    /**
     * 删除应用，级联删除关联的 OAuth2 客户端。
     *
     * @param id 应用 ID
     */
    @Transactional
    public void delete(@NonNull Long id) {
        var app = dataManager.findById(AuthApp.class, id);
        if (app.isEmpty()) {
            return;
        }
        AuthApp authApp = app.get();
        // 级联删除关联的 OAuth2 客户端
        if (authApp.getClientId() != null) {
            clientService.deleteClient(authApp.getClientId());
            log.info("Deleted associated OAuth2 client: clientId={}", authApp.getClientId());
        }
        dataManager.deleteById(AuthApp.class, id);
        log.info("Deleted app: id={}, name={}", id, authApp.getName());
    }

    /**
     * 更新应用启停状态。
     *
     * <p>联动更新关联的 OAuth2 客户端状态——停用应用后 OAuth2 授权将被拒绝。
     *
     * @param id     应用 ID
     * @param status 状态：1=启用，0=停用
     */
    @Transactional
    public void updateStatus(@NonNull Long id, int status) {
        var app = dataManager.findById(AuthApp.class, id);
        if (app.isEmpty()) {
            return;
        }
        AuthApp authApp = app.get();
        authApp.setStatus(status);
        dataManager.save(AuthApp.class, authApp);

        // 联动 OAuth2 客户端状态
        if (authApp.getClientId() != null) {
            updateClientStatus(authApp.getClientId(), status);
        }
        log.info("Updated app status: id={}, name={}, status={}", id, authApp.getName(), status);
    }

    /**
     * 获取微前端应用列表（仅启用的 mf/wujie 类型应用）。
     *
     * <p>用于 workspace 宿主应用动态加载微前端子应用。
     * 如果指定了租户 ID，同时包含全局应用（tenant_id IS NULL）。
     *
     * @param tenantId 租户 ID，为 null 时返回全部
     * @return 启用的微前端应用列表，按 sortOrder 排序
     */
    public List<AuthApp> findEnabledMicroApps(@Nullable String tenantId) {
        // 临时清除租户上下文，避免 DataManager 自动加 tenant_id 条件
        try (var scope = tenantContextHolder.scope(null)) {
            var condition = Conditions.builder(AuthApp.class)
                    .eq(AuthApp::getStatus, 1)
                    .in(AuthApp::getAppType, List.of("mf", "wujie"));

            List<AuthApp> all = dataManager.findList(AuthApp.class, condition.build());

            if (tenantId != null) {
                return all.stream()
                        .filter(app -> app.getTenantId() == null || tenantId.equals(app.getTenantId()))
                        .sorted((a, b) -> Integer.compare(
                                a.getSortOrder() != null ? a.getSortOrder() : 99,
                                b.getSortOrder() != null ? b.getSortOrder() : 99))
                        .toList();
            }

            return all.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 99,
                            b.getSortOrder() != null ? b.getSortOrder() : 99))
                    .toList();
        }
    }

    // ========== 私有方法 ==========

    /**
     * 将 source 中非 null 的字段合并到 target，避免部分更新时将未传字段置 null。
     */
    private void mergeNonNull(@NonNull AuthApp target, @NonNull AuthApp source) {
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        if (source.getLogo() != null) {
            target.setLogo(source.getLogo());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getAppType() != null) {
            target.setAppType(source.getAppType());
        }
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        if (source.getSortOrder() != null) {
            target.setSortOrder(source.getSortOrder());
        }
        if (source.getActiveRule() != null) {
            target.setActiveRule(source.getActiveRule());
        }
        if (source.getEntryUrl() != null) {
            target.setEntryUrl(source.getEntryUrl());
        }
        if (source.getExportPath() != null) {
            target.setExportPath(source.getExportPath());
        }
        if (source.getWujieUrl() != null) {
            target.setWujieUrl(source.getWujieUrl());
        }
        if (source.getAllowedOrigins() != null) {
            target.setAllowedOrigins(source.getAllowedOrigins());
        }
        if (source.getClientId() != null) {
            target.setClientId(source.getClientId());
        }
        if (source.getTenantId() != null) {
            target.setTenantId(source.getTenantId());
        }
    }

    /**
     * 更新 OAuth2 客户端的启停状态。
     */
    private void updateClientStatus(@NonNull String clientId, int status) {
        var client = dataManager.findOneByField(AuthClient.class, AuthClient::getClientId, clientId);
        if (client.isPresent()) {
            AuthClient authClient = client.get();
            authClient.setStatus(status);
            dataManager.save(AuthClient.class, authClient);
            log.info("Updated OAuth2 client status: clientId={}, status={}", clientId, status);
        }
    }
}
