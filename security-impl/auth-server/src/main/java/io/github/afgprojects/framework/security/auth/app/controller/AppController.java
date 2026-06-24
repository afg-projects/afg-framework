package io.github.afgprojects.framework.security.auth.app.controller;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.security.auth.app.entity.AuthApp;
import io.github.afgprojects.framework.security.auth.app.service.JdbcAppService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 应用管理 API。
 *
 * <p>提供应用的 CRUD、启停控制和微前端列表查询。
 * 通过 {@code @Bean} 注册而非组件扫描，路径前缀需包含 {@code /auth-api}。
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/apps")
public class AppController {

    private final JdbcAppService appService;

    public AppController(JdbcAppService appService) {
        this.appService = appService;
    }

    /**
     * 获取应用列表，支持租户过滤。
     */
    @GetMapping
    public Result<List<AuthApp>> list(@RequestParam @Nullable String tenantId) {
        return Result.success(appService.findAll(tenantId));
    }

    /**
     * 获取应用详情。
     */
    @GetMapping("/{id}")
    public Result<AuthApp> getById(@PathVariable String id) {
        return Result.success(appService.findById(id).orElse(null));
    }

    /**
     * 创建应用。
     *
     * <p>请求体可包含 {@code oAuth2Client} 字段，用于同时创建 OAuth2 客户端。
     */
    @PostMapping
    public Result<AuthApp> create(@RequestBody AppCreateRequest request) {
        ClientDetails clientDetails = null;
        if (request.oAuth2Client != null) {
            clientDetails = toClientDetails(request.oAuth2Client);
        }
        return Result.success(appService.create(request.toAuthApp(), clientDetails));
    }

    /**
     * 更新应用。
     *
     * <p>请求体可包含 {@code oAuth2Client} 字段，用于同时更新 OAuth2 客户端。
     */
    @PutMapping("/{id}")
    public Result<AuthApp> update(@PathVariable String id, @RequestBody AppUpdateRequest request) {
        AuthApp app = request.toAuthApp();
        app.setId(id);
        ClientDetails clientDetails = null;
        if (request.oAuth2Client != null) {
            clientDetails = toClientDetails(request.oAuth2Client);
        }
        return Result.success(appService.updateWithClient(app, clientDetails));
    }

    /**
     * 删除应用，级联删除关联的 OAuth2 客户端。
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable String id) {
        appService.delete(id);
        return Result.success(true);
    }

    /**
     * 更新应用启停状态。
     *
     * @param id     应用 ID
     * @param status 状态：1=启用，0=停用
     */
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable String id, @RequestBody StatusRequest statusRequest) {
        appService.updateStatus(id, statusRequest.status);
        return Result.success(true);
    }

    /**
     * 获取微前端应用列表（仅启用的 mf/wujie 类型）。
     *
     * <p>供 workspace 宿主应用动态加载微前端子应用。
     */
    @GetMapping("/micro-apps")
    public Result<List<AuthApp>> microApps(@RequestParam @Nullable String tenantId) {
        return Result.success(appService.findEnabledMicroApps(tenantId));
    }

    // ========== 内部方法 ==========

    private ClientDetails toClientDetails(OAuth2ClientRequest request) {
        return new ClientDetails(
                request.clientId,
                request.clientSecret,
                request.clientName,
                request.redirectUris != null ? request.redirectUris : Set.of(),
                request.scopes != null ? request.scopes : Set.of(),
                request.grantTypes != null ? request.grantTypes : Set.of(),
                request.requirePkce != null ? request.requirePkce : false,
                request.accessTokenTtl != null ? Duration.ofSeconds(request.accessTokenTtl) : Duration.ofHours(2),
                request.refreshTokenTtl != null ? Duration.ofSeconds(request.refreshTokenTtl) : Duration.ofDays(7)
        );
    }

    // ========== 请求 DTO ==========

    /**
     * 创建应用请求
     */
    public record AppCreateRequest(
            String name,
            String logo,
            String description,
            String appType,
            Integer sortOrder,
            String activeRule,
            String entryUrl,
            String exportPath,
            String wujieUrl,
            String allowedOrigins,
            String tenantId,
            OAuth2ClientRequest oAuth2Client
    ) {
        public AuthApp toAuthApp() {
            AuthApp app = new AuthApp();
            app.setName(name);
            app.setLogo(logo);
            app.setDescription(description);
            app.setAppType(appType != null ? appType : "mf");
            app.setSortOrder(sortOrder != null ? sortOrder : 0);
            app.setActiveRule(activeRule);
            app.setEntryUrl(entryUrl);
            app.setExportPath(exportPath);
            app.setWujieUrl(wujieUrl);
            app.setAllowedOrigins(allowedOrigins);
            app.setTenantId(tenantId);
            return app;
        }
    }

    /**
     * 更新应用请求
     */
    public record AppUpdateRequest(
            String name,
            String logo,
            String description,
            String appType,
            Integer status,
            Integer sortOrder,
            String activeRule,
            String entryUrl,
            String exportPath,
            String wujieUrl,
            String allowedOrigins,
            String tenantId,
            OAuth2ClientRequest oAuth2Client
    ) {
        public AuthApp toAuthApp() {
            AuthApp app = new AuthApp();
            app.setName(name);
            app.setLogo(logo);
            app.setDescription(description);
            app.setAppType(appType);
            app.setStatus(status);
            app.setSortOrder(sortOrder);
            app.setActiveRule(activeRule);
            app.setEntryUrl(entryUrl);
            app.setExportPath(exportPath);
            app.setWujieUrl(wujieUrl);
            app.setAllowedOrigins(allowedOrigins);
            app.setTenantId(tenantId);
            return app;
        }
    }

    /**
     * 状态更新请求
     */
    public record StatusRequest(int status) {
    }

    /**
     * OAuth2 客户端请求（嵌套在应用创建/更新请求中）
     */
    public record OAuth2ClientRequest(
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
    }
}
