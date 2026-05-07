package io.github.afgprojects.framework.security.auth.user;

import org.jspecify.annotations.Nullable;

/**
 * OAuth2 客户端服务 SPI
 *
 * <p>提供 OAuth2 客户端信息的加载接口，由业务系统实现此接口来提供客户端信息存储。
 *
 * <p>示例实现：
 * <pre>{@code
 * @Service
 * public class JpaClientDetailsService implements AfgClientDetailsService {
 *     private final ClientRepository clientRepository;
 *
 *     public AfgClientDetails loadClientByClientId(String clientId) {
 *         ClientEntity entity = clientRepository.findByClientId(clientId);
 *         if (entity == null) {
 *             return null;
 *         }
 *         return convertToClientDetails(entity);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AfgClientDetailsService {

    /**
     * 根据客户端 ID 加载客户端详情
     *
     * @param clientId 客户端 ID
     * @return 客户端详情，如果不存在返回 null
     */
    @Nullable
    AfgClientDetails loadClientByClientId(String clientId);
}