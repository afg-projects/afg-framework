package io.github.afgprojects.framework.security.auth.app.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * 应用实体。
 *
 * <p>统一管理应用的业务信息、微前端配置和 OAuth2 客户端关联。
 * 与 {@code auth_client} 表通过 {@code client_id} 外键关联，
 * {@code auth_app} 负责应用业务信息，{@code auth_client} 负责 OAuth2 协议参数。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "auth_app")
public class AuthApp extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "logo", length = 500)
    private @Nullable String logo;

    @Column(name = "description", length = 500)
    private @Nullable String description;

    /**
     * 应用类型：mf / wujie / standalone / api
     */
    @Column(name = "app_type", nullable = false, length = 20)
    private String appType = "mf";

    /**
     * 状态：1=启用，0=停用
     */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // ========== 微前端配置 ==========

    /**
     * 路由前缀，如 /identity
     */
    @Column(name = "active_rule", length = 100)
    private @Nullable String activeRule;

    /**
     * MF 模式：remoteEntry.js URL
     */
    @Column(name = "entry_url", length = 500)
    private @Nullable String entryUrl;

    /**
     * MF 模式：暴露模块路径，如 ./export-app
     */
    @Column(name = "export_path", length = 100)
    private @Nullable String exportPath;

    /**
     * Wujie 模式：子应用完整 URL
     */
    @Column(name = "wujie_url", length = 500)
    private @Nullable String wujieUrl;

    // ========== CORS 配置 ==========

    /**
     * CORS 允许的来源，逗号分隔
     */
    @Column(name = "allowed_origins")
    private @Nullable String allowedOrigins;

    // ========== OAuth2 客户端关联 ==========

    /**
     * 关联的 OAuth2 客户端 ID（FK → auth_client.client_id），
     * nullable：不是所有应用都需要 OAuth2 客户端
     */
    @Column(name = "client_id", length = 100)
    private @Nullable String clientId;

    // ========== 多租户 ==========

    /**
     * 租户 ID，null 表示全局应用（所有租户可见）
     */
    @Column(name = "tenant_id", length = 50)
    private @Nullable String tenantId;

    /**
     * 检查应用是否为微前端类型（mf 或 wujie）。
     *
     * @return 如果是微前端类型返回 true
     */
    public boolean isMicroApp() {
        return "mf".equals(appType) || "wujie".equals(appType);
    }

    /**
     * 检查应用是否启用。
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
