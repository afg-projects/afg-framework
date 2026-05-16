package io.github.afgprojects.framework.security.auth.audit.alert;

import io.github.afgprojects.framework.security.core.audit.SecurityEventService;

/**
 * 告警通道接口。
 *
 * <p>定义告警发送的统一接口，支持多种告警通道实现（如日志、邮件、短信等）。
 *
 * @since 1.0.0
 */
public interface AlertChannel {

    /**
     * 获取通道类型标识。
     *
     * @return 通道类型，如 "log"、"email"、"sms" 等
     */
    String getType();

    /**
     * 发送告警。
     *
     * @param event 安全事件信息
     */
    void send(SecurityEventService.SecurityEventInfo event);
}
