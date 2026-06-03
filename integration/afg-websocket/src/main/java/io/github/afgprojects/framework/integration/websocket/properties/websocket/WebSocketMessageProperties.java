package io.github.afgprojects.framework.integration.websocket.properties.websocket;

import lombok.Data;

/**
 * WebSocket 消息配置。
 */
@Data
public class WebSocketMessageProperties {

    /**
     * 消息缓冲区大小限制（字节）
     */
    private int bufferSizeLimit = 64 * 1024;

    /**
     * 消息处理时间限制（秒）
     */
    private int timeLimitSeconds = 10;
}