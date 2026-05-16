package io.github.afgprojects.framework.security.auth.audit.config;

import io.github.afgprojects.framework.security.auth.audit.alert.AlertChannel;
import io.github.afgprojects.framework.security.auth.audit.alert.LogAlertChannel;
import io.github.afgprojects.framework.security.auth.audit.service.DefaultAlertService;
import io.github.afgprojects.framework.security.core.audit.AlertService;
import io.github.afgprojects.framework.security.core.audit.SecurityEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 审计自动配置类。
 *
 * <p>配置审计服务的核心组件：
 * <ul>
 *   <li>{@link AlertService} - 告警服务</li>
 *   <li>{@link AlertChannel} - 告警通道（默认提供日志通道）</li>
 * </ul>
 *
 * <p>支持通过实现 {@link AlertChannel} 接口扩展自定义告警通道。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(prefix = "afg.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    /**
     * 创建日志告警通道。
     *
     * <p>作为默认的告警通道实现，通过日志输出告警信息。
     *
     * @return LogAlertChannel 实例
     */
    @Bean
    @ConditionalOnMissingBean(AlertChannel.class)
    public LogAlertChannel logAlertChannel() {
        log.info("Initializing LogAlertChannel");
        return new LogAlertChannel();
    }

    /**
     * 创建告警服务。
     *
     * <p>根据安全事件类型和配置规则，通过配置的告警通道发送告警。
     *
     * @param auditProperties 审计配置属性
     * @param securityEventService 安全事件服务
     * @param alertChannels 告警通道列表（自动注入）
     * @return AlertService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SecurityEventService.class)
    public AlertService alertService(
            AuditProperties auditProperties,
            SecurityEventService securityEventService,
            @Autowired(required = false) List<AlertChannel> alertChannels) {
        if (alertChannels == null || alertChannels.isEmpty()) {
            log.warn("No AlertChannel beans found, alert service may not function properly");
        }
        log.info("Initializing DefaultAlertService");
        return new DefaultAlertService(auditProperties, securityEventService,
            alertChannels != null ? alertChannels : List.of());
    }
}
