package io.github.afgprojects.framework.security.resource.permission;

import io.github.afgprojects.framework.core.web.security.signature.SignatureAlgorithm;
import io.github.afgprojects.framework.core.web.security.signature.SignatureGenerator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * 带签名验证的远程权限客户端。
 *
 * <p>调用认证服务器内部接口时自动添加签名，实现服务间零信任认证。
 *
 * <p>签名流程：
 * <ol>
 *   <li>生成时间戳和随机 nonce</li>
 *   <li>构造签名字符串：timestamp + nonce + body</li>
 *   <li>使用 HMAC-SHA256 计算签名</li>
 *   <li>在请求头中添加 X-Signature、X-Timestamp、X-Nonce、X-Key-Id</li>
 * </ol>
 */
@Slf4j
public class SignedHttpPermissionClient implements RemotePermissionClient {

    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_KEY_ID = "X-Key-Id";

    private final RestClient restClient;
    private final SignatureGenerator signatureGenerator;
    private final String keyId;
    private final String secret;
    private final SignatureAlgorithm algorithm;
    private final SecureRandom secureRandom;

    /**
     * 创建带签名的权限客户端。
     *
     * @param authServerUrl 认证服务器地址
     * @param keyId         密钥标识
     * @param secret        密钥
     */
    public SignedHttpPermissionClient(@NonNull String authServerUrl, @NonNull String keyId, @NonNull String secret) {
        this(authServerUrl, keyId, secret, SignatureAlgorithm.HMAC_SHA256);
    }

    /**
     * 创建带签名的权限客户端。
     *
     * @param authServerUrl 认证服务器地址
     * @param keyId         密钥标识
     * @param secret        密钥
     * @param algorithm     签名算法
     */
    public SignedHttpPermissionClient(
            @NonNull String authServerUrl,
            @NonNull String keyId,
            @NonNull String secret,
            @NonNull SignatureAlgorithm algorithm) {

        this.restClient = RestClient.builder()
                .baseUrl(authServerUrl)
                .build();
        this.signatureGenerator = new SignatureGenerator();
        this.keyId = keyId;
        this.secret = secret;
        this.algorithm = algorithm;
        this.secureRandom = new SecureRandom();
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
                    .headers(this::addSignatureHeaders)
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
                    .headers(this::addSignatureHeaders)
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
                    .headers(this::addSignatureHeaders)
                    .retrieve()
                    .body(Set.class);
            return result != null ? result : Set.of();
        } catch (Exception e) {
            log.error("Failed to get permissions: userId={}", userId, e);
            return Set.of();
        }
    }

    /**
     * 添加签名请求头。
     */
    private void addSignatureHeaders(org.springframework.http.HttpHeaders headers) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = generateNonce();

        String signature = signatureGenerator.generate(algorithm, secret, timestamp, nonce, null);

        headers.set(HEADER_TIMESTAMP, timestamp);
        headers.set(HEADER_NONCE, nonce);
        headers.set(HEADER_SIGNATURE, signature);
        headers.set(HEADER_KEY_ID, keyId);

        log.debug("Added signature headers: keyId={}, nonce={}", keyId, nonce);
    }

    /**
     * 生成随机 nonce。
     */
    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
