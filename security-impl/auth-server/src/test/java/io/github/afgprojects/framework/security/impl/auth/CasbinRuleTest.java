package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.casbin.model.CasbinRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CasbinRule 测试
 */
@DisplayName("CasbinRule 测试")
class CasbinRuleTest {

    @Nested
    @DisplayName("createPolicy 方法")
    class CreatePolicyTests {

        @Test
        @DisplayName("应创建策略规则")
        void shouldCreatePolicyRule() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            assertThat(rule.getPtype()).isEqualTo("p");
            assertThat(rule.getV0()).isEqualTo("user1");
            assertThat(rule.getV1()).isEqualTo("tenant1");
            assertThat(rule.getV2()).isEqualTo("resource1");
            assertThat(rule.getV3()).isEqualTo("read");
        }

        @Test
        @DisplayName("不同参数应创建不同的规则")
        void shouldCreateDifferentRulesForDifferentParams() {
            CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule rule2 = CasbinRule.createPolicy("user2", "tenant1", "resource1", "write");

            assertThat(rule1).isNotEqualTo(rule2);
        }
    }

    @Nested
    @DisplayName("createRole 方法")
    class CreateRoleTests {

        @Test
        @DisplayName("应创建角色规则")
        void shouldCreateRoleRule() {
            CasbinRule rule = CasbinRule.createRole("user1", "tenant1", "admin");

            assertThat(rule.getPtype()).isEqualTo("g");
            assertThat(rule.getV0()).isEqualTo("user1");
            assertThat(rule.getV1()).isEqualTo("tenant1");
            assertThat(rule.getV2()).isEqualTo("admin");
            assertThat(rule.getV3()).isNull();
        }

        @Test
        @DisplayName("不同参数应创建不同的角色规则")
        void shouldCreateDifferentRoleRulesForDifferentParams() {
            CasbinRule role1 = CasbinRule.createRole("user1", "tenant1", "admin");
            CasbinRule role2 = CasbinRule.createRole("user1", "tenant1", "editor");

            assertThat(role1).isNotEqualTo(role2);
        }
    }

    @Nested
    @DisplayName("toPolicy 方法")
    class ToPolicyTests {

        @Test
        @DisplayName("策略规则应转换为 4 元素数组")
        void shouldConvertPolicyToArray() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            String[] policy = rule.toPolicy();

            assertThat(policy).containsExactly("user1", "tenant1", "resource1", "read");
        }

        @Test
        @DisplayName("角色规则应转换为 3 元素数组")
        void shouldConvertRoleToArray() {
            CasbinRule rule = CasbinRule.createRole("user1", "tenant1", "admin");

            String[] policy = rule.toPolicy();

            assertThat(policy).containsExactly("user1", "tenant1", "admin");
        }

        @Test
        @DisplayName("未知类型应返回空数组")
        void shouldReturnEmptyArrayForUnknownType() {
            CasbinRule rule = new CasbinRule("x", null, null, null, null);

            String[] policy = rule.toPolicy();

            assertThat(policy).isEmpty();
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("相同参数的策略应相等")
        void shouldBeEqualWhenSameParams() {
            CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule rule2 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            assertThat(rule1).isEqualTo(rule2);
            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }

        @Test
        @DisplayName("不同类型的规则应不相等")
        void shouldNotBeEqualWhenDifferentType() {
            CasbinRule policy = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule role = CasbinRule.createRole("user1", "tenant1", "resource1");

            assertThat(policy).isNotEqualTo(role);
        }

        @Test
        @DisplayName("不同字段的规则应不相等")
        void shouldNotBeEqualWhenDifferentFields() {
            CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");
            CasbinRule rule2 = CasbinRule.createPolicy("user1", "tenant1", "resource1", "write");

            assertThat(rule1).isNotEqualTo(rule2);
        }
    }

    @Nested
    @DisplayName("toString 方法")
    class ToStringTests {

        @Test
        @DisplayName("策略规则的 toString 应包含所有字段")
        void shouldIncludeAllFieldsInPolicyToString() {
            CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "resource1", "read");

            String str = rule.toString();

            assertThat(str).contains("p");
            assertThat(str).contains("user1");
            assertThat(str).contains("tenant1");
            assertThat(str).contains("resource1");
            assertThat(str).contains("read");
        }

        @Test
        @DisplayName("角色规则的 toString 应包含 3 个字段")
        void shouldIncludeFieldsInRoleToString() {
            CasbinRule rule = CasbinRule.createRole("user1", "tenant1", "admin");

            String str = rule.toString();

            assertThat(str).contains("g");
            assertThat(str).contains("user1");
            assertThat(str).contains("tenant1");
            assertThat(str).contains("admin");
        }
    }
}