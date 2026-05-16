package io.github.afgprojects.framework.security.auth.audit.service;

import io.github.afgprojects.framework.security.auth.audit.alert.AlertChannel;
import io.github.afgprojects.framework.security.auth.audit.config.AuditProperties;
import io.github.afgprojects.framework.security.core.audit.AlertService;
import io.github.afgprojects.framework.security.core.audit.SecurityEventService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认告警服务实现。
 *
 * <p>根据安全事件类型和配置规则，通过配置的告警通道发送告警。
 *
 * @since 1.0.0
 */
@Slf4j
@Service
public class DefaultAlertService implements AlertService {

    private final AuditProperties auditProperties;
    private final SecurityEventService securityEventService;
    private final Map<String, AlertChannel> alertChannelMap;

    public DefaultAlertService(
            AuditProperties auditProperties,
            SecurityEventService securityEventService,
            List<AlertChannel> alertChannels) {
        this.auditProperties = auditProperties;
        this.securityEventService = securityEventService;
        this.alertChannelMap = alertChannels.stream()
            .collect(Collectors.toMap(AlertChannel::getType, Function.identity()));
        log.info("Initialized DefaultAlertService with {} channels: {}",
            alertChannelMap.size(), alertChannelMap.keySet());
    }

    @Override
    public void checkAndAlert(SecurityEventService.@NonNull SecurityEventInfo event) {
        if (!auditProperties.isEnabled()) {
            return;
        }

        AuditProperties.AlertConfig alertConfig = auditProperties.getAlert();
        String eventType = event.getEventType();

        // 检查登录失败告警
        if (alertConfig.isLoginFailureAlert() && isLoginFailureEvent(eventType)) {
            checkLoginFailureAlert(event, alertConfig);
        }

        // 检查新设备告警
        if (alertConfig.isNewDeviceAlert() && isNewDeviceEvent(eventType)) {
            sendAlertToChannels(event, alertConfig.getChannels());
        }

        // 检查新位置告警
        if (alertConfig.isNewLocationAlert() && isNewLocationEvent(eventType)) {
            sendAlertToChannels(event, alertConfig.getChannels());
        }

        // 检查可疑 IP 告警
        if (alertConfig.isSuspiciousIpAlert() && isSuspiciousIpEvent(eventType)) {
            sendAlertToChannels(event, alertConfig.getChannels());
        }
    }

    @Override
    public void sendAlert(SecurityEventService.@NonNull SecurityEventInfo event, @NonNull List<String> channels) {
        for (String channelType : channels) {
            AlertChannel channel = alertChannelMap.get(channelType);
            if (channel != null) {
                try {
                    channel.send(event);
                    log.debug("Alert sent via channel: {} for event: {}", channelType, event.getEventType());
                } catch (Exception e) {
                    log.error("Failed to send alert via channel: {}", channelType, e);
                }
            } else {
                log.warn("Alert channel not found: {}", channelType);
            }
        }
    }

    /**
     * 检查登录失败告警。
     */
    private void checkLoginFailureAlert(SecurityEventService.SecurityEventInfo event, AuditProperties.AlertConfig alertConfig) {
        String userId = event.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }

        // 获取最近的登录失败事件
        List<SecurityEventService.SecurityEventInfo> recentFailures =
            securityEventService.getEventsByType("LOGIN_FAILURE", Duration.ofHours(1));

        // 统计该用户的失败次数
        long failureCount = recentFailures.stream()
            .filter(e -> userId.equals(e.getUserId()))
            .count();

        if (failureCount >= alertConfig.getLoginFailureThreshold()) {
            log.warn("Login failure threshold exceeded for user: {}, count: {}", userId, failureCount);
            sendAlertToChannels(event, alertConfig.getChannels());
        }
    }

    /**
     * 发送告警到配置的通道。
     */
    private void sendAlertToChannels(SecurityEventService.SecurityEventInfo event,
                                     List<AuditProperties.AlertChannelConfig> channelConfigs) {
        for (AuditProperties.AlertChannelConfig channelConfig : channelConfigs) {
            AlertChannel channel = alertChannelMap.get(channelConfig.getType());
            if (channel != null) {
                try {
                    channel.send(event);
                    log.debug("Alert sent via channel: {} for event: {}", channelConfig.getType(), event.getEventType());
                } catch (Exception e) {
                    log.error("Failed to send alert via channel: {}", channelConfig.getType(), e);
                }
            } else {
                log.warn("Alert channel not found: {}", channelConfig.getType());
            }
        }
    }

    /**
     * 判断是否为登录失败事件。
     */
    private boolean isLoginFailureEvent(String eventType) {
        return "LOGIN_FAILURE".equals(eventType) ||
               "AUTHENTICATION_FAILURE".equals(eventType);
    }

    /**
     * 判断是否为新设备事件。
     */
    private boolean isNewDeviceEvent(String eventType) {
        return "NEW_DEVICE_LOGIN".equals(eventType);
    }

    /**
     * 判断是否为新位置事件。
     */
    private boolean isNewLocationEvent(String eventType) {
        return "NEW_LOCATION_LOGIN".equals(eventType);
    }

    /**
     * 判断是否为可疑 IP 事件。
     */
    private boolean isSuspiciousIpEvent(String eventType) {
        return "SUSPICIOUS_IP".equals(eventType) ||
               "BLOCKED_IP_ATTEMPT".equals(eventType);
    }
}
