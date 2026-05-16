package io.github.afgprojects.framework.security.auth.audit.alert;

import io.github.afgprojects.framework.security.core.audit.SecurityEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志告警通道。
 *
 * <p>通过日志输出告警信息，作为默认的告警通道实现。
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class LogAlertChannel implements AlertChannel {

    @Override
    public String getType() {
        return "log";
    }

    @Override
    public void send(SecurityEventService.SecurityEventInfo event) {
        log.warn("[SECURITY ALERT] Type: {}, User: {}, IP: {}, Details: {}",
            event.getEventType(), event.getUserId(), event.getIp(), event.getDetails());
    }
}
