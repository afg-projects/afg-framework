package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.auth.casbin.model.CasbinRule;
import io.github.afgprojects.framework.security.auth.casbin.model.InMemoryPolicyService;
import io.github.afgprojects.framework.security.auth.properties.casbin.CasbinConfig;
import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CasbinAfgEnforcer 测试
 *
 * <p>由于 Casbin 的 FileAdapter 不支持运行时 addPolicy（autoSave 时报 "Method not implemented"），
 * 测试通过直接操作底层 Enforcer 来添加策略，验证 enforce 的正确性。
 */
@DisplayName("CasbinAfgEnforcer 测试")
class CasbinAfgEnforcerTest {

    private InMemoryPolicyService policyService;
    private CasbinConfig casbinConfig;
    private CasbinAfgEnforcer enforcer;

    @BeforeEach
    void setUp() {
        policyService = new InMemoryPolicyService();
        casbinConfig = new CasbinConfig();
        casbinConfig.setEnabled(true);
        casbinConfig.setModelType("rbac-domain");
        casbinConfig.setAutoSave(false);
        casbinConfig.setAutoBuildRoleLinks(true);
        enforcer = new CasbinAfgEnforcer(casbinConfig, policyService);
    }

    /**
     * 通过底层 Enforcer 直接添加策略（绕过 FileAdapter 的 autoSave 限制）
     */
    private void addPolicyDirectly(String subject, String domain, String resource, String action) {
        Enforcer enf = enforcer.getEnforcer();
        enf.addPolicy(subject, domain, resource, action);
    }

    private void addRoleDirectly(String user, String domain, String role) {
        Enforcer enf = enforcer.getEnforcer();
        enf.addGroupingPolicy(user, domain, role);
    }

    @Nested
    @DisplayName("enforce 方法 - 基本权限判定")
    class EnforceTests {

