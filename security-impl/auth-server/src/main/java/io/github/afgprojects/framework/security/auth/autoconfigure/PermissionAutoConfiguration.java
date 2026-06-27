package io.github.afgprojects.framework.security.auth.autoconfigure;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.auth.api.*;
import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.auth.permission.adapter.JdbcCasbinAdapter;
import io.github.afgprojects.framework.security.auth.permission.service.CasbinRbacService;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcResourceService;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;
import io.github.afgprojects.framework.security.auth.properties.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.properties.casbin.CasbinConfig;
import io.github.afgprojects.framework.security.core.permission.RbacService;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 权限模块自动配置。
 *
 * <p>配置 RBAC 权限服务的核心组件：
 * <ul>
 *   <li>Casbin Model - 权限模型</li>
 *   <li>Casbin Adapter - 策略适配器</li>
 *   <li>Casbin Enforcer - 权限执行器</li>
 *   <li>JdbcRoleService - 角色服务</li>
 *   <li>JdbcResourceService - 资源服务</li>
 *   <li>CasbinRbacService - RBAC 服务</li>
 * </ul>
 *
 * <p>配置权限管理 API：
 * <ul>
 *   <li>RoleController - 角色管理 API</li>
 *   <li>PermissionController - 权限管理 API</li>
 *   <li>ResourceController - 资源管理 API</li>
 *   <li>UserPermissionController - 用户权限管理 API</li>
 *   <li>PermissionQueryController - 权限查询 API（供资源服务器调用）</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = CasbinAutoConfiguration.class, afterName = {
    "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration",
    "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration"
})
@EnableConfigurationProperties(AuthSecurityProperties.class)

@ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionAutoConfiguration {

    // ========== Casbin 核心组件 ==========

    /**
     * 配置 Casbin Model。
     *
     * @param properties 认证服务器配置属性
     * @return Casbin Model 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public Model casbinModel(AuthSecurityProperties properties) {
        CasbinConfig casbinConfig = properties.getCasbin();
        String modelPath = casbinConfig.getModelPath();

        Model model = new Model();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(modelPath)) {
            if (is == null) {
                log.warn("Casbin model file not found: {}, using default RBAC model", modelPath);
                model.addDef("r", "r", "sub, dom, obj, act");
                model.addDef("p", "p", "sub, dom, obj, act");
                model.addDef("g", "g", ", _, _");
                model.addDef("e", "e", "some(where (p.eft == allow))");
                model.addDef("m", "m", "g(r.sub, r.dom, p.sub) && r.dom == p.dom && r.obj == p.obj && r.act == p.act");
            } else {
                model.loadModelFromText(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("Failed to load Casbin model", e);
            throw new RuntimeException("Failed to load Casbin model", e);
        }
        log.info("Casbin model loaded from: {}", modelPath);
        return model;
    }

    /**
     * 配置 JDBC Casbin 适配器。
     *
     * @param dataManager 数据管理器
     * @return JdbcCasbinAdapter 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcCasbinAdapter jdbcCasbinAdapter(DataManager dataManager) {
        return new JdbcCasbinAdapter(dataManager);
    }

    /**
     * 配置 Casbin Enforcer。
     *
     * @param model  Casbin 模型
     * @param adapter Casbin 适配器
     * @return Enforcer 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public Enforcer casbinEnforcer(Model model, JdbcCasbinAdapter adapter) {
        Enforcer enforcer = new Enforcer(model, adapter);
        enforcer.enableAutoSave(true);
        log.info("Casbin enforcer initialized");
        return enforcer;
    }

    /**
     * 配置角色服务。
     *
     * @param dataManager 数据管理器
     * @param casbinEnforcer Casbin 执行器（用于双写后 reload 策略）
     * @return JdbcRoleService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CasbinAfgEnforcer.class)
    public JdbcRoleService jdbcRoleService(DataManager dataManager, CasbinAfgEnforcer casbinEnforcer) {
        return new JdbcRoleService(dataManager, casbinEnforcer);
    }

    /**
     * 配置资源服务。
     *
     * @param dataManager 数据管理器
     * @return JdbcResourceService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcResourceService jdbcResourceService(DataManager dataManager) {
        return new JdbcResourceService(dataManager);
    }

    /**
     * 配置 Casbin RBAC 服务。
     *
     * @param enforcer    Casbin 执行器
     * @param roleService 角色服务
     * @return CasbinRbacService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CasbinRbacService casbinRbacService(Enforcer enforcer, JdbcRoleService roleService) {
        return new CasbinRbacService(enforcer, roleService);
    }

    // ========== 权限管理 API ==========

    /**
     * 配置角色管理 API。
     *
     * @param roleService 角色服务
     * @param dataManager 数据管理器
     * @return RoleController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "api-enabled", havingValue = "true", matchIfMissing = true)
    public RoleController roleController(JdbcRoleService roleService, DataManager dataManager) {
        log.info("Initializing RoleController");
        return new RoleController(roleService, dataManager);
    }

    /**
     * 配置权限管理 API。
     *
     * @param resourceService 资源服务
     * @return PermissionController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "api-enabled", havingValue = "true", matchIfMissing = true)
    public PermissionController permissionController(JdbcResourceService resourceService) {
        log.info("Initializing PermissionController");
        return new PermissionController(resourceService);
    }

    /**
     * 配置资源管理 API。
     *
     * @param resourceService 资源服务
     * @return ResourceController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "api-enabled", havingValue = "true", matchIfMissing = true)
    public ResourceController resourceController(JdbcResourceService resourceService) {
        log.info("Initializing ResourceController");
        return new ResourceController(resourceService);
    }

    /**
     * 配置用户权限管理 API。
     *
     * @param rbacService RBAC 服务
     * @param roleService 角色服务
     * @return UserPermissionController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RbacService.class)
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "api-enabled", havingValue = "true", matchIfMissing = true)
    public UserPermissionController userPermissionController(RbacService rbacService, JdbcRoleService roleService) {
        log.info("Initializing UserPermissionController");
        return new UserPermissionController(rbacService, roleService);
    }

    /**
     * 配置权限查询 API（供资源服务器调用）。
     *
     * @param rbacService RBAC 服务
     * @return PermissionQueryController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RbacService.class)
    @ConditionalOnProperty(prefix = "afg.security.auth-server.permission", name = "query-api-enabled", havingValue = "true", matchIfMissing = true)
    public PermissionQueryController permissionQueryController(RbacService rbacService) {
        log.info("Initializing PermissionQueryController");
        return new PermissionQueryController(rbacService);
    }

}
