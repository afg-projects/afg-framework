package io.github.afgprojects.framework.security.casbin;

import org.casbin.jcasbin.main.Enforcer;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.security.core.permission.PermissionService;

/**
 * Casbin 自动配置。
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CasbinProperties.class)
@ConditionalOnProperty(prefix = "afg.security.casbin", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CasbinAutoConfiguration {

    /**
     * 配置 Casbin 权限服务。
     *
     * @param enforcer Casbin 执行器
     * @return 权限服务
     */
    @Bean
    @ConditionalOnMissingBean(PermissionService.class)
    public PermissionService casbinPermissionService(@NonNull Enforcer enforcer) {
        return new CasbinPermissionService(enforcer);
    }
}