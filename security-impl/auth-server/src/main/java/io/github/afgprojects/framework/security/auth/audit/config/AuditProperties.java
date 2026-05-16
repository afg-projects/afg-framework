package io.github.afgprojects.framework.security.auth.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 审计配置属性。
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.audit")
public class AuditProperties {

    /**
     * 是否启用审计功能。
     */
    private boolean enabled = true;

    /**
     * 告警配置。
     */
    private AlertConfig alert = new AlertConfig();

    /**
     * 告警配置。
     */
    @Data
    public static class AlertConfig {

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

    /**
     * 告警通道配置。
     */
    @Data
    public static class AlertChannelConfig {

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
}
