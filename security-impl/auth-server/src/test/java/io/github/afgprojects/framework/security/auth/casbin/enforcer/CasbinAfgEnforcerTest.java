package io.github.afgprojects.framework.security.auth.casbin.enforcer;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties.CasbinConfig;
import io.github.afgprojects.framework.security.auth.casbin.model.AfgPolicyService;
import io.github.afgprojects.framework.security.auth.casbin.model.CasbinRule;
import io.github.afgprojects.framework.security.auth.casbin.model.InMemoryPolicyService;
import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

/**
 * CasbinAfgEnforcer 测试
 */
class CasbinAfgEnforcerTest {

    private AuthSecurityProperties.CasbinConfig properties;
    private AfgPolicyService policyService;
    private CasbinAfgEnforcer enforcer;

    @BeforeEach
    void setUp() {
        properties = new AuthSecurityProperties.CasbinConfig();
        policyService = new InMemoryPolicyService();

        // 添加测试策略 - RBAC with domains 模式
        // 策略：admin 角色可以在 tenant1 域中读取 /api/data
        policyService.savePolicy(CasbinRule.createPolicy("admin", "tenant1", "/api/data", "read"));
        // 策略：admin 角色可以在 tenant1 域中写入 /api/data
        policyService.savePolicy(CasbinRule.createPolicy("admin", "tenant1", "/api/data", "write"));

        // 角色继承：user1 在 tenant1 域中属于 admin 角色
        policyService.savePolicy(CasbinRule.createRole("user1", "tenant1", "admin"));

        enforcer = new CasbinAfgEnforcer(properties, policyService);
    }

    @Test
    void should_enforcePolicy_when_userHasRole() {
        // user1 继承 admin 角色，应该有权限
        boolean result = enforcer.enforce("user1", "tenant1", "/api/data", "read");
        assertThat(result).isTrue();
    }

    @Test
    void should_denyPolicy_when_noRole() {
        // guest 没有任何角色，不应该有权限
        boolean result = enforcer.enforce("guest", "tenant1", "/api/data", "read");
        assertThat(result).isFalse();
    }

    @Test
    void should_denyCrossDomain() {
        // user1 在 tenant2 域中没有角色
        boolean result = enforcer.enforce("user1", "tenant2", "/api/data", "read");
        assertThat(result).isFalse();
    }

    @Test
    void should_enforceWithWildcardResource() {
        // 添加通配符策略给 super 角色
        policyService.savePolicy(CasbinRule.createPolicy("super", "tenant1", "/api/*", "read"));
        // user2 继承 super 角色
        policyService.savePolicy(CasbinRule.createRole("user2", "tenant1", "super"));
        enforcer.reloadPolicies();

        boolean result = enforcer.enforce("user2", "tenant1", "/api/users", "read");
        assertThat(result).isTrue();
    }

    @Test
    void should_enforceByKeyMatch2() {
        // 添加 keyMatch2 策略给 admin 角色
        policyService.savePolicy(CasbinRule.createPolicy("admin", "tenant1", "/api/*", "get"));
        enforcer.reloadPolicies();

        // keyMatch2 应该匹配 /api/users 和 /api/data
        assertThat(enforcer.enforce("user1", "tenant1", "/api/users", "get")).isTrue();
        assertThat(enforcer.enforce("user1", "tenant1", "/api/data", "get")).isTrue();
        assertThat(enforcer.enforce("user1", "tenant1", "/api/data", "post")).isFalse();
    }

    @Test
    void should_enforceSubjectOnly() {
        // 使用 AfgEnforcer 接口的两参数方法
        // 由于策略在 tenant1 域中，使用空域时不会有匹配
        boolean result = enforcer.enforce("admin", "/api/data", "read");
        // 这个测试验证两参数方法在没有域的情况下行为
        // 实际应用中，应该使用四参数方法或者提供默认域
        assertThat(result).isFalse();
    }

    @Test
    void should_denyWhenSubjectIsNull() {
        boolean result = enforcer.enforce((String) null, "/api/data", "read");
        assertThat(result).isFalse();
    }

    @Test
    void should_addPolicy() {
        // 添加新角色和策略
        enforcer.addPolicy("viewer", "tenant1", "/api/users", "read");
        enforcer.addRoleForUser("user2", "tenant1", "viewer");

        boolean result = enforcer.enforce("user2", "tenant1", "/api/users", "read");
        assertThat(result).isTrue();
    }

    @Test
    void should_removePolicy() {
        enforcer.addPolicy("viewer", "tenant1", "/api/users", "read");
        enforcer.addRoleForUser("user2", "tenant1", "viewer");
        assertThat(enforcer.enforce("user2", "tenant1", "/api/users", "read")).isTrue();

        enforcer.removePolicy("viewer", "tenant1", "/api/users", "read");
        assertThat(enforcer.enforce("user2", "tenant1", "/api/users", "read")).isFalse();
    }

    @Test
    void should_addRoleForUser() {
        enforcer.addRoleForUser("user2", "tenant1", "admin");

        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isTrue();
    }

    @Test
    void should_deleteRoleForUser() {
        enforcer.addRoleForUser("user2", "tenant1", "admin");
        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isTrue();

        enforcer.deleteRoleForUser("user2", "tenant1", "admin");
        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isFalse();
    }

    @Test
    void should_reloadPolicies() {
        // 初始 user2 没有权限
        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isFalse();

        // 直接通过 policyService 添加策略和角色
        policyService.savePolicy(CasbinRule.createPolicy("viewer", "tenant1", "/api/data", "read"));
        policyService.savePolicy(CasbinRule.createRole("user2", "tenant1", "viewer"));

        // 重载前仍然没有权限
        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isFalse();

        // 重载后有权限
        enforcer.reloadPolicies();
        assertThat(enforcer.enforce("user2", "tenant1", "/api/data", "read")).isTrue();
    }

    @Test
    void should_getEnforcer() {
        Enforcer casbinEnforcer = enforcer.getEnforcer();
        assertThat(casbinEnforcer).isNotNull();
    }

    @Test
    void should_clearPolicies() {
        enforcer.clearPolicies();
        assertThat(enforcer.enforce("admin", "tenant1", "/api/data", "read")).isFalse();
    }
}
