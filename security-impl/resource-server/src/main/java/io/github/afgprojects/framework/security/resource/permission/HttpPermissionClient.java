package io.github.afgprojects.framework.security.resource.permission;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.RestClient;

import java.util.Set;

/**
 * 基于 HTTP 的远程权限客户端实现。
 *
 * <p>通过 HTTP 调用认证服务器的权限查询 API。
 */
@Slf4j
public class HttpPermissionClient implements RemotePermissionClient {

    private final RestClient restClient;

    public HttpPermissionClient(String authServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authServerUrl)
                .build();
    }

    public HttpPermissionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean hasPermission(@NonNull String userId, @NonNull String permission, @Nullable String tenantId) {
        try {
            Boolean result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/permissions/check")
                            .queryParam("userId", userId)
                            .queryParam("permission", permission)
                            .queryParam("tenantId", tenantId)
                            .build())
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check permission: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role, @Nullable String tenantId) {
        try {
            Boolean result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/permissions/check-role")
                            .queryParam("userId", userId)
                            .queryParam("role", role)
                            .queryParam("tenantId", tenantId)
                            .build())
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to check role: userId={}, role={}", userId, role, e);
            return false;
        }
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public Set<String> getPermissions(@NonNull String userId, @Nullable String tenantId) {
        try {
            Set<String> result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/permissions/{userId}")
                            .queryParam("tenantId", tenantId)
                            .build(userId))
                    .retrieve()
                    .body(Set.class);
            return result != null ? result : Set.of();
        } catch (Exception e) {
            log.error("Failed to get permissions: userId={}", userId, e);
            return Set.of();
        }
    }
}