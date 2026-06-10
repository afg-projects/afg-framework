package io.github.afgprojects.framework.governance.server.grpc;

import io.github.afgprojects.framework.core.web.security.signature.SignatureAlgorithm;
import io.github.afgprojects.framework.core.web.security.signature.SignatureGenerator;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 服务端签名认证拦截器
 * <p>
 * 验证客户端请求中的签名信息，防止请求伪造和重放攻击。
 * <p>
 * 客户端需要在 gRPC Metadata 中提供以下信息：
 * <ul>
 *   <li>X-Signature: 签名值（Base64 编码）</li>
 *   <li>X-Timestamp: 时间戳（毫秒）</li>
 *   <li>X-Nonce: 随机数（防重放）</li>
 *   <li>X-Key-Id: 密钥标识</li>
 * </ul>
 * <p>
 * 签名算法：
 * <pre>
 * signature = HMAC-SHA256(secret, timestamp + "\n" + nonce + "\n" + body)
 * </pre>
 */
@Slf4j
@Component
@GlobalServerInterceptor
public class GrpcSignatureServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> KEY_SIGNATURE =
            Metadata.Key.of("x-signature", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_TIMESTAMP =
            Metadata.Key.of("x-timestamp", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_NONCE =
            Metadata.Key.of("x-nonce", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_KEY_ID =
            Metadata.Key.of("x-key-id", Metadata.ASCII_STRING_MARSHALLER);

    private final GovernanceServerSecurityProperties properties;
    private final SignatureGenerator signatureGenerator;

    // 用于缓存已处理的 nonce，防止重放攻击
    private final java.util.concurrent.ConcurrentHashMap<String, Long> nonceCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int NONCE_CACHE_MAX_SIZE = 10000;

    public GrpcSignatureServerInterceptor(GovernanceServerSecurityProperties properties) {
        this.properties = properties;
        this.signatureGenerator = new SignatureGenerator();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // 如果安全认证未启用，直接放行
        if (!properties.isEnabled() || !properties.getSignature().isEnabled()) {
            return next.startCall(call, headers);
        }

        // 提取签名信息
        String signature = headers.get(KEY_SIGNATURE);
        String timestamp = headers.get(KEY_TIMESTAMP);
        String nonce = headers.get(KEY_NONCE);
        String keyId = headers.get(KEY_KEY_ID);

        // 验证必要字段
        if (signature == null || timestamp == null || nonce == null) {
            log.warn("Missing signature headers: signature={}, timestamp={}, nonce={}",
                    signature != null, timestamp != null, nonce != null);
            call.close(Status.UNAUTHENTICATED.withDescription("Missing signature headers"), headers);
            return new ServerCall.Listener<>() {};
        }

        // 验证时间戳
        try {
            long timestampMs = Long.parseLong(timestamp);
            long currentTimeMs = System.currentTimeMillis();
            long toleranceMs = TimeUnit.SECONDS.toMillis(properties.getSignature().getTimestampTolerance());

            if (Math.abs(currentTimeMs - timestampMs) > toleranceMs) {
                log.warn("Timestamp out of tolerance: timestamp={}, current={}, tolerance={}ms",
                        timestampMs, currentTimeMs, toleranceMs);
                call.close(Status.UNAUTHENTICATED.withDescription("Timestamp out of tolerance"), headers);
                return new ServerCall.Listener<>() {};
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid timestamp format"), headers);
            return new ServerCall.Listener<>() {};
        }

        // 验证 nonce（防重放）
        if (nonceCache.containsKey(nonce)) {
            log.warn("Nonce already used (replay attack): {}", nonce);
            call.close(Status.UNAUTHENTICATED.withDescription("Nonce already used"), headers);
            return new ServerCall.Listener<>() {};
        }

        // 清理过期的 nonce 缓存
        cleanupNonceCache();

        // 验证签名
        // 对于 gRPC，我们使用方法名作为签名的 body 部分
        String methodDescriptor = call.getMethodDescriptor().getFullMethodName();
        boolean valid = signatureGenerator.verify(
                SignatureAlgorithm.HMAC_SHA256,
                properties.getSignature().getSecret(),
                timestamp,
                nonce,
                methodDescriptor,
                signature
        );

        if (!valid) {
            log.warn("Signature verification failed: method={}", methodDescriptor);
            call.close(Status.UNAUTHENTICATED.withDescription("Signature verification failed"), headers);
            return new ServerCall.Listener<>() {};
        }

        // 记录 nonce
        nonceCache.put(nonce, System.currentTimeMillis());

        // 签名验证通过，继续处理请求
        log.debug("Signature verification passed: method={}", methodDescriptor);
        return next.startCall(call, headers);
    }

    /**
     * 清理过期的 nonce 缓存
     */
    private void cleanupNonceCache() {
        if (nonceCache.size() > NONCE_CACHE_MAX_SIZE) {
            long expireTime = System.currentTimeMillis() -
                    TimeUnit.SECONDS.toMillis(properties.getSignature().getTimestampTolerance() * 2);
            nonceCache.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        }
    }
}
