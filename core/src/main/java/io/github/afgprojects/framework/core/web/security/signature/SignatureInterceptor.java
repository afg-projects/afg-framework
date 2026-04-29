package io.github.afgprojects.framework.core.web.security.signature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 签名验证拦截器
 * <p>
 * 拦截带有 @{@link SignatureRequired} 注解的接口，验证请求签名。
 * <p>
 * 签名流程：
 * <ol>
 *   <li>从请求头获取签名相关信息：X-Signature、X-Timestamp、X-Nonce</li>
 *   <li>验证时间戳是否在容忍度范围内</li>
 *   <li>验证 nonce 是否已被使用（防重放）</li>
 *   <li>根据配置的密钥重新计算签名</li>
 *   <li>比较签名是否一致</li>
 * </ol>
 *
 * @see SignatureRequired
 * @see SignatureGenerator
 */
public class SignatureInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SignatureInterceptor.class);

    /**
     * 签名请求头
     */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /**
     * 时间戳请求头
     */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /**
     * Nonce 请求头
     */
    public static final String HEADER_NONCE = "X-Nonce";

    /**
     * 密钥标识请求头
     */
    public static final String HEADER_KEY_ID = "X-Key-Id";

    private final SignatureProperties properties;
    private final SignatureGenerator signatureGenerator;
    private final NonceCache nonceCache;

    /**
     * 创建签名拦截器
     *
     * @param properties         配置属性
     * @param signatureGenerator 签名生成器
     * @param nonceCache         nonce 缓存
     */
    public SignatureInterceptor(
            @NonNull SignatureProperties properties,
            @NonNull SignatureGenerator signatureGenerator,
            @NonNull NonceCache nonceCache) {
        this.properties = properties;
        this.signatureGenerator = signatureGenerator;
        this.nonceCache = nonceCache;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法是否有 @SignatureRequired 注解
        SignatureRequired annotation = handlerMethod.getMethodAnnotation(SignatureRequired.class);
        if (annotation == null) {
            return true;
        }

        // 验证签名
        verifySignature(request, annotation);

        return true;
    }

    /**
     * 验证签名
     */
    private void verifySignature(HttpServletRequest request, SignatureRequired annotation) {
        // 1. 获取请求头
        String signature = getRequiredHeader(request, HEADER_SIGNATURE, SignatureException.SignatureErrorType.MISSING_SIGNATURE);
        String timestampStr = getRequiredHeader(request, HEADER_TIMESTAMP, SignatureException.SignatureErrorType.MISSING_TIMESTAMP);
        String nonce = getRequiredHeader(request, HEADER_NONCE, SignatureException.SignatureErrorType.MISSING_NONCE);

        // 2. 解析并验证时间戳
        long timestamp = parseTimestamp(timestampStr);
        validateTimestamp(timestamp, annotation.timestampTolerance());

        // 3. 验证 nonce（防重放）
        if (annotation.nonceRequired()) {
            validateNonce(nonce, timestamp, annotation.timestampTolerance());
        }

        // 4. 获取密钥
        String keyId = getKeyId(request, annotation);
        SignatureProperties.KeyConfig keyConfig = getKeyConfig(keyId);

        // 5. 获取请求体
        String body = getRequestBody(request);

        // 6. 验证签名
        boolean valid = signatureGenerator.verify(
                annotation.algorithm(),
                keyConfig.getSecret(),
                timestampStr,
                nonce,
                body,
                signature);

        if (!valid) {
            log.warn("Signature verification failed: keyId={}, nonce={}", keyId, nonce);
            throw new SignatureException(SignatureException.SignatureErrorType.INVALID_SIGNATURE);
        }

        log.debug("Signature verification passed: keyId={}, nonce={}", keyId, nonce);
    }

    /**
     * 获取必需的请求头
     */
    private String getRequiredHeader(
            HttpServletRequest request,
            String headerName,
            SignatureException.SignatureErrorType errorType) {

        String value = request.getHeader(headerName);
        if (value == null || value.isEmpty()) {
            throw new SignatureException(errorType);
        }
        return value;
    }

    /**
     * 解析时间戳
     */
    private long parseTimestamp(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new SignatureException(SignatureException.SignatureErrorType.INVALID_TIMESTAMP, e);
        }
    }

    /**
     * 验证时间戳
     */
    private void validateTimestamp(long timestamp, int tolerance) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - timestamp);

        // 时间戳差值超过容忍度
        if (diff > tolerance * 1000L) {
            log.warn("Timestamp expired: timestamp={}, current={}, diff={}ms", timestamp, currentTime, diff);
            throw new SignatureException(SignatureException.SignatureErrorType.TIMESTAMP_EXPIRED);
        }
    }

    /**
     * 验证 nonce（防止重放攻击）
     */
    private void validateNonce(String nonce, long timestamp, int tolerance) {
        // 清理过期的 nonce
        long expireTime = System.currentTimeMillis() - tolerance * 1000L;
        nonceCache.cleanExpired(expireTime);

        // 检查 nonce 是否已被使用
        if (!nonceCache.checkAndAdd(nonce, timestamp)) {
            log.warn("Nonce reused, possible replay attack: nonce={}", nonce);
            throw new SignatureException(SignatureException.SignatureErrorType.NONCE_REUSED);
        }
    }

    /**
     * 获取密钥标识
     */
    private String getKeyId(HttpServletRequest request, SignatureRequired annotation) {
        // 优先使用注解指定的 keyId
        String keyId = annotation.keyId();
        if (keyId != null && !keyId.isEmpty()) {
            return keyId;
        }

        // 其次使用请求头中的 keyId
        keyId = request.getHeader(HEADER_KEY_ID);
        if (keyId != null && !keyId.isEmpty()) {
            return keyId;
        }

        // 最后使用默认 keyId
        return properties.getDefaultKeyId();
    }

    /**
     * 获取密钥配置
     */
    private SignatureProperties.KeyConfig getKeyConfig(String keyId) {
        SignatureProperties.KeyConfig keyConfig = properties.getKeyConfig(keyId);
        if (keyConfig == null) {
            log.warn("Key not found: keyId={}", keyId);
            throw new SignatureException(SignatureException.SignatureErrorType.KEY_NOT_FOUND);
        }
        if (!keyConfig.isEnabled()) {
            log.warn("Key disabled: keyId={}", keyId);
            throw new SignatureException(SignatureException.SignatureErrorType.KEY_DISABLED);
        }
        return keyConfig;
    }

    /**
     * 获取请求体
     * <p>
     * 对于可重复读取的请求（如 ContentCachingRequestWrapper），直接读取；
     * 否则返回 null。
     */
    private @Nullable String getRequestBody(HttpServletRequest request) {
        try {
            // 尝试从缓存中读取请求体
            if (request instanceof org.springframework.web.util.ContentCachingRequestWrapper wrapper) {
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    return new String(content, StandardCharsets.UTF_8);
                }
            }

            // 尝试直接读取（可能会消耗输入流）
            if (request.getInputStream().available() > 0) {
                return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.debug("Failed to read request body: {}", e.getMessage());
        }

        return null;
    }
}
