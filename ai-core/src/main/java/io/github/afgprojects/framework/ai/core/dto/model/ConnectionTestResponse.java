package io.github.afgprojects.framework.ai.core.dto.model;

import lombok.Builder;
import lombok.Data;

/**
 * 模型连接测试响应
 */
@Data
@Builder
public class ConnectionTestResponse {

    private boolean success;
    private String message;
    private String modelName;
    private Long responseTime;

    public static ConnectionTestResponse success(String modelName, long responseTime) {
        return ConnectionTestResponse.builder()
                .success(true)
                .message("连接成功")
                .modelName(modelName)
                .responseTime(responseTime)
                .build();
    }

    public static ConnectionTestResponse failure(String message) {
        return ConnectionTestResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
