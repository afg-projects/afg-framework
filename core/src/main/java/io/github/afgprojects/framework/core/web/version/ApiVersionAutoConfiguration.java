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
 *
 * <p>性能优化：
 * <ul>
 *   <li>版本匹配逻辑在 {@link ApiVersionRequestCondition} 中执行，只对带 @ApiVersion 注解的请求生效</li>
 *   <li>没有 @ApiVersion 注解的 Controller 完全不受影响，零额外开销</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Servlet.class)
@ConditionalOnProperty(prefix = "afg.api-version", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiVersionProperties.class)
public class ApiVersionAutoConfiguration {

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
     * 创建 API 版本路由映射处理器
     * 只处理带有 @ApiVersion 注解的 Controller
     *
     * @return 版本路由映射处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @NonNull public ApiVersionRequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping() {
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping(properties);
        // 设置高优先级（0），确保优先于默认的 RequestMappingHandlerMapping
        mapping.setOrder(0);
        return mapping;
    }
}
