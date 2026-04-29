package io.github.afgprojects.framework.core.web.security.signature;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 签名生成器
 * <p>
 * 支持 HMAC-SHA256/384/512 签名算法。
 * 签名流程：
 * <ol>
 *   <li>构造签名字符串：timestamp + nonce + body（如果有）</li>
 *   <li>使用 HMAC 算法生成签名</li>
 *   <li>Base64 编码签名结果</li>
 * </ol>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class SignatureGenerator {

    private static final Logger log = LoggerFactory.getLogger(SignatureGenerator.class);

    /**
     * 生成签名
     *
     * @param algorithm 签名算法
     * @param secret    密钥
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param body      请求体（可为 null）
     * @return Base64 编码的签名字符串
     */
    @NonNull
    public String generate(
            @NonNull SignatureAlgorithm algorithm,
            @NonNull String secret,
            @NonNull String timestamp,
            @NonNull String nonce,
            @Nullable String body) {

        // 构造签名字符串
        String signingString = buildSigningString(timestamp, nonce, body);

        // 生成 HMAC 签名
        byte[] signature = hmac(algorithm, secret, signingString);

        // Base64 编码
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * 验证签名
     *
     * @param algorithm     签名算法
     * @param secret        密钥
     * @param timestamp     时间戳
     * @param nonce         随机数
     * @param body          请求体（可为 null）
     * @param expectedSign  期望的签名
     * @return 如果签名验证通过返回 true
     */
    public boolean verify(
            @NonNull SignatureAlgorithm algorithm,
            @NonNull String secret,
            @NonNull String timestamp,
            @NonNull String nonce,
            @Nullable String body,
            @NonNull String expectedSign) {

        try {
            String actualSign = generate(algorithm, secret, timestamp, nonce, body);
            return constantTimeEquals(actualSign, expectedSign);
        } catch (RuntimeException e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构造签名字符串
     * <p>
     * 格式：timestamp\nnonce\nbody
     */
    @NonNull
    protected String buildSigningString(
            @NonNull String timestamp,
            @NonNull String nonce,
            @Nullable String body) {

        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append('\n');
        sb.append(nonce);
        if (body != null && !body.isEmpty()) {
            sb.append('\n').append(body);
        }
        return sb.toString();
    }

    /**
     * 使用 HMAC 算法生成签名
     */
    @NonNull
    protected byte[] hmac(
            @NonNull SignatureAlgorithm algorithm,
            @NonNull String secret,
            @NonNull String data) {

        try {
            Mac mac = Mac.getInstance(algorithm.getAlgorithm());
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    algorithm.getAlgorithm());
            mac.init(keySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unsupported algorithm: " + algorithm, e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid secret key", e);
        }
    }

    /**
     * 常量时间比较，防止时序攻击
     */
    protected boolean constantTimeEquals(@NonNull String a, @NonNull String b) {
        if (a.length() != b.length()) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
