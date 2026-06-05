package io.github.afgprojects.framework.governance.server.grpc;

import io.github.afgprojects.framework.core.web.security.signature.SignatureAlgorithm;
import io.github.afgprojects.framework.core.web.security.signature.SignatureGenerator;
import io.github.afgprojects.framework.governance.server.properties.GovernanceServerSecurityProperties;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * gRPC 客户端签名拦截器
 * <p>
 * 为客户端请求自动添加签名信息，用于服务端验证。
 * <p>
 * 添加的 Metadata：
 * <ul>
 *   <li>X-Signature: 签名值（Base64 编码）</li>
 *   <li>X-Timestamp: 时间戳（毫秒）</li>
 *   <li>X-Nonce: 随机数（防重放）</li>
 *   <li>X-Key-Id: 密钥标识</li>
 * </ul>
 */
@Slf4j
@Component
public class GrpcSignatureClientInterceptor implements ClientInterceptor {

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
    private final SecureRandom secureRandom;

    public GrpcSignatureClientInterceptor(GovernanceServerSecurityProperties properties) {
        this.properties = properties;
        this.signatureGenerator = new SignatureGenerator();
        this.secureRandom = new SecureRandom();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                // 如果安全认证未启用，直接发送请求
                if (!properties.isEnabled() || !properties.getSignature().isEnabled()) {
                    super.start(responseListener, headers);
                    return;
                }

                // 生成签名参数
                String timestamp = String.valueOf(System.currentTimeMillis());
                String nonce = generateNonce();
                String keyId = properties.getSignature().getKeyId();

                // 使用方法名作为签名的 body
                String methodDescriptor = method.getFullMethodName();

                // 生成签名
                String signature = signatureGenerator.generate(
                        SignatureAlgorithm.HMAC_SHA256,
                        properties.getSignature().getSecret(),
                        timestamp,
                        nonce,
                        methodDescriptor
                );

                // 添加签名信息到 Metadata
                headers.put(KEY_SIGNATURE, signature);
                headers.put(KEY_TIMESTAMP, timestamp);
                headers.put(KEY_NONCE, nonce);
                headers.put(KEY_KEY_ID, keyId);

                log.debug("Added signature to request: method={}, keyId={}", methodDescriptor, keyId);

                super.start(responseListener, headers);
            }
        };
    }

    /**
     * 生成随机 nonce
     */
    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
