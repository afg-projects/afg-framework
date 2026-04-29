package io.github.afgprojects.framework.integration.websocket.handler;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理 WebSocket 会话，提供：
 * </p>
 * <ul>
 *   <li>在线用户管理</li>
 *   <li>会话跟踪</li>
 *   <li>订阅管理</li>
 * </ul>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>线程安全的会话存储</li>
 *   <li>支持同一用户多会话</li>
 *   <li>会话事件监听</li>
 *   <li>订阅目的地跟踪</li>
 * </ul>
 */
@Component
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    /**
     * 用户会话映射：username -> sessionIds
     */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 会话用户映射：sessionId -> username
     */
    private final Map<String, String> sessionUser = new ConcurrentHashMap<>();

    /**
     * 会话订阅映射：sessionId -> destinations
     */
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    // ==================== Event Listeners ====================

    /**
     * 处理连接事件
     *
     * @param event 连接事件
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();

        if (sessionId != null) {
            String username = user != null ? user.getName() : "anonymous";
            sessionUser.put(sessionId, username);
            userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            log.info("WebSocket connected: username={}, sessionId={}, onlineUsers={}",
                    username, sessionId, userSessions.size());
        }
    }

    /**
     * 处理断开连接事件
     *
     * @param event 断开连接事件
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        if (sessionId != null) {
            String username = sessionUser.remove(sessionId);
            if (username != null) {
                Set<String> sessions = userSessions.get(username);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        userSessions.remove(username);
                    }
                }
            }

            // 清理订阅
            sessionSubscriptions.remove(sessionId);

            log.info("WebSocket disconnected: username={}, sessionId={}, onlineUsers={}",
                    username, sessionId, userSessions.size());
        }
    }

    /**
     * 处理订阅事件
     *
     * @param event 订阅事件
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (sessionId != null && destination != null) {
            sessionSubscriptions
                    .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                    .add(destination);

            log.debug("WebSocket subscribed: sessionId={}, destination={}", sessionId, destination);
        }
    }

    /**
     * 处理取消订阅事件
     *
     * @param event 取消订阅事件
     */
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (sessionId != null && subscriptionId != null) {
            Set<String> destinations = sessionSubscriptions.get(sessionId);
            if (destinations != null) {
                // 注意：这里简化处理，实际可能需要根据 subscriptionId 映射到 destination
                log.debug("WebSocket unsubscribed: sessionId={}, subscriptionId={}", sessionId, subscriptionId);
            }
        }
    }

    // ==================== Query Methods ====================

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 获取在线会话数量
     *
     * @return 在线会话数量
     */
    public int getOnlineSessionCount() {
        return sessionUser.size();
    }

    /**
     * 判断用户是否在线
     *
     * @param username 用户名
     * @return 是否在线
     */
    public boolean isUserOnline(@NonNull String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 获取用户的会话ID列表
     *
     * @param username 用户名
     * @return 会话ID集合
     */
    @Nullable
    public Set<String> getUserSessions(@NonNull String username) {
        return userSessions.get(username);
    }

    /**
     * 获取会话对应的用户名
     *
     * @param sessionId 会话ID
     * @return 用户名
     */
    @Nullable
    public String getSessionUser(@NonNull String sessionId) {
        return sessionUser.get(sessionId);
    }

    /**
     * 获取会话订阅的目的地列表
     *
     * @param sessionId 会话ID
     * @return 目的地集合
     */
    @Nullable
    public Set<String> getSessionSubscriptions(@NonNull String sessionId) {
        return sessionSubscriptions.get(sessionId);
    }

    /**
     * 获取所有在线用户
     *
     * @return 在线用户集合
     */
    public Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }
}
