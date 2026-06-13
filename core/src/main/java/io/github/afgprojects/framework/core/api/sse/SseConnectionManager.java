package io.github.afgprojects.framework.core.api.sse;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接管理器接口
 * <p>
 * 定义统一的 SSE（Server-Sent Events）连接管理接口，支持多种连接管理后端。
 * 核心语义：{@code createConnection} 创建 SSE 连接，{@code sendEvent} 向指定客户端发送事件，
 * {@code sendEventToAll} 广播事件到所有活跃连接。
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private SseConnectionManager sseManager;
 *
 * // 创建 SSE 连接
 * @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 * public SseEmitter events(String userId) {
 *     return sseManager.createConnection(userId);
 * }
 *
 * // 向指定客户端发送事件
 * sseManager.sendEvent(userId, SseEmitter.event().name("message").data("Hello"));
 *
 * // 广播事件到所有连接
 * sseManager.sendEventToAll(SseEmitter.event().name("notification").data("System update"));
 * }</pre>
 *
 * @since 1.0.0
 */
public interface SseConnectionManager {

    /**
     * 创建 SSE 连接
     * <p>
     * 为指定客户端创建一个新的 SSE 连接。如果该客户端已有连接，旧连接将被关闭。
     * </p>
     *
     * @param clientId 客户端标识（如用户 ID、会话 ID）
     * @return SSE 发射器实例
     */
    SseEmitter createConnection(String clientId);

    /**
     * 向指定客户端发送事件
     * <p>
     * 如果客户端不存在或连接已断开，操作将被忽略。
     * 发送失败时自动关闭并移除该连接。
     * </p>
     *
     * @param clientId 客户端标识
     * @param event    SSE 事件构建器
     */
    void sendEvent(String clientId, SseEmitter.SseEventBuilder event);

    /**
     * 广播事件到所有活跃连接
     * <p>
     * 遍历所有活跃连接发送事件，发送失败的连接将被自动关闭并移除。
     * </p>
     *
     * @param event SSE 事件构建器
     */
    void sendEventToAll(SseEmitter.SseEventBuilder event);

    /**
     * 关闭指定客户端的连接
     * <p>
     * 关闭并移除指定客户端的 SSE 连接。如果客户端不存在，操作无效。
     * </p>
     *
     * @param clientId 客户端标识
     */
    void closeConnection(String clientId);

    /**
     * 检查指定客户端是否已连接
     *
     * @param clientId 客户端标识
     * @return 如果客户端存在活跃连接返回 {@code true}
     */
    boolean isConnected(String clientId);

    /**
     * 获取当前活跃连接数
     *
     * @return 活跃连接数
     */
    int getActiveConnectionCount();

    /**
     * 获取所有活跃连接的客户端 ID 列表
     *
     * @return 活跃客户端 ID 列表
     */
    List<String> getActiveConnectionIds();
}
