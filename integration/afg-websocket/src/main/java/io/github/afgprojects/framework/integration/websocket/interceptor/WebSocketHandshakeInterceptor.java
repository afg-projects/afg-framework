package io.github.afgprojects.framework.integration.websocket.interceptor;

import java.util.Map;

import io.github.afgprojects.framework.core.web.context.RequestContext;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 握手拦截器
 * <p>
 * 在握手阶段将请求上下文信息传递到 WebSocket 会话属性中，
 * 便于后续消息处理时获取用户身份信息。
 * </p>
 *
 * <h3>传递的信息</h3>
 * <ul>
 *   <li>traceId - 链路追踪ID</li>
 *   <li>userId - 用户ID</li>
 *   <li>username - 用户名</li>
 *   <li>tenantId - 租户ID</li>
 *   <li>clientIp - 客户端IP</li>
 * </ul>
 */
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    // WebSocket 会话属性键
    public static final String ATTR_TRACE_ID = "traceId";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_TENANT_ID = "tenantId";
    public static final String ATTR_CLIENT_IP = "clientIp";

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {

        // 从请求上下文获取用户信息
        RequestContext context = AfgRequestContextHolder.getContext();

        if (context != null) {
            // 将上下文信息存入 WebSocket 会话属性
            if (context.getTraceId() != null) {
                attributes.put(ATTR_TRACE_ID, context.getTraceId());
            }
            if (context.getUserId() != null) {
                attributes.put(ATTR_USER_ID, context.getUserId());
            }
            if (context.getUsername() != null) {
                attributes.put(ATTR_USERNAME, context.getUsername());
            }
            if (context.getTenantId() != null) {
                attributes.put(ATTR_TENANT_ID, context.getTenantId());
            }
            if (context.getClientIp() != null) {
                attributes.put(ATTR_CLIENT_IP, context.getClientIp());
            }

            log.debug("WebSocket handshake: traceId={}, userId={}, username={}",
                    context.getTraceId(), context.getUserId(), context.getUsername());
        }

        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.warn("WebSocket handshake failed: {}", exception.getMessage());
        }
    }
}