package io.github.afgprojects.framework.core.web.i18n;

import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

import io.github.afgprojects.framework.core.model.exception.ErrorCodeMessageSourceConfig;

/**
 * 国际化自动配置
 * 配置 MessageSource 和 LocaleFilter
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
public class LocaleAutoConfiguration {

    /**
     * 配置 MessageSource
     * 加载 messages.properties 资源文件
     */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        // 设置默认消息，当找不到对应错误码时使用
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * 配置 LocaleFilter
     * 自动解析 Accept-Language 头并设置 LocaleContextHolder
     */
    @Bean("afgLocaleFilter")
    @ConditionalOnMissingBean
    public LocaleFilter localeFilter() {
        return new LocaleFilter();
    }

    /**
     * 配置 ErrorCodeMessageSourceConfig
     * 将 MessageSource 注入到 ErrorCodeMessageSource
     */
    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    public ErrorCodeMessageSourceConfig errorCodeMessageSourceConfig(MessageSource messageSource) {
        return new ErrorCodeMessageSourceConfig(messageSource);
    }
}