/**
 * API 请求签名验证
 * <p>
 * 提供请求签名验证机制，防止请求篡改和重放攻击。
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.signature.SignatureRequired} - 标记需要签名验证的接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.signature.SignatureInterceptor} - 签名验证拦截器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.signature.SignatureGenerator} - 签名生成器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.signature.NonceCache} - Nonce 缓存，防重放</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.signature.SignatureProperties} - 配置属性</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * // 1. 配置密钥
 * afg:
 *   security:
 *     signature:
 *       enabled: true
 *       keys:
 *         default:
 *           secret: "your-secret-key-at-least-32-chars"
 *
 * // 2. 在 Controller 方法上标记注解
 * &#64;PostMapping("/api/sensitive")
 * &#64;SignatureRequired(timestampTolerance = 300)
 * public Result&lt;String&gt; sensitiveOperation(&#64;RequestBody Request request) {
 *     return Results.success("ok");
 * }
 *
 * // 3. 客户端签名
 * String timestamp = String.valueOf(System.currentTimeMillis());
 * String nonce = UUID.randomUUID().toString();
 * String body = jsonBody;
 * String signature = SignatureGenerator.generate(algorithm, secret, timestamp, nonce, body);
 *
 * // 4. 发送请求
 * request.header("X-Signature", signature)
 *         .header("X-Timestamp", timestamp)
 *         .header("X-Nonce", nonce);
 * </pre>
 */
package io.github.afgprojects.framework.core.web.security.signature;
