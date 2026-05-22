package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.auth.api.DataScopeController;
import io.github.afgprojects.framework.security.auth.api.DeptController;
import io.github.afgprojects.framework.security.auth.datascope.service.DataScopeResolverImpl;
import io.github.afgprojects.framework.security.auth.datascope.service.JdbcDataScopeService;
import io.github.afgprojects.framework.security.auth.datascope.service.JdbcDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据范围模块自动配置。
 *
 * <p>配置数据范围服务的核心组件：
 * <ul>
 *   <li>JdbcDeptService - 部门服务</li>
 *   <li>JdbcDataScopeService - 数据范围服务</li>
 * </ul>
 *
 * <p>配置数据范围管理 API：
 * <ul>
 *   <li>DeptController - 部门管理 API</li>
 *   <li>DataScopeController - 数据范围管理 API</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityProperties.class)
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeAutoConfiguration {

    // ========== 数据范围服务 ==========

    /**
     * 配置部门服务。
     *
     * @param dataManager 数据管理器
     * @return JdbcDeptService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcDeptService jdbcDeptService(DataManager dataManager) {
        log.info("Initializing JdbcDeptService");
        return new JdbcDeptService(dataManager);
    }

    /**
     * 配置数据范围服务。
     *
     * @param dataManager 数据管理器
     * @param deptService 部门服务
     * @return JdbcDataScopeService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcDataScopeService jdbcDataScopeService(DataManager dataManager, JdbcDeptService deptService) {
        log.info("Initializing JdbcDataScopeService");
        return new JdbcDataScopeService(dataManager, deptService);
    }

    /**
     * 配置数据范围解析器。
     *
     * @param deptService     部门服务
     * @param dataScopeService 数据范围服务
     * @return DataScopeResolverImpl 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeResolverImpl dataScopeResolver(JdbcDeptService deptService, JdbcDataScopeService dataScopeService) {
        log.info("Initializing DataScopeResolverImpl");
        return new DataScopeResolverImpl(deptService, dataScopeService);
    }

    // ========== 数据范围管理 API ==========

    /**
     * 配置部门管理 API。
     *
     * @param deptService 部门服务
     * @return DeptController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "api-enabled", havingValue = "true", matchIfMissing = true)
    public DeptController deptController(JdbcDeptService deptService) {
        log.info("Initializing DeptController");
        return new DeptController(deptService);
    }

    /**
     * 配置数据范围管理 API。
     *
     * @param dataScopeService 数据范围服务
     * @return DataScopeController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "data-scope-api-enabled", havingValue = "true", matchIfMissing = true)
    public DataScopeController dataScopeController(JdbcDataScopeService dataScopeService) {
        log.info("Initializing DataScopeController");
        return new DataScopeController(dataScopeService);
    }
}
