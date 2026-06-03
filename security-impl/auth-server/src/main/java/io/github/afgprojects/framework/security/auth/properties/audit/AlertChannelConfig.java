package io.github.afgprojects.framework.security.auth.properties.audit;

import java.util.List;

import lombok.Data;

/**
 * 告警通道配置。
 */
@Data
public class AlertChannelConfig {

    /**
     * 通道类型。
     */
    private String type;

    /**
     * 接收者列表。
     */
    private List<String> recipients;

    public AlertChannelConfig() {
    }

    public AlertChannelConfig(String type) {
        this.type = type;
    }
}
