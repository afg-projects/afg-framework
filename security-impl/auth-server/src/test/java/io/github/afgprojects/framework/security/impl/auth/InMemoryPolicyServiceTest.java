package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.casbin.model.CasbinRule;
import io.github.afgprojects.framework.security.auth.casbin.model.InMemoryPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryPolicyService 测试
 */
@DisplayName("InMemoryPolicyService 测试")
class InMemoryPolicyServiceTest {

    private InMemoryPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new InMemoryPolicyService();
    }

    @Nested
    @DisplayName("loadPolicies 方法")
    class LoadPoliciesTests {

        @Test
        @DisplayName("初始状态应返回空列表")
        void shouldReturnEmptyListWhenNoPolicies() {
            List<CasbinRule> policies = policyService.loadPolicies();

            assertThat(policies).isEmpty();
        }

        @Test
        @DisplayName("添加策略后应返回策略列表")
        void shouldReturnPoliciesAfterSave() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            policyService.savePolicy(rule);

            List<CasbinRule> policies = policyService.loadPolicies();

            assertThat(policies).hasSize(1);
        }
    }

    @Nested
    @DisplayName("savePolicy 方法")
    class SavePolicyTests {

        @Test
        @DisplayName("应能保存策略规则")
        void shouldSavePolicyRule() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            policyService.savePolicy(rule);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).hasSize(1);
            assertThat(policies.get(0).getPtype()).isEqualTo("p");
            assertThat(policies.get(0).getV0()).isEqualTo("user1");
        }

        @Test
        @DisplayName("应能保存角色规则")
        void shouldSaveRoleRule() {
            CasbinRule role = CasbinRule.createRole("user1", "tenant1", "admin");

            policyService.savePolicy(role);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).hasSize(1);
            assertThat(policies.get(0).getPtype()).isEqualTo("g");
        }

        @Test
        @DisplayName("应能保存多个规则")
        void shouldSaveMultipleRules() {
            CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule rule2 = CasbinRule.createPolicy("user2", "tenant1", "resource2", "write");
            CasbinRule role = CasbinRule.createRole("user1", "tenant1", "admin");

            policyService.savePolicy(rule1);
            policyService.savePolicy(rule2);
            policyService.savePolicy(role);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).hasSize(3);
        }
    }

    @Nested
    @DisplayName("removePolicy 方法")
    class RemovePolicyTests {

        @Test
        @DisplayName("应能删除已存在的规则")
        void shouldRemoveExistingRule() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            policyService.savePolicy(rule);

            policyService.removePolicy(rule);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).isEmpty();
        }

        @Test
        @DisplayName("删除不存在的规则应无效果")
        void shouldDoNothingWhenRuleNotExists() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            policyService.removePolicy(rule);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).isEmpty();
        }

        @Test
        @DisplayName("应只删除匹配的规则")
        void shouldOnlyRemoveMatchingRule() {
            CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule rule2 = CasbinRule.createPolicy("user2", "tenant1", "resource2", "write");
            policyService.savePolicy(rule1);
            policyService.savePolicy(rule2);

            policyService.removePolicy(rule1);

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).hasSize(1);
            assertThat(policies.get(0).getV0()).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("clearPolicies 方法")
    class ClearPoliciesTests {

        @Test
        @DisplayName("应清空所有规则")
        void shouldClearAllPolicies() {
            policyService.savePolicy(CasbinRule.createPolicy("user1", "tenant1", "resource1", "read"));
            policyService.savePolicy(CasbinRule.createPolicy("user2", "tenant1", "resource2", "write"));
            policyService.savePolicy(CasbinRule.createRole("user1", "tenant1", "admin"));

            policyService.clearPolicies();

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).isEmpty();
        }

        @Test
        @DisplayName("空存储清空应无效果")
        void shouldDoNothingWhenEmpty() {
            policyService.clearPolicies();

            List<CasbinRule> policies = policyService.loadPolicies();
            assertThat(policies).isEmpty();
        }
    }
}
