package io.github.afgprojects.framework.core.web.version;

import jakarta.servlet.Servlet;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 版本管理自动配置类
 *
 * <p>配置示例:
 * <pre>{@code
 * afg:
 *   api-version:
 *     enabled: true
 *     default-version: "1.0.0"
 *     header-name: "X-API-Version"
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@ConditionalOnProperty(prefix = "afg.api-version", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiVersionProperties.class)
public class ApiVersionAutoConfiguration implements WebMvcConfigurer {

    private final ApiVersionProperties properties;

    public ApiVersionAutoConfiguration(@NonNull ApiVersionProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 API 版本解析器
     *
     * @return 版本解析器
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull public ApiVersionResolver apiVersionResolver() {
        return new ApiVersionResolver(properties);
    }

    /**
     * 创建 API 版本拦截器
     *
     * @param resolver 版本解析器
     * @return 版本拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull public ApiVersionInterceptor apiVersionInterceptor(@NonNull ApiVersionResolver resolver) {
        return new ApiVersionInterceptor(resolver, properties);
    }

    /**
     * 创建 API 版本路由映射处理器
     *
     * @return 版本路由映射处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull public ApiVersionRequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandlerMapping(properties);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor(apiVersionResolver()))
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**");
    }
}