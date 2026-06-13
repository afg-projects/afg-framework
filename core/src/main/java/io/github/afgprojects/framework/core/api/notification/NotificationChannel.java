package io.github.afgprojects.framework.core.api.notification;

/**
 * 通知渠道枚举
 * <p>
 * 定义支持的通知渠道类型。邮件/短信/IM 等真实渠道的实现由集成模块提供，
 * Core 模块仅定义 SPI 接口和日志/NoOp 降级实现。
 *
 * @since 1.0.0
 */
public enum NotificationChannel {

    /**
     * 邮件通知
     */
    EMAIL,

    /**
     * 短信通知
     */
    SMS,

    /**
     * 站内信通知
     */
    IN_APP,

    /**
     * Webhook 回调通知
     */
    WEBHOOK,

    /**
     * 钉钉通知
     */
    DINGTALK,

    /**
     * 飞书通知
     */
    FEISHU,

    /**
     * 企业微信通知
     */
    WECOM
}
