package io.github.afgprojects.framework.security.auth.properties.audit;

import java.util.List;

import lombok.Data;

/**
 * 告警配置。
 */
@Data
public class AlertConfig {

    /**
     * 是否启用登录失败告警。
     */
    private boolean loginFailureAlert = true;

    /**
     * 登录失败阈值，超过此值触发告警。
     */
    private int loginFailureThreshold = 5;

    /**
     * 是否启用新设备登录告警。
     */
    private boolean newDeviceAlert = true;

    /**
     * 是否启用新位置登录告警。
     */
    private boolean newLocationAlert = true;

    /**
     * 是否启用可疑 IP 告警。
     */
    private boolean suspiciousIpAlert = true;

    /**
     * 告警通道配置列表。
     */
    private List<AlertChannelConfig> channels = List.of(new AlertChannelConfig("log"));
}
