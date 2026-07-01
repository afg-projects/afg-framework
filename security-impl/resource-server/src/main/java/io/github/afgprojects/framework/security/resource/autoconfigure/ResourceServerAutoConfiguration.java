package io.github.afgprojects.framework.security.resource.autoconfigure;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import io.github.afgprojects.framework.core.cache.CacheManager;
import io.github.afgprojects.framework.security.core.token.JwtClaimsConfig;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import io.github.afgprojects.framework.security.resource.jwt.JwtAfgAuthenticationConverter;
import io.github.afgprojects.framework.security.resource.jwt.JwtAuthenticationConverter;
import io.github.afgprojects.framework.security.resource.permission.CachedPermissionChecker;
import io.github.afgprojects.framework.security.resource.permission.DynamicApiPermissionInterceptor;
import io.github.afgprojects.framework.security.resource.permission.DynamicApiPermissionManager;
import io.github.afgprojects.framework.security.resource.permission.HttpPermissionClient;
import io.github.afgprojects.framework.security.resource.permission.JwtPermissionChecker;
import io.github.afgprojects.framework.security.resource.permission.PermissionAspect;
import io.github.afgprojects.framework.security.resource.permission.RemotePermissionClient;
import io.github.afgprojects.framework.security.resource.permission.SignedHttpPermissionClient;
import io.github.afgprojects.framework.security.resource.properties.ResourceSecurityProperties;
import io.github.afgprojects.framework.security.resource.tenant.TokenTenantResolver;
import io.github.afgprojects.framework.security.core.tenant.HeaderTenantResolver;
import io.github.afgprojects.framework.security.core.tenant.TenantResolverChain;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 资源服务器自动配置。
 *
 * <p>自动配置 JWT 验证、远程验证和租户解析等组件。
 *
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(ResourceSecurityProperties.class)
@ConditionalOnProperty(prefix = "afg.security.resource-server", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ResourceServerAutoConfiguration {

    /**
     * 注册 JwtClaimsExtractor Bean。
     *
     * <p>从 ResourceSecurityJwtProperties 构建 JwtClaimsConfig，
     * 统一管理 JWT Claim 名称映射。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource-server.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public JwtClaimsExtractor jwtClaimsExtractor(@NonNull ResourceSecurityProperties properties) {
        JwtClaimsConfig claimsConfig = new JwtClaimsConfig();
        claimsConfig.setUserIdClaim(properties.getJwt().getUserIdClaim());
        claimsConfig.setUsernameClaim(properties.getJwt().getUsernameClaim());
        claimsConfig.setRolesClaim(properties.getJwt().getRolesClaim());
        claimsConfig.setPermissionsClaim(properties.getJwt().getPermissionsClaim());
        claimsConfig.setTenantIdClaim(properties.getJwt().getTenantIdClaim());
        log.info("Configuring JwtClaimsExtractor with userIdClaim={}, usernameClaim={}, tenantIdClaim={}",
                claimsConfig.getUserIdClaim(), claimsConfig.getUsernameClaim(), claimsConfig.getTenantIdClaim());
        return new JwtClaimsExtractor(claimsConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource-server.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(@NonNull JwtClaimsExtractor claimsExtractor) {
        log.info("Configuring JWT authentication converter");
        return new JwtAuthenticationConverter(claimsExtractor);
    }

    /**
     * 注册 Jwt 到 AFG 认证令牌的适配转换器 Bean。
     *
     * <p>供 Spring Security 的 {@code .jwt().jwtAuthenticationConverter(...)} 接入，
     * 将 {@link Jwt} 转换为 {@link io.github.afgprojects.framework.security.core.authentication.AfgAuthentication}，
     * 从 JWT 的 permissions/roles claim 还原 authorities。
     * 应用若自定义了 {@code Converter<Jwt, AbstractAuthenticationToken>}，本 bean 自动退让。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource-server.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(Converter.class)
    public JwtAfgAuthenticationConverter jwtAfgAuthenticationConverter(@NonNull JwtAuthenticationConverter delegate) {
        log.info("Configuring Jwt -> AfgAuthentication converter");
        return new JwtAfgAuthenticationConverter(delegate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource-server.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public TokenTenantResolver tokenTenantResolver(@NonNull JwtClaimsExtractor claimsExtractor) {
        log.info("Configuring token tenant resolver with claim: {}", claimsExtractor.getClaimsConfig().getTenantIdClaim());
        TokenTenantResolver resolver = new TokenTenantResolver(claimsExtractor);
        resolver.setOrder(100);
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean
    public HeaderTenantResolver headerTenantResolver(@NonNull ResourceSecurityProperties properties) {
        log.info("Configuring header tenant resolver with header: {}", properties.getTenant().getHeaderName());
        HeaderTenantResolver resolver = new HeaderTenantResolver(properties.getTenant().getHeaderName());
        resolver.setOrder(200);
        return resolver;
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantResolverChain tenantResolverChain(
            @Autowired(required = false) @Nullable TokenTenantResolver tokenTenantResolver,
            @NonNull HeaderTenantResolver headerTenantResolver,
            @NonNull ResourceSecurityProperties properties) {

        log.info("Configuring tenant resolver chain with strategies: {}", properties.getTenant().getStrategies());

        TenantResolverChain chain = new TenantResolverChain();
        chain.setFailIfUnresolved(properties.getTenant().isFailIfUnresolved());

        List<String> strategies = properties.getTenant().getStrategies();
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

        if (chain.isEmpty()) {
            log.info("No tenant strategies configured, using default resolvers");
            if (tokenTenantResolver != null) {
                chain.addResolver(tokenTenantResolver);
            }
            chain.addResolver(headerTenantResolver);
        }

        return chain;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtPermissionChecker jwtPermissionChecker(@Autowired(required = false) @Nullable JwtClaimsExtractor claimsExtractor) {
        log.info("Configuring JWT permission checker");
        if (claimsExtractor != null) {
            return new JwtPermissionChecker(claimsExtractor);
        }
        return new JwtPermissionChecker();
    }

    @Bean
    @ConditionalOnProperty(prefix = "afg.security.resource-server.permission", name = "auth-server-url")
    @ConditionalOnMissingBean
    public RemotePermissionClient remotePermissionClient(@NonNull ResourceSecurityProperties properties) {
        String authServerUrl = properties.getPermission().getAuthServerUrl();
        String keyId = properties.getPermission().getKeyId();
        String secret = properties.getPermission().getSecret();

        if (keyId != null && !keyId.isEmpty() && secret != null && !secret.isEmpty()) {
            log.info("Configuring signed HTTP permission client with auth server: {}, keyId: {}", authServerUrl, keyId);
            return new SignedHttpPermissionClient(authServerUrl, keyId, secret);
        } else {
            log.warn("Configuring unsigned HTTP permission client - NOT recommended for production! Consider configuring keyId and secret.");
            return new HttpPermissionClient(authServerUrl);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public CachedPermissionChecker cachedPermissionChecker(
            @Autowired(required = false) @Nullable RemotePermissionClient remoteClient,
            @NonNull JwtPermissionChecker jwtChecker,
            @Autowired(required = false) @Nullable CacheManager cacheManager) {
        log.info("Configuring cached permission checker");
        return new CachedPermissionChecker(remoteClient, jwtChecker, cacheManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionAspect permissionAspect(@NonNull CachedPermissionChecker permissionChecker) {
        log.info("Configuring permission aspect");
        return new PermissionAspect(permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicApiPermissionManager dynamicApiPermissionManager(
            @Autowired(required = false) @Nullable RemoteConfigClient remoteConfigClient) {
        log.info("Configuring dynamic API permission manager");
        return new DynamicApiPermissionManager(remoteConfigClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicApiPermissionInterceptor dynamicApiPermissionInterceptor(
            @NonNull DynamicApiPermissionManager permissionManager,
            @NonNull CachedPermissionChecker permissionChecker) {
        log.info("Configuring dynamic API permission interceptor");
        return new DynamicApiPermissionInterceptor(permissionManager, permissionChecker);
    }
}
