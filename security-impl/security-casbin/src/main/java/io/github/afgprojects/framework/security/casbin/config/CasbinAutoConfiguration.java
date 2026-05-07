package io.github.afgprojects.framework.security.casbin.config;

import io.github.afgprojects.framework.core.web.security.AfgEnforcer;
import io.github.afgprojects.framework.security.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.casbin.model.AfgPolicyService;
import io.github.afgprojects.framework.security.casbin.model.InMemoryPolicyService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Casbin 自动配置
 *
 * <p>自动配置 Casbin 权限执行器，需要提供 {@link AfgPolicyService} 的实现。
 *
 * <p>如果没有提供 {@link AfgPolicyService}，将使用内存存储（仅用于测试）。
 *
 * <p>配置示例：
 * <pre>
 * afg:
 *   security:
 *     casbin:
 *       enabled: true
 *       model-type: rbac-domain
 *       policy-adapter-type: jdbc
 *       auto-save: true
 *       auto-build-role-links: true
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CasbinProperties.class)
@ConditionalOnProperty(prefix = "afg.security.casbin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CasbinAutoConfiguration {

    /**
     * 配置 Casbin 权限执行器
     *
     * @param properties    Casbin 配置属性
     * @param policyService 策略服务
     * @return Casbin 权限执行器实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgEnforcer.class)
    public CasbinAfgEnforcer casbinAfgEnforcer(CasbinProperties properties, AfgPolicyService policyService) {
        return new CasbinAfgEnforcer(properties, policyService);
    }

    /**
     * 配置默认的内存策略服务
     *
     * <p>当没有提供其他实现时使用，仅用于测试。
     *
     * @return 内存策略服务实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgPolicyService.class)
    public AfgPolicyService inMemoryPolicyService() {
        return new InMemoryPolicyService();
    }
}
