package io.github.afgprojects.framework.core.model.exception;

import io.github.afgprojects.framework.core.web.i18n.LocaleAutoConfiguration;
import org.springframework.context.MessageSource;

import lombok.RequiredArgsConstructor;

/**
 * 错误码消息源配置器
 * 在 Spring 容器启动时将 MessageSource 注入到 ErrorCodeMessageSource
 *
 * <p>此类的实例化由 {@link LocaleAutoConfiguration} 管理
 */
@RequiredArgsConstructor
public class ErrorCodeMessageSourceConfig {

    private final MessageSource messageSource;

    /**
     * 初始化 MessageSource
     * 由 Spring 容器在创建 Bean 后调用
     */
    public void init() {
        ErrorCodeMessageSource.setMessageSource(messageSource);
    }
}
