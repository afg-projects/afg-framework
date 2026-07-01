package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.app.controller.AppController;
import io.github.afgprojects.framework.security.auth.app.controller.OAuth2ClientController;
import io.github.afgprojects.framework.security.auth.app.service.JdbcAppService;
import io.github.afgprojects.framework.security.auth.app.service.JdbcOAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.OAuth2ClientService;
import io.github.afgprojects.framework.security.core.oauth2.model.ClientDetails;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.properties.oauth2.OAuth2Config;
import io.github.afgprojects.framework.security.auth.properties.token.TokenConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用管理自动配置。
 *
 * <p>当 {@link DataManager} Bean 存在时（即 JDBC 数据访问层可用），
 * 自动配置应用管理相关的 Service 和 Controller，并使用
 * {@link JdbcOAuth2ClientService} 替代 {@link io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService}。
 *
 * <p>首次启动时，从 YAML 配置初始化 OAuth2 客户端种子数据到数据库。
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = OAuth2AutoConfiguration.class)
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppManagementAutoConfiguration {

    /**
     * 注册基于 JDBC 的 OAuth2 客户端服务，替代 InMemoryOAuth2ClientService。
     *
     * <p>使用 {@code @ConditionalOnMissingBean} 确保可被自定义实现替换。
     * 显式指定按 {@link OAuth2ClientService} 接口类型匹配，
     * 确保 {@link io.github.afgprojects.framework.security.auth.oauth2.InMemoryOAuth2ClientService}
     * 不会被同时注册。
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2ClientService.class)
    public JdbcOAuth2ClientService jdbcOAuth2ClientService(DataManager dataManager) {
        log.info("Configuring JDBC OAuth2 client service");
        return new JdbcOAuth2ClientService(dataManager);
    }

    /**
     * 注册应用管理服务。
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcAppService jdbcAppService(DataManager dataManager,
                                         JdbcOAuth2ClientService clientService,
                                         TenantContextHolder tenantContextHolder) {
        log.info("Configuring app management service");
        return new JdbcAppService(dataManager, clientService, tenantContextHolder);
    }

    /**
     * 注册应用管理 Controller。
     */
    @Bean
    @ConditionalOnMissingBean
    public AppController appController(JdbcAppService appService) {
        log.info("Configuring app management controller");
        return new AppController(appService);
    }

    /**
     * 注册 OAuth2 客户端管理 Controller。
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientController oauth2ClientController(JdbcOAuth2ClientService clientService) {
        log.info("Configuring OAuth2 client management controller");
        return new OAuth2ClientController(clientService);
    }

    /**
     * OAuth2 客户端种子数据初始化器。
     *
     * <p>当数据库中 {@code auth_client} 表为空时，从 YAML 配置初始化种子数据。
     * 使用 {@link SmartInitializingSingleton} 延迟到所有 Bean（包括 Liquibase）初始化完成后再执行，
     * 避免在表尚未创建时查询数据库。
     */
    @Bean
    public ClientDetailsSeedInitializer clientDetailsSeedInitializer(
            JdbcOAuth2ClientService clientService,
            AuthSecurityProperties properties) {
        return new ClientDetailsSeedInitializer(clientService, properties);
    }

    /**
     * 种子数据初始化器。
     *
     * <p>实现 {@link SmartInitializingSingleton}，在所有单例 Bean 初始化完成（包括 Liquibase 迁移）
     * 之后执行。从 YAML 配置读取客户端列表，仅在数据库为空时写入。
     */
    static class ClientDetailsSeedInitializer implements SmartInitializingSingleton {
        private final JdbcOAuth2ClientService clientService;
        private final AuthSecurityProperties properties;

        ClientDetailsSeedInitializer(JdbcOAuth2ClientService clientService, AuthSecurityProperties properties) {
            this.clientService = clientService;
            this.properties = properties;
        }

        @Override
        public void afterSingletonsInstantiated() {
            OAuth2Config oauth2Config = properties.getOauth2();
            TokenConfig tokenConfig = properties.getToken();

            Set<ClientDetails> seedClients = oauth2Config.getClients().stream()
                    .map(config -> new ClientDetails(
                            config.clientId(),
                            config.clientSecret(),
                            config.clientName(),
                            config.redirectUris(),
                            config.scopes(),
                            config.grantTypes(),
                            config.requirePkce(),
                            tokenConfig.getAccessTokenTtl(),
                            tokenConfig.getRefreshTokenTtl()
                    ))
                    .collect(Collectors.toSet());

            clientService.initSeedDataIfEmpty(seedClients);
        }
    }
}
