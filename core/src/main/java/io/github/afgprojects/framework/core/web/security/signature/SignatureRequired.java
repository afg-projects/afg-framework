package io.github.afgprojects.framework.core.web.security.signature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要签名验证的接口
 * <p>
 * 用于 Controller 方法上，表示该接口需要进行签名验证。
 * 签名验证可以防止请求篡改和重放攻击。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;PostMapping("/api/sensitive")
 * &#64;SignatureRequired(timestampTolerance = 300)
 * public Result&lt;String&gt; sensitiveOperation(@RequestBody Request request) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @see SignatureInterceptor
 * @see SignatureGenerator
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignatureRequired {

    /**
     * 签名算法
     * <p>
     * 默认使用 HMAC-SHA256
     *
     * @return 签名算法
     */
    SignatureAlgorithm algorithm() default SignatureAlgorithm.HMAC_SHA256;

    /**
     * 时间戳容忍度（秒）
     * <p>
     * 客户端时间戳与服务端时间的最大差值，用于防止重放攻击。
     * 默认 300 秒（5 分钟）。
     *
     * @return 时间戳容忍度
     */
    int timestampTolerance() default 300;

    /**
     * 是否需要 nonce
     * <p>
     * nonce 是客户端生成的随机字符串，用于防止重放攻击。
     * 开启后，服务端会缓存已使用的 nonce，在时间戳容忍度时间内拒绝重复的 nonce。
     * 默认开启。
     *
     * @return 是否需要 nonce
     */
    boolean nonceRequired() default true;

    /**
     * 密钥标识
     * <p>
     * 用于支持多密钥场景。如果指定，则使用对应密钥进行签名验证；
     * 否则使用默认密钥。
     *
     * @return 密钥标识
     */
    String keyId() default "";
}
