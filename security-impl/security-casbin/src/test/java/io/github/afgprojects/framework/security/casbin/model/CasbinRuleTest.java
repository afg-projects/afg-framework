package io.github.afgprojects.framework.security.casbin.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * CasbinRule 测试
 */
class CasbinRuleTest {

    @Test
    void should_createPolicyRule() {
        CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");

        assertThat(rule.getPtype()).isEqualTo("p");
        assertThat(rule.getV0()).isEqualTo("user1");
        assertThat(rule.getV1()).isEqualTo("tenant1");
        assertThat(rule.getV2()).isEqualTo("/api/data");
        assertThat(rule.getV3()).isEqualTo("read");
    }

    @Test
    void should_createRoleRule() {
        CasbinRule rule = CasbinRule.createRole("user1", "tenant1", "admin");

        assertThat(rule.getPtype()).isEqualTo("g");
        assertThat(rule.getV0()).isEqualTo("user1");
        assertThat(rule.getV1()).isEqualTo("tenant1");
        assertThat(rule.getV2()).isEqualTo("admin");
    }

    @Test
    void should_convertToPolicyArray() {
        CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");

        String[] policy = rule.toPolicy();

        assertThat(policy).containsExactly("user1", "tenant1", "/api/data", "read");
    }

    @Test
    void should_convertToRoleArray() {
        CasbinRule rule = CasbinRule.createRole("user1", "tenant1", "admin");

        String[] role = rule.toPolicy();

        assertThat(role).containsExactly("user1", "tenant1", "admin");
    }

    @Test
    void should_haveCorrectToString() {
        CasbinRule rule = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");

        String str = rule.toString();

        assertThat(str).contains("p");
        assertThat(str).contains("user1");
        assertThat(str).contains("tenant1");
        assertThat(str).contains("/api/data");
        assertThat(str).contains("read");
    }

    @Test
    void should_checkEquality() {
        CasbinRule rule1 = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");
        CasbinRule rule2 = CasbinRule.createPolicy("user1", "tenant1", "/api/data", "read");
        CasbinRule rule3 = CasbinRule.createPolicy("user2", "tenant1", "/api/data", "read");

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1).isNotEqualTo(rule3);
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
    }
}
