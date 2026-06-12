package io.github.afgprojects.framework.governance.client.common;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Governance 客户端签名拦截器
 *
 * @author afg-projects
 */
@Slf4j
public class SignatureClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> KEY_SIGNATURE =
            Metadata.Key.of("x-signature", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_TIMESTAMP =
            Metadata.Key.of("x-timestamp", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_NONCE =
            Metadata.Key.of("x-nonce", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_KEY_ID =
            Metadata.Key.of("x-key-id", Metadata.ASCII_STRING_MARSHALLER);

    private final String keyId;
    private final String secret;
    private final SecureRandom secureRandom = new SecureRandom();

    public SignatureClientInterceptor(String keyId, String secret) {
        this.keyId = keyId;
        this.secret = secret;
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
                String timestamp = String.valueOf(System.currentTimeMillis());
                String nonce = generateNonce();
                String methodDescriptor = method.getFullMethodName();

                String signature = generateSignature(timestamp, nonce, methodDescriptor);

                headers.put(KEY_SIGNATURE, signature);
                headers.put(KEY_TIMESTAMP, timestamp);
                headers.put(KEY_NONCE, nonce);
                headers.put(KEY_KEY_ID, keyId);

                log.debug("Added signature to request: method={}, keyId={}", methodDescriptor, keyId);

                super.start(responseListener, headers);
            }
        };
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateSignature(String timestamp, String nonce, String body) {
        try {
            String signingString = timestamp + "\n" + nonce + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] signature = mac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR, "Failed to generate signature", e);
        }
    }
}