        @Test
        @DisplayName("无策略时应拒绝所有请求")
        void shouldDenyAllWhenNoPolicies() {
            boolean result = enforcer.enforce("user1", "tenant1", "resource1", "read");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("subject 为 null 时应拒绝")
        void shouldDenyWhenSubjectIsNull() {
            boolean result = enforcer.enforce(null, "tenant1", "resource1", "read");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("已添加策略后应允许匹配的请求")
        void shouldAllowMatchingRequest() {
            addPolicyDirectly("user1", "tenant1", "resource1", "read");

            boolean result = enforcer.enforce("user1", "tenant1", "resource1", "read");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不匹配的动作应被拒绝")
        void shouldDenyNonMatchingAction() {
            addPolicyDirectly("user1", "tenant1", "resource1", "read");

            boolean result = enforcer.enforce("user1", "tenant1", "resource1", "write");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("不匹配的域应被拒绝")
        void shouldDenyNonMatchingDomain() {
            addPolicyDirectly("user1", "tenant1", "resource1", "read");

            boolean result = enforcer.enforce("user1", "tenant2", "resource1", "read");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("不匹配的主体应被拒绝")
        void shouldDenyNonMatchingSubject() {
            addPolicyDirectly("user1", "tenant1", "resource1", "read");

            boolean result = enforcer.enforce("user2", "tenant1", "resource1", "read");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("RBAC 角色继承")
    class RbacTests {

        @Test
        @DisplayName("用户继承角色后应获得角色权限")
        void shouldInheritRolePermissions() {
            addPolicyDirectly("admin", "tenant1", "resource1", "read");
            addRoleDirectly("user1", "tenant1", "admin");

            boolean result = enforcer.enforce("user1", "tenant1", "resource1", "read");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同域的角色不应交叉继承")
        void shouldNotCrossDomainInherit() {
            addPolicyDirectly("admin", "tenant1", "resource1", "read");
            addRoleDirectly("user1", "tenant2", "admin");

            boolean result = enforcer.enforce("user1", "tenant1", "resource1", "read");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("用户可以有多个角色")
        void shouldSupportMultipleRoles() {
            addPolicyDirectly("reader", "tenant1", "resource1", "read");
            addPolicyDirectly("writer", "tenant1", "resource1", "write");
            addRoleDirectly("user1", "tenant1", "reader");
            addRoleDirectly("user1", "tenant1", "writer");

            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "read")).isTrue();
            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "write")).isTrue();
        }
    }

    @Nested
    @DisplayName("addPolicy 和 removePolicy 方法 - 策略服务操作")
    class AddAndRemovePolicyTests {

        @Test
        @DisplayName("addPolicy 应保存策略到策略服务")
        void shouldSavePolicyToService() {
            enforcer.addPolicy("user1", "tenant1", "resource1", "read");

            assertThat(policyService.loadPolicies()).hasSize(1);
            assertThat(policyService.loadPolicies().get(0).getPtype()).isEqualTo("p");
        }

        @Test
        @DisplayName("removePolicy 应从策略服务中删除策略")
        void shouldRemovePolicyFromService() {
            enforcer.addPolicy("user1", "tenant1", "resource1", "read");
            enforcer.removePolicy("user1", "tenant1", "resource1", "read");

            assertThat(policyService.loadPolicies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addRoleForUser 和 deleteRoleForUser 方法")
    class RoleManagementTests {

        @Test
        @DisplayName("addRoleForUser 应保存角色到策略服务")
        void shouldSaveRoleToService() {
            enforcer.addRoleForUser("user1", "tenant1", "admin");

            assertThat(policyService.loadPolicies()).hasSize(1);
            assertThat(policyService.loadPolicies().get(0).getPtype()).isEqualTo("g");
        }

        @Test
        @DisplayName("deleteRoleForUser 应从策略服务中删除角色")
        void shouldDeleteRoleFromService() {
            enforcer.addRoleForUser("user1", "tenant1", "admin");
            enforcer.deleteRoleForUser("user1", "tenant1", "admin");

            assertThat(policyService.loadPolicies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("域（租户）隔离")
    class DomainIsolationTests {

        @Test
        @DisplayName("不同租户的策略应完全隔离")
        void shouldIsolatePoliciesByTenant() {
            addPolicyDirectly("admin", "tenant1", "resource1", "read");
            addPolicyDirectly("admin", "tenant2", "resource1", "write");
            addRoleDirectly("user1", "tenant1", "admin");
            addRoleDirectly("user1", "tenant2", "admin");

            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "read")).isTrue();
            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "write")).isFalse();
            assertThat(enforcer.enforce("user1", "tenant2", "resource1", "read")).isFalse();
            assertThat(enforcer.enforce("user1", "tenant2", "resource1", "write")).isTrue();
        }
    }

    @Nested
    @DisplayName("clearPolicies 方法")
    class ClearPoliciesTests {

        @Test
        @DisplayName("clearPolicies 应清空所有策略")
        void shouldClearAllPolicies() {
            addPolicyDirectly("user1", "tenant1", "resource1", "read");
            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "read")).isTrue();

            enforcer.clearPolicies();

            assertThat(enforcer.enforce("user1", "tenant1", "resource1", "read")).isFalse();
            assertThat(policyService.loadPolicies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("从已有策略初始化")
    class InitializationTests {

        @Test
        @DisplayName("应能从策略服务加载已有策略")
        void shouldLoadExistingPoliciesOnInitialization() {
            policyService.savePolicy(CasbinRule.createPolicy("admin", "tenant1", "resource1", "read"));
            policyService.savePolicy(CasbinRule.createRole("user1", "tenant1", "admin"));

            CasbinAfgEnforcer newEnforcer = new CasbinAfgEnforcer(casbinConfig, policyService);

            assertThat(newEnforcer.enforce("user1", "tenant1", "resource1", "read")).isTrue();
        }

        @Test
        @DisplayName("空策略服务应正常初始化")
        void shouldInitializeWithEmptyPolicyService() {
            CasbinAfgEnforcer newEnforcer = new CasbinAfgEnforcer(casbinConfig, policyService);

            assertThat(newEnforcer.enforce("user1", "tenant1", "resource1", "read")).isFalse();
        }
    }

    @Nested
    @DisplayName("getEnforcer 方法")
    class GetEnforcerTests {

        @Test
        @DisplayName("应返回底层 Casbin Enforcer")
        void shouldReturnUnderlyingEnforcer() {
            assertThat(enforcer.getEnforcer()).isNotNull();
        }
    }
}