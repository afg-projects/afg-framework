package io.github.afgprojects.framework.core.autoconfigure;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.security.AfgEnforcer;
import io.github.afgprojects.framework.core.web.security.AfgSecurityConfiguration;
import io.github.afgprojects.framework.core.web.security.AfgSecurityConfigurer;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;
import io.github.afgprojects.framework.core.web.security.DefaultAfgSecurityContextBridge;
import io.github.afgprojects.framework.core.web.security.filter.SecurityHeaderFilter;
import io.github.afgprojects.framework.core.web.security.filter.SqlInjectionFilter;
import io.github.afgprojects.framework.core.web.security.filter.XssFilter;
import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;

/**
 * AFG 安全自动配置
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@ConditionalOnProperty(prefix = "afg.core.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class AfgSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.core.security.input-sanitizer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EnhancedInputSanitizer enhancedInputSanitizer(AfgCoreProperties properties) {
        return new EnhancedInputSanitizer(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.core.security.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    @ConditionalOnBean(EnhancedInputSanitizer.class)
    public XssFilter xssFilter(EnhancedInputSanitizer sanitizer) {
        return new XssFilter(sanitizer);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "afg.core.security.sql-injection",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    @ConditionalOnBean(EnhancedInputSanitizer.class)
    public SqlInjectionFilter sqlInjectionFilter(EnhancedInputSanitizer sanitizer) {
        return new SqlInjectionFilter(sanitizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityHeaderFilter securityHeaderFilter(AfgCoreProperties properties) {
        SecurityHeaderFilter filter = new SecurityHeaderFilter();
        AfgCoreProperties.SecurityConfig.SecurityHeaderConfig config = properties.getSecurity().getSecurityHeader();
        if (config.getContentSecurityPolicy() != null) {
            filter.setContentSecurityPolicy(config.getContentSecurityPolicy());
        }
        if (config.getStrictTransportSecurity() != null) {
            filter.setStrictTransportSecurity(config.getStrictTransportSecurity());
        }
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean
    public AfgSecurityContextBridge afgSecurityContextBridge() {
        return new DefaultAfgSecurityContextBridge();
    }

    @Bean
    @ConditionalOnMissingBean
    public AfgEnforcer afgEnforcer() {
        return (context, resource, action) -> false;
    }

    @Bean
    public AfgSecurityConfiguration afgSecurityConfiguration(List<AfgSecurityConfigurer> configurers) {
        AfgSecurityConfiguration config = new AfgSecurityConfiguration();
        configurers.stream()
                .sorted(Comparator.comparingInt(AfgSecurityConfigurer::getOrder))
                .forEach(c -> c.configure(config));
        return config;
    }
}
