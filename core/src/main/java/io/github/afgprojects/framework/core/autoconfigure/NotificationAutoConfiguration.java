package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.notification.LogNotificationService;
import io.github.afgprojects.framework.core.api.notification.NoOpNotificationService;
import io.github.afgprojects.framework.core.api.notification.NotificationService;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 通知服务自动配置
 * <p>
 * 自动配置通知服务功能，默认使用 {@link LogNotificationService}（日志输出）。
 * 真实渠道实现（邮件、短信、钉钉等）由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     notification:
 *       enabled: true
 *       default-channel: EMAIL
 *       log-notifications: true
 *       retry-count: 3
 *       retry-interval-ms: 1000
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.notification", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class NotificationAutoConfiguration {

    /**
     * 日志通知服务
     * <p>
     * 当没有邮件/短信等真实渠道实现时，将通知内容记录到日志。
     * 适用于开发和测试环境，生产环境应替换为真实渠道实现。
     *
     * @return 日志通知服务实例
     */
    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    @ConditionalOnProperty(prefix = "afg.core.notification", name = "log-notifications", havingValue = "true", matchIfMissing = true)
    public NotificationService logNotificationService() {
        return new LogNotificationService();
    }

    /**
     * NoOp 通知服务降级实现
     * <p>
     * 当不需要任何通知功能且日志通知也不启用时使用。
     * 所有通知操作返回成功结果但不实际发送。
     *
     * @return NoOp 通知服务实例
     */
    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    public NotificationService noOpNotificationService() {
        return new NoOpNotificationService();
    }
}
