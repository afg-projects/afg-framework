package io.github.afgprojects.framework.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import io.github.afgprojects.framework.security.auth.user.AfgClientDetails;
import io.github.afgprojects.framework.security.auth.user.AfgClientDetailsService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JDBC 实现的 OAuth2 客户端服务
 *
 * <p>从 auth_client 表加载客户端信息，适用于生产环境。
 *
 * <h3>数据库表结构</h3>
 * <pre>
 * CREATE TABLE auth_client (
 *     id VARCHAR(64) PRIMARY KEY,
 *     client_id VARCHAR(128) UNIQUE NOT NULL,
 *     client_secret VARCHAR(256),
 *     client_name VARCHAR(256) NOT NULL,
 *     redirect_uris TEXT NOT NULL,
 *     scopes TEXT NOT NULL,
 *     grant_types TEXT NOT NULL,
 *     auth_methods TEXT NOT NULL,
 *     require_pkce BOOLEAN DEFAULT FALSE,
 *     access_token_ttl INTEGER DEFAULT 3600,
 *     refresh_token_ttl INTEGER DEFAULT 604800,
 *     status INTEGER DEFAULT 1,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Service
 * public class JdbcClientDetailsService implements AfgClientDetailsService {
 *     private final JdbcTemplate jdbcTemplate;
 *
 *     @Override
 *     public AfgClientDetails loadClientByClientId(String clientId) {
 *         String sql = "SELECT * FROM auth_client WHERE client_id = ? AND status = 1";
 *         return jdbcTemplate.queryForObject(sql, new ClientRowMapper(), clientId);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcClientDetailsService implements AfgClientDetailsService {

    private static final String SQL_LOAD_CLIENT =
            "SELECT id, client_id, client_secret, client_name, redirect_uris, scopes, " +
            "grant_types, auth_methods, require_pkce, access_token_ttl, refresh_token_ttl, status " +
            "FROM auth_client WHERE client_id = ? AND status = 1";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcClientDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Nullable
    public AfgClientDetails loadClientByClientId(String clientId) {
        log.debug("Loading client by clientId: {}", clientId);

        try {
            AfgClientDetails clientDetails = jdbcTemplate.queryForObject(
                    SQL_LOAD_CLIENT,
                    new ClientRowMapper(),
                    clientId);

            log.debug("Client loaded successfully: clientId={}, name={}",
                    clientId, clientDetails.getClientName());

            return clientDetails;

        } catch (Exception e) {
            log.warn("Client not found or error loading client: clientId={}, error={}",
                    clientId, e.getMessage());
            return null;
        }
    }

    /**
     * 客户端行映射器
     *
     * <p>将数据库行映射为 AfgClientDetails 对象
     */
    private static class ClientRowMapper implements RowMapper<AfgClientDetails> {

        @Override
        public AfgClientDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
            String clientId = rs.getString("client_id");
            String clientSecret = rs.getString("client_secret");
            String clientName = rs.getString("client_name");

            // 解析逗号分隔的字段
            Set<String> redirectUris = parseSet(rs.getString("redirect_uris"));
            Set<String> scopes = parseSet(rs.getString("scopes"));
            Set<String> grantTypeStrings = parseSet(rs.getString("grant_types"));
            Set<String> authMethodStrings = parseSet(rs.getString("auth_methods"));

            // 转换为 Spring Security 类型
            Set<AuthorizationGrantType> grantTypes = convertGrantTypes(grantTypeStrings);
            Set<ClientAuthenticationMethod> authMethods = convertAuthMethods(authMethodStrings);

            return AfgClientDetails.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .clientName(clientName)
                    .redirectUris(redirectUris)
                    .scopes(scopes)
                    .authorizationGrantTypes(grantTypes)
                    .clientAuthenticationMethods(authMethods)
                    .build();
        }

        /**
         * 解析逗号分隔的字符串为 Set
         *
         * @param value 字符串值
         * @return Set 集合
         */
        private Set<String> parseSet(@Nullable String value) {
            if (value == null || value.isBlank()) {
                return Collections.emptySet();
            }

            Set<String> result = new HashSet<>();
            String[] parts = value.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }

        /**
         * 转换授权类型字符串为 AuthorizationGrantType
         *
         * @param grantTypeStrings 授权类型字符串集合
         * @return AuthorizationGrantType 集合
         */
        private Set<AuthorizationGrantType> convertGrantTypes(Set<String> grantTypeStrings) {
            Set<AuthorizationGrantType> grantTypes = new HashSet<>();

            for (String grantType : grantTypeStrings) {
                switch (grantType.toLowerCase()) {
                    case "authorization_code":
                        grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                        break;
                    case "client_credentials":
                        grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                        break;
                    case "refresh_token":
                        grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                        break;
                    case "password":
                        // Password grant is deprecated, but still supported for legacy systems
                        grantTypes.add(new AuthorizationGrantType("password"));
                        break;
                    default:
                        log.warn("Unknown grant type: {}", grantType);
                        grantTypes.add(new AuthorizationGrantType(grantType));
                }
            }

            return grantTypes;
        }

        /**
         * 转换认证方法字符串为 ClientAuthenticationMethod
         *
         * @param authMethodStrings 认证方法字符串集合
         * @return ClientAuthenticationMethod 集合
         */
        private Set<ClientAuthenticationMethod> convertAuthMethods(Set<String> authMethodStrings) {
            Set<ClientAuthenticationMethod> authMethods = new HashSet<>();

            for (String authMethod : authMethodStrings) {
                switch (authMethod.toLowerCase()) {
                    case "client_secret_basic":
                        authMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                        break;
                    case "client_secret_post":
                        authMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                        break;
                    case "client_secret_jwt":
                        authMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                        break;
                    case "private_key_jwt":
                        authMethods.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                        break;
                    case "none":
                        authMethods.add(ClientAuthenticationMethod.NONE);
                        break;
                    default:
                        log.warn("Unknown authentication method: {}", authMethod);
                        authMethods.add(new ClientAuthenticationMethod(authMethod));
                }
            }

            return authMethods;
        }
    }
}