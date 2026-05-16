package io.github.afgprojects.framework.security.auth.tenant.config;

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
 * <p>当 {@code afg.auth.tenant.enabled=true} 时自动配置租户相关组件。
 *
 * <p>配置内容包括：
 * <ul>
 *   <li>{@link TenantContextHolder} - 租户上下文持有者</li>
 *   <li>{@link TenantResolverChain} - 租户解析器链</li>
 *   <li>{@link TenantValidator} - 租户验证器</li>
 *   <li>{@link TenantFilter} - 租户过滤器</li>
 *   <li>{@link TenantProperties} - 租户配置属性</li>
 * </ul>
 *
 * <p>租户验证器选择策略：
 * <ul>
 *   <li>当 {@link AfgTenantService} 存在时，使用 {@link DefaultTenantValidator}</li>
 *   <li>当 {@link AfgTenantService} 不存在时，使用 {@link NoOpTenantValidator}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "afg.auth.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
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
     * @param properties 租户配置属性
     * @return Caffeine 缓存实例
     */
    @Bean
    @ConditionalOnBean(AfgTenantService.class)
    public Cache<String, Boolean> tenantValidationCache(TenantProperties properties) {
        Duration ttl = properties.getValidation().getCacheTtl();
        log.info("创建租户验证缓存: ttl={}", ttl);

        return Caffeine.newBuilder()
                .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(1000)
                .build();
    }

    /**
     * 创建默认租户验证器（当 AfgTenantService 存在时）。
     *
     * @param tenantService 租户服务
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
     *创建空操作租户验证器（当 AfgTenantService 不存在时）。
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
     * @param properties 租户配置属性
     * @param validator 租户验证器
     * @return TenantResolverChain 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantResolverChain tenantResolverChain(
            @NonNull TenantProperties properties,
            @NonNull TenantValidator validator) {

        log.info("创建 TenantResolverChain: strategies={}, failIfUnresolved={}",
                properties.getStrategies(), properties.isFailIfUnresolved());

        // 创建解析器列表（根据配置策略）
        List<TenantResolver> resolvers = createResolvers(properties);

        // 创建解析器链
        return new TenantResolverChain(
                resolvers,
                validator,
                properties.isFailIfUnresolved()
        );
    }

    /**
     * 创建租户过滤器。
     *
     * @param resolverChain 租户解析器链
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
     * @param properties 租户配置属性
     * @return 解析器列表
     */
    private List<TenantResolver> createResolvers(@NonNull TenantProperties properties) {
        List<TenantResolver> resolvers = new ArrayList<>();

        for (TenantProperties.TenantStrategy strategy : properties.getStrategies()) {
            TenantResolver resolver = createResolverForStrategy(strategy, properties);
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
     * @param strategy 解析策略
     * @param properties 租户配置属性
     * @return 解析器实例，如果策略不支持则返回 null
     */
    private TenantResolver createResolverForStrategy(
            TenantProperties.TenantStrategy strategy,
            TenantProperties properties) {

        switch (strategy) {
            case TOKEN:
                // Token 解析器需要从 JWT 中提取租户信息
                // 这里暂时返回 null，实际实现需要注入 JwtDecoder
                log.warn("TOKEN 解析器暂未实现，需要配置 JwtDecoder");
                return null;

            case HEADER:
                // 从请求头解析租户
                return createHeaderResolver(properties);

            case DOMAIN:
                // 从域名解析租户
                return createDomainResolver(properties);

            case DEFAULT:
                // 使用默认租户
                return createDefaultResolver(properties);

            default:
                log.warn("未知的租户解析策略: {}", strategy);
                return null;
        }
    }

    /**
     * 创建请求头解析器。
     *
     * @param properties 租户配置属性
     * @return HeaderTenantResolver 实例
     */
    private TenantResolver createHeaderResolver(TenantProperties properties) {
        // 使用内部类实现，避免依赖 resource-server 模块
        return new HeaderResolver(properties.getHeaderName());
    }

    /**
     * 创建域名解析器。
     *
     * @param properties 租户配置属性
     * @return DomainTenantResolver 实例
     */
    private TenantResolver createDomainResolver(TenantProperties properties) {
        return new DomainResolver(properties.getDomainMappings());
    }

    /**
     * 创建默认租户解析器。
     *
     * @param properties 租户配置属性
     * @return DefaultTenantResolver 实例
     */
    private TenantResolver createDefaultResolver(TenantProperties properties) {
        return new DefaultResolver(properties.getDefaultTenant());
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

            // 从域名映射中查找租户 ID
            String tenantId = domainMappings.get(host);
            if (tenantId != null) {
                return new SimpleTenantContext(tenantId);
            }

            // 尝试从子域名提取（如 tenant.example.com -> tenant）
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