package io.github.afgprojects.framework.security.permission.config;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.permission.adapter.JdbcCasbinAdapter;
import io.github.afgprojects.framework.security.permission.service.CasbinRbacService;
import io.github.afgprojects.framework.security.permission.service.JdbcResourceService;
import io.github.afgprojects.framework.security.permission.service.JdbcRoleService;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 安全权限模块自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(DataManager.class)
public class PermissionAutoConfiguration {

    @Value("${afg.security.permission.model-path:casbin/rbac_model.conf}")
    private String modelPath;

    @Bean
    @ConditionalOnMissingBean
    public Model casbinModel() {
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

    @Bean
    @ConditionalOnMissingBean
    public JdbcCasbinAdapter jdbcCasbinAdapter(DataManager dataManager) {
        return new JdbcCasbinAdapter(dataManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public Enforcer casbinEnforcer(Model model, JdbcCasbinAdapter adapter) {
        Enforcer enforcer = new Enforcer(model, adapter);
        enforcer.enableAutoSave(true);
        log.info("Casbin enforcer initialized");
        return enforcer;
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcRoleService jdbcRoleService(DataManager dataManager, Enforcer enforcer) {
        return new JdbcRoleService(dataManager, enforcer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcResourceService jdbcResourceService(DataManager dataManager) {
        return new JdbcResourceService(dataManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public CasbinRbacService casbinRbacService(Enforcer enforcer, JdbcRoleService roleService) {
        return new CasbinRbacService(enforcer, roleService);
    }
}
