package io.github.afgprojects.framework.core.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextProvider;
import io.github.afgprojects.framework.core.web.context.DataScopeContextFilter;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;

/**
 * 数据权限自动配置
 * <p>
 * 配置数据权限的基础设施，包括配置属性、上下文过滤器等。
 * MyBatis-Plus 相关的拦截器配置在 data-mybatis-plus 模块中。
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   data-scope:
 *     enabled: true
 *     default-scope-type: DEPT
 *     dept-table: sys_dept
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgCoreProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DataScopeAutoConfiguration implements WebMvcConfigurer {

    /**
     * 数据权限上下文过滤器
     * <p>
     * 在请求开始时初始化数据权限上下文，请求结束时清除。
     * 需要 AfgCoreProperties 存在且 enabled=true。
     */
    @Bean
    @ConditionalOnBean(AfgCoreProperties.class)
    @ConditionalOnProperty(prefix = "afg.core.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public DataScopeContextFilter dataScopeContextFilter(
            AfgCoreProperties properties,
            @Nullable DataScopeContextProvider contextProvider,
            @Nullable AfgSecurityContextBridge securityContextBridge) {
        return new DataScopeContextFilter(properties, contextProvider, securityContextBridge);
    }
}