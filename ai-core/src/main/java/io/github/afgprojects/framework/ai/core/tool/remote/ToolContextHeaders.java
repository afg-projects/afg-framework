package io.github.afgprojects.framework.ai.core.tool.remote;

import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 工具上下文 HTTP 头定义。
 *
 * <p>定义安全上下文在 HTTP 请求头中的传递方式。
 *
 * <h2>安全设计</h2>
 * <p>采用透传认证请求头的方式，而不是直接传递用户信息：
 * <ul>
 *   <li><b>Authorization</b> - OAuth2/JWT Token（推荐）</li>
 *   <li><b>Cookie</b> - Session Cookie</li>
 *   <li><b>X-Auth-Token</b> - 自定义认证 Token</li>
 * </ul>
 *
 * <p>远程服务通过验证这些认证请求头来获取真实的用户身份，
 * 避免直接传递 userId、tenantId 等敏感信息被伪造的风险。
 *
 * <h2>辅助信息</h2>
 * <p>仅传递非敏感的辅助信息：
 * <ul>
 *   <li><b>X-AFG-Request-Id</b> - 请求追踪 ID</li>
 *   <li><b>X-AFG-Session-Id</b> - AI 会话 ID</li>
 *   <li><b>X-AFG-Client-Ip</b> - 客户端 IP（用于审计）</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 调用方：透传认证请求头
 * Map<String, String> headers = ToolContextHeaders.propagateAuthHeaders(context, originalHeaders);
 *
 * // 服务方：从认证请求头解析用户身份
 * ToolContext context = ToolContextHeaders.extractFromHeaders(headers);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ToolContextHeaders {

    /**
     * AFG 请求头前缀。
     */
    public static final String PREFIX = "X-AFG-";

    // ========== 认证请求头（透传） ==========

    /**
     * Authorization 请求头（OAuth2/JWT）。
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Cookie 请求头。
     */
    public static final String COOKIE = "Cookie";

    /**
     * 自定义认证 Token 请求头。
     */
    public static final String X_AUTH_TOKEN = "X-Auth-Token";

    /**
     * API Key 请求头。
     */
    public static final String X_API_KEY = "X-Api-Key";

    // ========== AFG 辅助请求头 ==========

    /**
     * 请求追踪 ID 请求头。
     */
    public static final String REQUEST_ID = PREFIX + "Request-Id";

    /**
     * AI 会话 ID 请求头。
     */
    public static final String SESSION_ID = PREFIX + "Session-Id";

    /**
     * 客户端 IP 请求头（用于审计）。
     */
    public static final String CLIENT_IP = PREFIX + "Client-Ip";

    /**
     * 工具调用 ID 请求头。
     */
    public static final String TOOL_CALL_ID = PREFIX + "Tool-Call-Id";

    /**
     * 来源服务 ID 请求头。
     */
    public static final String SOURCE_SERVICE = PREFIX + "Source-Service";

    // ========== 需要透传的认证请求头列表 ==========

    /**
     * 需要透传的认证请求头。
     */
    private static final Set<String> AUTH_HEADERS_TO_PROPAGATE = Set.of(
        AUTHORIZATION,
        COOKIE,
        X_AUTH_TOKEN,
        X_API_KEY
    );

    private ToolContextHeaders() {
        // 工具类，禁止实例化
    }

    /**
     * 透传认证请求头。
     *
     * <p>从原始请求头中提取认证相关的请求头，用于远程工具调用。
     * 远程服务通过验证这些请求头获取真实的用户身份。
     *
     * @param originalHeaders 原始 HTTP 请求头
     * @return 需要透传的请求头 Map
     */
    public static @NonNull Map<String, String> propagateAuthHeaders(
            @NonNull Map<String, String> originalHeaders) {
        Map<String, String> headers = new HashMap<>();

        // 透传认证请求头
        for (String authHeader : AUTH_HEADERS_TO_PROPAGATE) {
            String value = originalHeaders.get(authHeader);
            if (value != null && !value.isBlank()) {
                headers.put(authHeader, value);
            }
        }

        return headers;
    }

    /**
     * 写入辅助信息到请求头。
     *
     * <p>仅写入非敏感的辅助信息，不包含用户身份信息。
     *
     * @param context 工具上下文
     * @return HTTP 请求头 Map
     */
    public static @NonNull Map<String, String> writeToHeaders(@NonNull ToolContext context) {
        Map<String, String> headers = new HashMap<>();

        // 仅写入辅助信息
        if (context.getRequestId() != null) {
            headers.put(REQUEST_ID, context.getRequestId());
        }
        if (context.getSessionId() != null) {
            headers.put(SESSION_ID, context.getSessionId());
        }
        if (context.getClientIp() != null) {
            headers.put(CLIENT_IP, context.getClientIp());
        }

        return headers;
    }

    /**
     * 从请求头提取辅助信息。
     *
     * <p>仅提取辅助信息，用户身份需要通过认证请求头验证获取。
     *
     * @param headers HTTP 请求头
     * @return 辅助信息 Map
     */
    public static @NonNull Map<String, String> extractAuxiliaryHeaders(@NonNull Map<String, String> headers) {
        Map<String, String> auxiliary = new HashMap<>();

        String requestId = headers.get(REQUEST_ID);
        if (requestId != null) {
            auxiliary.put("requestId", requestId);
        }

        String sessionId = headers.get(SESSION_ID);
        if (sessionId != null) {
            auxiliary.put("sessionId", sessionId);
        }

        String clientIp = headers.get(CLIENT_IP);
        if (clientIp != null) {
            auxiliary.put("clientIp", clientIp);
        }

        return auxiliary;
    }

    /**
     * 从 Spring HttpHeaders 透传认证请求头。
     *
     * @param headers Spring HttpHeaders
     * @return 需要透传的请求头 Map
     */
    public static @NonNull Map<String, String> propagateAuthHeadersFromSpring(
            org.springframework.http.@NonNull HttpHeaders headers) {
        Map<String, String> headerMap = new HashMap<>();

        for (String authHeader : AUTH_HEADERS_TO_PROPAGATE) {
            String value = headers.getFirst(authHeader);
            if (value != null && !value.isBlank()) {
                headerMap.put(authHeader, value);
            }
        }

        return headerMap;
    }

    /**
     * 判断是否包含认证请求头。
     *
     * @param headers HTTP 请求头
     * @return 如果包含认证请求头返回 true
     */
    public static boolean hasAuthHeaders(@NonNull Map<String, String> headers) {
        for (String authHeader : AUTH_HEADERS_TO_PROPAGATE) {
            String value = headers.get(authHeader);
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取认证 Token 类型。
     *
     * @param headers HTTP 请求头
     * @return Token 类型：bearer、basic、api-key、cookie、unknown、none
     */
    public static @NonNull String getAuthType(@NonNull Map<String, String> headers) {
        String authorization = headers.get(AUTHORIZATION);
        if (authorization != null) {
            if (authorization.toLowerCase().startsWith("bearer ")) {
                return "bearer";
            }
            if (authorization.toLowerCase().startsWith("basic ")) {
                return "basic";
            }
        }

        String authToken = headers.get(X_AUTH_TOKEN);
        if (authToken != null && !authToken.isBlank()) {
            return "auth-token";
        }

        String apiKey = headers.get(X_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            return "api-key";
        }

        String cookie = headers.get(COOKIE);
        if (cookie != null && !cookie.isBlank()) {
            return "cookie";
        }

        return "none";
    }
}