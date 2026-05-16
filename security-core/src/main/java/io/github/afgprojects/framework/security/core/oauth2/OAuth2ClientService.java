package io.github.afgprojects.framework.security.core.oauth2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;

/**
 * OAuth2 客户端服务接口。
 *
 * <p>管理 OAuth2 客户端信息。
 *
 * @since 1.0.0
 */
public interface OAuth2ClientService {

    /**
     * 根据客户端 ID 加载客户端详情。
     *
     * @param clientId 客户端 ID，永不为 null
     * @return 客户端详情，如果不存在则返回 null
     */
    @Nullable
    ClientDetails loadClientByClientId(@NonNull String clientId);

    /**
     * 验证客户端凭证。
     *
     * @param clientId 客户端 ID，永不为 null
     * @param clientSecret 客户端密钥
     * @return 如果验证通过返回 true
     */
    boolean validateClientCredentials(@NonNull String clientId, @Nullable String clientSecret);

    /**
     * 检查客户端是否需要 PKCE。
     *
     * @param clientId 客户端 ID，永不为 null
     * @return 如果需要 PKCE 返回 true
     */
    boolean requiresPkce(@NonNull String clientId);

    /**
     * 保存客户端详情。
     *
     * @param clientDetails 客户端详情，永不为 null
     */
    void saveClient(@NonNull ClientDetails clientDetails);

    /**
     * 删除客户端。
     *
     * @param clientId 客户端 ID，永不为 null
     */
    void deleteClient(@NonNull String clientId);
}