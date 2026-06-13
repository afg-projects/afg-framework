package io.github.afgprojects.framework.core.autoconfigure;

import jakarta.servlet.Servlet;
import jakarta.validation.Validator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.exception.GlobalExceptionHandler;

/**
 * Bean Validation 自动配置
 * <p>
 * 在 Web 环境下自动注册 {@link GlobalExceptionHandler}（统一异常处理）
 * 和 {@link MethodValidationPostProcessor}（方法级参数校验）。
 * <p>
 * {@link GlobalExceptionHandler} 已处理以下验证异常：
 * <ul>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException} — @Valid on request body</li>
 *   <li>{@link org.springframework.validation.BindException} — @Valid on form data</li>
 *   <li>{@link jakarta.validation.ConstraintViolationException} — @Validated on method parameters</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     validation:
 *       enabled: true
 *       include-field-errors: true
 *       default-error-message: "参数校验失败"
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Servlet.class, Validator.class})
@ConditionalOnProperty(prefix = "afg.core.validation", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class ValidationAutoConfiguration {

    /**
     * 注册全局异常处理器。
     * <p>
     * 条件注册，允许业务应用通过定义自己的 {@link GlobalExceptionHandler} Bean 来覆盖。
     *
     * @return GlobalExceptionHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册方法级校验后置处理器。
     * <p>
     * 使 {@code @Validated} 注解在 Service 方法参数上生效，
     * 校验失败抛出 {@link jakarta.validation.ConstraintViolationException}。
     * <p>
     * 条件注册，如果应用已定义则不覆盖。
     *
     * @return MethodValidationPostProcessor 实例
     */
    @Bean
    @ConditionalOnMissingBean(MethodValidationPostProcessor.class)
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
