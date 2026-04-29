package io.github.afgprojects.framework.core.autoconfigure;

import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.web.logging.LoggingProperties;
import io.github.afgprojects.framework.core.web.logging.MdcFilter;

/**
 * 日志自动配置类
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.logging.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MdcFilter mdcFilter(LoggingProperties properties) {
        return new MdcFilter(properties);
    }
}
