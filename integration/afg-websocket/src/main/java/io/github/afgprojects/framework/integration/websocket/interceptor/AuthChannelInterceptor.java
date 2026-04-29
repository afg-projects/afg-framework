package io.github.afgprojects.framework.integration.websocket.interceptor;

import java.security.Principal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * WebSocket 认证通道拦截器
 * <p>
 * 处理 WebSocket 连接的认证授权：
 * </p>
 * <ul>
 *   <li>从 STOMP CONNECT 命令获取认证信息</li>
 *   <li>设置用户主体（Principal）</li>
 *   <li>记录连接日志</li>
 * </ul>
 *
 * <h3>认证流程</h3>
 * <ol>
 *   <li>客户端发送 STOMP CONNECT 命令</li>
 *   <li>拦截器从 SecurityContext 获取认证信息</li>
 *   <li>将认证信息设置到消息头中</li>
 *   <li>后续消息可通过 Principal 获取用户信息</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;MessageMapping("/chat")
 * public void handleChat(Principal principal, ChatMessage message) {
 *     String username = principal.getName(); // 获取当前用户
 *     // ...
 * }
 * </pre>
 */
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    /**
     * STOMP Authorization 请求头
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    @Nullable
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        switch (command) {
            case CONNECT:
                handleConnect(accessor);
                break;
            case DISCONNECT:
                handleDisconnect(accessor);
                break;
            case SUBSCRIBE:
                handleSubscribe(accessor);
                break;
            case SEND:
                handleSend(accessor);
                break;
            default:
                break;
        }

        return message;
    }

    /**
     * 处理 CONNECT 命令
     * <p>
     * 从 SecurityContext 获取认证信息并设置到会话中。
     * </p>
     *
     * @param accessor STOMP 头访问器
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        // 尝试从 SecurityContext 获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            // 设置用户主体
            accessor.setUser(authentication);

            log.info("WebSocket CONNECT: user={}, sessionId={}",
                    authentication.getName(), accessor.getSessionId());
        } else {
            // 尝试从请求头获取 token（如果使用 token 认证）
            String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Bearer token 存在，可以在此处验证 token
                log.debug("WebSocket CONNECT with Bearer token: sessionId={}", accessor.getSessionId());
            }

            log.debug("WebSocket CONNECT: anonymous, sessionId={}", accessor.getSessionId());
        }
    }

    /**
     * 处理 DISCONNECT 命令
     *
     * @param accessor STOMP 头访问器
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        log.info("WebSocket DISCONNECT: user={}, sessionId={}",
                user != null ? user.getName() : "anonymous",
                accessor.getSessionId());
    }

    /**
     * 处理 SUBSCRIBE 命令
     *
     * @param accessor STOMP 头访问器
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        log.debug("WebSocket SUBSCRIBE: user={}, destination={}, sessionId={}",
                user != null ? user.getName() : "anonymous",
                destination,
                accessor.getSessionId());
    }

    /**
     * 处理 SEND 命令
     *
     * @param accessor STOMP 头访问器
     */
    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        log.debug("WebSocket SEND: user={}, destination={}, sessionId={}",
                user != null ? user.getName() : "anonymous",
                destination,
                accessor.getSessionId());
    }
}