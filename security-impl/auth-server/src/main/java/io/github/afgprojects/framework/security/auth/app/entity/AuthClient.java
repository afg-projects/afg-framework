package io.github.afgprojects.framework.security.auth.app.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 客户端实体。
 *
 * <p>映射已有的 {@code auth_client} 数据库表，提供 OAuth2 客户端的持久化存储。
 * 与 {@link AuthApp} 通过 {@code client_id} 关联。
 *
 * <p>注意：数据库字段 {@code redirect_uris}、{@code scopes}、{@code grant_types}
 * 在数据库中以逗号分隔字符串存储，在 {@code ClientDetails} record 中以 {@code Set<String>} 表示，
 * 转换逻辑在 {@code JdbcOAuth2ClientService} 中处理。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "auth_client")
public class AuthClient extends BaseEntity {

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 255)
    private @Nullable String clientSecret;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    /**
     * 重定向 URI 列表，逗号分隔
     */
    @Column(name = "redirect_uris")
    private @Nullable String redirectUris;

    /**
     * 授权范围，逗号分隔
     */
    @Column(name = "scopes", length = 500)
    private @Nullable String scopes;

    /**
     * 授权类型，逗号分隔
     */
    @Column(name = "grant_types", length = 200)
    private @Nullable String grantTypes;

    @Column(name = "require_pkce")
    private Boolean requirePkce = false;

    @Column(name = "access_token_ttl")
    private Long accessTokenTtl = 7200L;

    @Column(name = "refresh_token_ttl")
    private Long refreshTokenTtl = 604800L;

    /**
     * 状态：1=启用，0=停用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 检查客户端是否启用。
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
