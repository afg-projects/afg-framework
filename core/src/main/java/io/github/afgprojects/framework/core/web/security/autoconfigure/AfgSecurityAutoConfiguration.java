package io.github.afgprojects.framework.core.web.security.autoconfigure;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
@EnableConfigurationProperties(AfgSecurityProperties.class)
public class AfgSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EnhancedInputSanitizer enhancedInputSanitizer(AfgSecurityProperties properties) {
        return new EnhancedInputSanitizer(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public XssFilter xssFilter(EnhancedInputSanitizer sanitizer) {
        return new XssFilter(sanitizer);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "afg.security.sql-injection",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    public SqlInjectionFilter sqlInjectionFilter(EnhancedInputSanitizer sanitizer) {
        return new SqlInjectionFilter(sanitizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityHeaderFilter securityHeaderFilter(AfgSecurityProperties properties) {
        SecurityHeaderFilter filter = new SecurityHeaderFilter();
        AfgSecurityProperties.SecurityHeaderConfig config = properties.getSecurityHeader();
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
