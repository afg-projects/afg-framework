package io.github.afgprojects.framework.core.api.sse;

import java.util.Collections;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * NoOp SSE 连接管理器实现
 * <p>
 * 本地降级实现，所有连接管理操作均为空操作。
 * {@code createConnection} 返回默认 SseEmitter（0 timeout，即立即完成），
 * {@code sendEvent}/{@code sendEventToAll} 不发送任何事件，
 * {@code isConnected} 总是返回 {@code false}，
 * {@code getActiveConnectionCount} 总是返回 0，
 * {@code getActiveConnectionIds} 总是返回空列表。
 * </p>
 * <p>
 * 由 {@code SseAutoConfiguration} 在无其他 {@link SseConnectionManager} 实现时自动注册。
 * 适用于不需要 SSE 功能的场景。
 * </p>
 *
 * @since 1.0.0
 */
public class NoOpSseConnectionManager implements SseConnectionManager {

    @Override
    public SseEmitter createConnection(String clientId) {
        // 返回 0 timeout SseEmitter，即立即完成，不保持长连接
        return new SseEmitter(0L);
    }

    @Override
    public void sendEvent(String clientId, SseEmitter.SseEventBuilder event) {
        // no-op: 不发送任何事件
    }

    @Override
    public void sendEventToAll(SseEmitter.SseEventBuilder event) {
        // no-op: 不发送任何事件
    }

    @Override
    public void closeConnection(String clientId) {
        // no-op: 没有连接需要关闭
    }

    @Override
    public boolean isConnected(String clientId) {
        return false;
    }

    @Override
    public int getActiveConnectionCount() {
        return 0;
    }

    @Override
    public List<String> getActiveConnectionIds() {
        return Collections.emptyList();
    }
}
