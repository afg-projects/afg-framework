package io.github.afgprojects.framework.security.resource.config;

import io.github.afgprojects.framework.core.cache.CacheManager;
import io.github.afgprojects.framework.security.resource.introspection.IntrospectionProperties;
import io.github.afgprojects.framework.security.resource.jwt.JwtAuthenticationConverter;
import io.github.afgprojects.framework.security.resource.jwt.JwtResourceProperties;
import io.github.afgprojects.framework.security.resource.permission.CachedPermissionChecker;
import io.github.afgprojects.framework.security.resource.permission.HttpPermissionClient;
import io.github.afgprojects.framework.security.resource.permission.JwtPermissionChecker;
import io.github.afgprojects.framework.security.resource.permission.PermissionAspect;
import io.github.afgprojects.framework.security.resource.permission.RemotePermissionClient;
import io.github.afgprojects.framework.security.resource.tenant.HeaderTenantResolver;
import io.github.afgprojects.framework.security.resource.tenant.TokenTenantResolver;
import io.github.afgprojects.framework.security.resource.tenant.TenantResolverChain;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 资源服务器自动配置。
 *
 * <p>自动配置 JWT 验证、远程验证和租户解析等组件。
 *
 * <p>启用条件：
 * <ul>
 *   <li>属性 afg.security.resource.enabled=true（默认）</li>
 *   <li>类路径存在 Spring Security OAuth2 Resource Server</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * afg.security.resource.enabled=true
 * afg.security.resource.jwt.jwk-set-uri=https://auth.example.com/.well-known/jwks.json
 * afg.security.resource.tenant.strategies=token,header
 * </pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({
        JwtResourceProperties.class,
        IntrospectionProperties.class,
        ResourceServerProperties.class
})
@ConditionalOnProperty(prefix = "afg.security.resource", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ResourceServerAutoConfiguration {

    /**
     * 配置 JWT 认证转换器。
     *
     * @param properties JWT 配置属性
     * @return JWT 认证转换器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(@NonNull JwtResourceProperties properties) {
        log.info("Configuring JWT authentication converter");
        return new JwtAuthenticationConverter(properties);
    }

    /**
     * 配置 Token 租户解析器。
     *
     * @param properties JWT 配置属性
     * @return Token 租户解析器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TokenTenantResolver tokenTenantResolver(@NonNull JwtResourceProperties properties) {
        log.info("Configuring token tenant resolver with claim: {}", properties.getTenantIdClaim());
        TokenTenantResolver resolver = new TokenTenantResolver(properties.getTenantIdClaim());
        resolver.setOrder(100); // 最高优先级
        return resolver;
    }

    /**
     * 配置请求头租户解析器。
     *
     * @param properties 资源服务器配置属性
     * @return 请求头租户解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public HeaderTenantResolver headerTenantResolver(@NonNull ResourceServerProperties properties) {
        log.info("Configuring header tenant resolver with header: {}", properties.getTenantHeaderName());
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties.getTenantHeaderName());
        resolver.setOrder(200); // 次高优先级
        return resolver;
    }

    /**
     * 配置租户解析链。
     *
     * @param tokenTenantResolver Token 租户解析器（可选，当 JWT 禁用时为 null）
     * @param headerTenantResolver 请求头租户解析器
     * @param properties 资源服务器配置属性
     * @return 租户解析链
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantResolverChain tenantResolverChain(
            @Autowired(required = false) @Nullable TokenTenantResolver tokenTenantResolver,
            @NonNull HeaderTenantResolver headerTenantResolver,
            @NonNull ResourceServerProperties properties) {

        log.info("Configuring tenant resolver chain with strategies: {}", properties.getTenantStrategies());

        TenantResolverChain chain = new TenantResolverChain();
        chain.setFailIfUnresolved(properties.isFailIfTenantUnresolved());

        // 根据配置的解析策略添加解析器
        List<String> strategies = properties.getTenantStrategies();
        for (String strategy : strategies) {
            switch (strategy.toLowerCase()) {
                case "token":
                    if (tokenTenantResolver != null) {
                        chain.addResolver(tokenTenantResolver);
                    } else {
                        log.warn("Token tenant resolver not available, skipping token strategy");
                    }
                    break;
                case "header":
                    chain.addResolver(headerTenantResolver);
                    break;
                default:
                    log.warn("Unknown tenant resolve strategy: {}", strategy);
            }
        }

        // 如果没有配置策略，默认使用可用的解析器
        if (chain.isEmpty()) {
            log.info("No tenant strategies configured, using default resolvers");
            if (tokenTenantResolver != null) {
                chain.addResolver(tokenTenantResolver);
            }
            chain.addResolver(headerTenantResolver);
        }

        return chain;
    }

    /**
     * 配置 JWT 权限校验器。
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtPermissionChecker jwtPermissionChecker() {
        log.info("Configuring JWT permission checker");
        return new JwtPermissionChecker();
    }

    /**
     * 配置 HTTP 远程权限客户端（可选）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource.permission", name = "auth-server-url")
    @ConditionalOnMissingBean
    public RemotePermissionClient remotePermissionClient(@NonNull ResourceServerProperties properties) {
        String authServerUrl = properties.getPermission().getAuthServerUrl();
        log.info("Configuring HTTP permission client with auth server: {}", authServerUrl);
        return new HttpPermissionClient(authServerUrl);
    }

    /**
     * 配置带缓存的权限校验器。
     */
    @Bean
    @ConditionalOnMissingBean
    public CachedPermissionChecker cachedPermissionChecker(
            @Autowired(required = false) @Nullable RemotePermissionClient remoteClient,
            @NonNull JwtPermissionChecker jwtChecker,
            @Autowired(required = false) @Nullable CacheManager cacheManager) {
        log.info("Configuring cached permission checker");
        return new CachedPermissionChecker(remoteClient, jwtChecker, cacheManager);
    }

    /**
     * 配置权限校验切面。
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionAspect permissionAspect(@NonNull CachedPermissionChecker permissionChecker) {
        log.info("Configuring permission aspect");
        return new PermissionAspect(permissionChecker);
    }
}