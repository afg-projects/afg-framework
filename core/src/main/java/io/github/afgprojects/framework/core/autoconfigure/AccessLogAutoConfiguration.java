package io.github.afgprojects.framework.core.autoconfigure;

import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.logging.AccessLogFilter;

/**
 * 访问日志自动配置
 * <p>
 * 在 Web 环境下自动注册 {@link AccessLogFilter}，记录所有 HTTP 请求的访问日志。
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     access-log:
 *       enabled: true
 *       exclude-paths: /health,/actuator/**
 *       include-query-string: true
 *       include-client-ip: true
 *       slow-request-threshold: 3000
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@ConditionalOnProperty(prefix = "afg.core.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class AccessLogAutoConfiguration {

    /**
     * 注册访问日志过滤器。
     *
     * @param properties 核心配置属性
     * @return AccessLogFilter 实例
     */
    @Bean("afgAccessLogFilter")
    @ConditionalOnMissingBean(AccessLogFilter.class)
    public AccessLogFilter accessLogFilter(AfgCoreProperties properties) {
        return new AccessLogFilter(properties);
    }
}
