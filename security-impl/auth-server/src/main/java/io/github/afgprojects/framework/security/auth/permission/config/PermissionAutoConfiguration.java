package io.github.afgprojects.framework.security.auth.permission.config;

import io.github.afgprojects.framework.security.auth.permission.DataScopeInterceptor;
import io.github.afgprojects.framework.security.auth.permission.DefaultRbacService;
import io.github.afgprojects.framework.security.auth.permission.InMemoryRolePermissionStorage;
import io.github.afgprojects.framework.security.auth.permission.JdbcDataScopeService;
import io.github.afgprojects.framework.security.auth.permission.JdbcRolePermissionStorage;
import io.github.afgprojects.framework.security.auth.permission.RbacPermissionService;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;

/**
 * 权限自动配置。
 *
 * <p>自动配置权限相关的 Bean，包括：
 * <ul>
 *   <li>RolePermissionStorage - 角色权限存储</li>
 *   <li>RbacService - RBAC 服务</li>
 *   <li>PermissionService - 权限服务</li>
 *   <li>DataScopeService - 数据权限服务</li>
 *   <li>DataScopeInterceptor - 数据权限拦截器</li>
 * </ul>
 *
 * <h3>配置属性</h3>
 * <pre>
 * afg.permission.enabled=true
 * afg.permission.default-data-scope=ALL
 * afg.permission.data-scope-interceptor-enabled=true
 * </pre>
 *
 * <h3>自定义实现</h3>
 * <p>可以通过提供自定义 Bean 来覆盖默认实现：
 * <pre>{@code
 * @Bean
 * public RolePermissionStorage customRolePermissionStorage() {
 *     return new JdbcRolePermissionStorage(jdbcTemplate);
 * }
 *
 * @Bean
 * public DataScopeService customDataScopeService() {
 *     return new JdbcDataScopeService(jdbcTemplate);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.permission", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PermissionProperties.class)
@Slf4j
public class PermissionAutoConfiguration {

    /**
     * 创建 JDBC 角色权限存储。
     *
     * <p>当 DataSource 可用时使用 JDBC 实现。
     *
     * @param dataSource 数据源
     * @return JDBC 角色权限存储实例
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(RolePermissionStorage.class)
    public RolePermissionStorage jdbcRolePermissionStorage(DataSource dataSource) {
        log.info("Using JdbcRolePermissionStorage as default RolePermissionStorage");
        return new JdbcRolePermissionStorage(new JdbcTemplate(dataSource));
    }

    /**
     * 创建内存角色权限存储。
     *
     * <p>当 DataSource 不可用时使用内存实现，适用于开发和测试环境。
     *
     * @return 内存角色权限存储实例
     */
    @Bean
    @ConditionalOnMissingBean({DataSource.class, RolePermissionStorage.class})
    public RolePermissionStorage inMemoryRolePermissionStorage() {
        log.info("Using InMemoryRolePermissionStorage as default RolePermissionStorage (no DataSource available)");
        return InMemoryRolePermissionStorage.createTestInstance();
    }

    /**
     * 创建 RBAC 服务。
     *
     * <p>默认使用 DefaultRbacService 实现。
     *
     * @param storage 角色权限存储
     * @return RBAC 服务实例
     */
    @Bean
    @ConditionalOnMissingBean(RbacService.class)
    public RbacService rbacService(RolePermissionStorage storage) {
        log.info("Using DefaultRbacService as default RbacService");
        return new DefaultRbacService(storage);
    }

    /**
     * 创建权限服务。
     *
     * <p>默认使用 RBAC 权限服务。
     *
     * @param storage 角色权限存储
     * @return 权限服务实例
     */
    @Bean
    @ConditionalOnMissingBean(PermissionService.class)
    public PermissionService permissionService(RolePermissionStorage storage) {
        log.info("Using RbacPermissionService as default PermissionService");
        return new RbacPermissionService(storage);
    }

    /**
     * 创建 JDBC 数据权限服务。
     *
     * <p>当 DataSource 可用时使用 JDBC 实现。
     *
     * @param dataSource 数据源
     * @return JDBC 数据权限服务实例
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(DataScopeService.class)
    public DataScopeService jdbcDataScopeService(DataSource dataSource) {
        log.info("Using JdbcDataScopeService as default DataScopeService");
        return new JdbcDataScopeService(new JdbcTemplate(dataSource));
    }

    /**
     * 创建内存数据权限服务。
     *
     * <p>当 DataSource 不可用时使用内存实现，返回默认的数据范围配置。
     *
     * @return 内存数据权限服务实例
     */
    @Bean
    @ConditionalOnMissingBean({DataSource.class, DataScopeService.class})
    public DataScopeService inMemoryDataScopeService() {
        log.info("Using InMemoryDataScopeService as default DataScopeService (no DataSource available)");
        return new InMemoryDataScopeService();
    }

    /**
     * 创建数据权限拦截器。
     *
     * <p>拦截器会在每个请求开始时设置数据权限上下文，
     * 请求结束时清除上下文。
     *
     * @param dataScopeService 数据权限服务
     * @param properties       权限配置属性
     * @return 数据权限拦截器实例
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeInterceptor.class)
    @ConditionalOnProperty(prefix = "afg.permission", name = "data-scope-interceptor-enabled",
            havingValue = "true", matchIfMissing = true)
    public DataScopeInterceptor dataScopeInterceptor(
            DataScopeService dataScopeService,
            PermissionProperties properties) {
        log.info("Creating DataScopeInterceptor with defaultDataScope={}", properties.getDefaultDataScope());
        return new DataScopeInterceptor(dataScopeService, properties);
    }

    /**
     * 内存数据权限服务实现。
     *
     * <p>默认返回 ALL 类型数据范围，适用于开发和测试。
     */
    @Slf4j
    static class InMemoryDataScopeService implements DataScopeService {

        @Override
        @NonNull
        public DataScope getDataScope(@NonNull String userId, @Nullable String tenantId) {
            log.debug("Returning default ALL data scope for userId={}, tenantId={}", userId, tenantId);
            return DataScope.of("default", "id", DataScopeType.ALL);
        }

        @Override
        public void setDataScope(@NonNull String userId, @Nullable String tenantId, @NonNull DataScope scope) {
            log.debug("Setting data scope for userId={}, tenantId={}: scopeType={}",
                    userId, tenantId, scope.scopeType());
            // 内存实现不持久化
        }

        @Override
        public void removeDataScope(@NonNull String userId, @Nullable String tenantId) {
            log.debug("Removing data scope for userId={}, tenantId={}", userId, tenantId);
            // 内存实现不持久化
        }
    }
}