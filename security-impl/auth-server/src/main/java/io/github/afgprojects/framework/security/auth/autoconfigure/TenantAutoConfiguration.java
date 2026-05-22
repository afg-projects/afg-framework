package io.github.afgprojects.framework.security.auth.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.tenant.filter.TenantFilter;
import io.github.afgprojects.framework.security.auth.tenant.resolver.TenantResolverChain;
import io.github.afgprojects.framework.security.auth.tenant.validator.DefaultTenantValidator;
import io.github.afgprojects.framework.security.auth.tenant.validator.NoOpTenantValidator;
import io.github.afgprojects.framework.security.auth.tenant.validator.TenantValidator;
import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * 租户自动配置。
 *
 * <p>当 {@code afg.security.auth-server.tenant.enabled=true} 时自动配置租户相关组件。
 *
 * <p>配置内容包括：
 * <ul>
 *   <li>{@link TenantContextHolder} - 租户上下文持有者</li>
 *   <li>{@link TenantResolverChain} - 租户解析器链</li>
 *   <li>{@link TenantValidator} - 租户验证器</li>
 *   <li>{@link TenantFilter} - 租户过滤器</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    /**
     * 创建租户上下文持有者。
     *
     * @return TenantContextHolder 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantContextHolder tenantContextHolder() {
        log.info("创建 TenantContextHolder");
        return new TenantContextHolder();
    }

    /**
     * 创建租户验证缓存。
     *
     * @param properties 认证服务器配置属性
     * @return Caffeine 缓存实例
     */
    @Bean
    @ConditionalOnBean(AfgTenantService.class)
    public Cache<String, Boolean> tenantValidationCache(AuthSecurityProperties properties) {
        Duration ttl = properties.getTenant().getValidation().getCacheTtl();
        log.info("创建租户验证缓存: ttl={}", ttl);

        return Caffeine.newBuilder()
                .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .build();
    }

    /**
     * 创建默认租户验证器（当 AfgTenantService 存在时）。
     *
     * @param tenantService   租户服务
     * @param validationCache 验证缓存
     * @return DefaultTenantValidator 实例
     */
    @Bean
    @ConditionalOnBean(AfgTenantService.class)
    public DefaultTenantValidator defaultTenantValidator(
            @NonNull AfgTenantService tenantService,
            @NonNull Cache<String, Boolean> validationCache) {
        log.info("创建 DefaultTenantValidator（使用 AfgTenantService）");
        return new DefaultTenantValidator(tenantService, validationCache);
    }

    /**
     * 创建空操作租户验证器（当 AfgTenantService 不存在时）。
     *
     * @return NoOpTenantValidator 实例
     */
    @Bean
    @ConditionalOnMissingBean(AfgTenantService.class)
    public NoOpTenantValidator noOpTenantValidator() {
        log.info("创建 NoOpTenantValidator（AfgTenantService 不存在）");
        return new NoOpTenantValidator();
    }

    /**
     * 创建租户解析器链。
     *
     * <p>根据配置的策略创建相应的解析器并按优先级排序。
     *
     * @param properties 认证服务器配置属性
     * @param validator   租户验证器
     * @return TenantResolverChain 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantResolverChain tenantResolverChain(
            @NonNull AuthSecurityProperties properties,
            @NonNull TenantValidator validator) {

        AuthSecurityProperties.TenantConfig tenantConfig = properties.getTenant();
        log.info("创建 TenantResolverChain: strategies={}, failIfUnresolved={}",
                tenantConfig.getStrategies(), tenantConfig.isFailIfUnresolved());

        // 创建解析器列表（根据配置策略）
        List<TenantResolver> resolvers = createResolvers(tenantConfig);

        // 创建解析器链
        return new TenantResolverChain(
                resolvers,
                validator,
                tenantConfig.isFailIfUnresolved()
        );
    }

    /**
     * 创建租户过滤器。
     *
     * @param resolverChain      租户解析器链
     * @param tenantContextHolder 租户上下文持有者
     * @return TenantFilter 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantFilter tenantFilter(
            @NonNull TenantResolverChain resolverChain,
            @NonNull TenantContextHolder tenantContextHolder) {
        log.info("创建 TenantFilter");
        return new TenantFilter(resolverChain, tenantContextHolder);
    }

    /**
     * 根据配置创建租户解析器列表。
     *
     * @param tenantConfig 租户配置
     * @return 解析器列表
     */
    private List<TenantResolver> createResolvers(AuthSecurityProperties.TenantConfig tenantConfig) {
        List<TenantResolver> resolvers = new ArrayList<>();

        for (AuthSecurityProperties.TenantConfig.TenantStrategy strategy : tenantConfig.getStrategies()) {
            TenantResolver resolver = createResolverForStrategy(strategy, tenantConfig);
            if (resolver != null) {
                resolvers.add(resolver);
                log.debug("添加解析器: strategy={}, resolver={}",
                        strategy, resolver.getClass().getSimpleName());
            }
        }

        return resolvers;
    }

    /**
     * 根据策略创建对应的解析器。
     *
     * @param strategy     解析策略
     * @param tenantConfig 租户配置
     * @return 解析器实例，如果策略不支持则返回 null
     */
    private TenantResolver createResolverForStrategy(
            AuthSecurityProperties.TenantConfig.TenantStrategy strategy,
            AuthSecurityProperties.TenantConfig tenantConfig) {

        switch (strategy) {
            case TOKEN:
                log.warn("TOKEN 解析器暂未实现，需要配置 JwtDecoder");
                return null;

            case HEADER:
                return createHeaderResolver(tenantConfig);

            case DOMAIN:
                return createDomainResolver(tenantConfig);

            case DEFAULT:
                return createDefaultResolver(tenantConfig);

            default:
                log.warn("未知的租户解析策略: {}", strategy);
                return null;
        }
    }

    /**
     * 创建请求头解析器。
     *
     * @param tenantConfig 租户配置
     * @return HeaderResolver 实例
     */
    private TenantResolver createHeaderResolver(AuthSecurityProperties.TenantConfig tenantConfig) {
        return new HeaderResolver(tenantConfig.getHeaderName());
    }

    /**
     * 创建域名解析器。
     *
     * @param tenantConfig 租户配置
     * @return DomainResolver 实例
     */
    private TenantResolver createDomainResolver(AuthSecurityProperties.TenantConfig tenantConfig) {
        return new DomainResolver(tenantConfig.getDomainMappings());
    }

    /**
     * 创建默认租户解析器。
     *
     * @param tenantConfig 租户配置
     * @return DefaultResolver 实例
     */
    private TenantResolver createDefaultResolver(AuthSecurityProperties.TenantConfig tenantConfig) {
        return new DefaultResolver(tenantConfig.getDefaultTenant());
    }

    // ========== 内部解析器实现 ==========

    /**
     * 请求头解析器（内部实现）。
     */
    private static class HeaderResolver implements TenantResolver {

        private final String headerName;
        private final int order = 200;

        HeaderResolver(String headerName) {
            this.headerName = headerName != null ? headerName : "X-Tenant-Id";
        }

        @Override
        public io.github.afgprojects.framework.security.core.tenant.TenantContext resolve(
                jakarta.servlet.http.HttpServletRequest request) {
            String tenantId = request.getHeader(headerName);
            if (tenantId == null || tenantId.isBlank()) {
                return null;
            }
            return new SimpleTenantContext(tenantId.trim());
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * 域名解析器（内部实现）。
     */
    private static class DomainResolver implements TenantResolver {

        private final java.util.Map<String, String> domainMappings;
        private final int order = 300;

        DomainResolver(java.util.Map<String, String> domainMappings) {
            this.domainMappings = domainMappings != null ? domainMappings : java.util.Map.of();
        }

        @Override
        public io.github.afgprojects.framework.security.core.tenant.TenantContext resolve(
                jakarta.servlet.http.HttpServletRequest request) {
            String host = request.getServerName();
            if (host == null || host.isBlank()) {
                return null;
            }

            String tenantId = domainMappings.get(host);
            if (tenantId != null) {
                return new SimpleTenantContext(tenantId);
            }

            String[] parts = host.split("\\.");
            if (parts.length >= 3) {
                String subdomain = parts[0];
                return new SimpleTenantContext(subdomain);
            }

            return null;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * 默认租户解析器（内部实现）。
     */
    private static class DefaultResolver implements TenantResolver {

        private final String defaultTenantId;
        private final int order = 400;

        DefaultResolver(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId != null ? defaultTenantId : "default";
        }

        @Override
        public io.github.afgprojects.framework.security.core.tenant.TenantContext resolve(
                jakarta.servlet.http.HttpServletRequest request) {
            return new SimpleTenantContext(defaultTenantId);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * 简单租户上下文实现。
     */
    private static class SimpleTenantContext implements io.github.afgprojects.framework.security.core.tenant.TenantContext {

        private final String tenantId;

        SimpleTenantContext(String tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }
    }
}