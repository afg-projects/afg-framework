package io.github.afgprojects.framework.core.api.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 本地内存 SSE 连接管理器实现
 * <p>
 * 使用 {@link ConcurrentHashMap} 管理所有 SSE 连接，适用于单机部署或降级场景。
 * 支持 timeout 配置、最大连接数限制、连接超时/完成/错误回调自动清理。
 * </p>
 * <p>
 * 注意：此实现仅对单实例有效，多实例部署时需要使用 Redis 等分布式实现。
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class LocalSseConnectionManager implements SseConnectionManager {

    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();
    private final AfgCoreProperties.SseConfig config;

    /**
     * 构造函数
     *
     * @param properties 核心配置属性（从中获取 SSE 配置）
     */
    public LocalSseConnectionManager(AfgCoreProperties properties) {
        this.config = properties.getSse();
    }

    @Override
    public SseEmitter createConnection(String clientId) {
        // 检查连接数限制
        if (connections.size() >= config.getMaxConnections()) {
            throw new BusinessException(CommonErrorCode.RATE_LIMIT_EXCEEDED,
                    new Object[]{"SSE 连接数已达上限: " + config.getMaxConnections()});
        }

        // 关闭已有连接
        SseEmitter existingEmitter = connections.remove(clientId);
        if (existingEmitter != null) {
            existingEmitter.complete();
            log.debug("Closed existing SSE connection for client: {}", clientId);
        }

        // 创建新的 SseEmitter
        long timeout = config.getTimeout();
        SseEmitter emitter = new SseEmitter(timeout);

        // 注册回调清理连接
        emitter.onCompletion(() -> {
            connections.remove(clientId);
            log.debug("SSE connection completed for client: {}", clientId);
        });
        emitter.onTimeout(() -> {
            connections.remove(clientId);
            log.debug("SSE connection timed out for client: {}", clientId);
        });
        emitter.onError(ex -> {
            connections.remove(clientId);
            log.debug("SSE connection error for client: {}", clientId, ex);
        });

        // 放入连接映射
        connections.put(clientId, emitter);
        log.debug("Created SSE connection for client: {}, timeout: {}ms", clientId, timeout);

        return emitter;
    }

    @Override
    public void sendEvent(String clientId, SseEmitter.SseEventBuilder event) {
        SseEmitter emitter = connections.get(clientId);
        if (emitter == null) {
            log.debug("No active SSE connection for client: {}", clientId);
            return;
        }
        try {
            emitter.send(event);
        } catch (IOException e) {
            log.warn("Failed to send SSE event to client: {}", clientId, e);
            closeConnection(clientId);
        }
    }

    @Override
    public void sendEventToAll(SseEmitter.SseEventBuilder event) {
        List<String> failedClients = new ArrayList<>();
        for (Map.Entry<String, SseEmitter> entry : connections.entrySet()) {
            try {
                entry.getValue().send(event);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to client: {}", entry.getKey(), e);
                failedClients.add(entry.getKey());
            }
        }
        // 清理发送失败的连接
        for (String clientId : failedClients) {
            closeConnection(clientId);
        }
    }

    @Override
    public void closeConnection(String clientId) {
        SseEmitter emitter = connections.remove(clientId);
        if (emitter != null) {
            emitter.complete();
            log.debug("Closed SSE connection for client: {}", clientId);
        }
    }

    @Override
    public boolean isConnected(String clientId) {
        return connections.containsKey(clientId);
    }

    @Override
    public int getActiveConnectionCount() {
        return connections.size();
    }

    @Override
    public List<String> getActiveConnectionIds() {
        return new ArrayList<>(connections.keySet());
    }
}
