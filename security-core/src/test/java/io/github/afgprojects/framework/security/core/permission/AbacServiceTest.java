package io.github.afgprojects.framework.security.core.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbacService 接口测试。
 *
 * @since 1.0.0
 */
class AbacServiceTest {

    @Test
    @DisplayName("测试 AbacService 接口定义 - enforce(三参数)")
    void testEnforceThreeParams() {
        AbacService service = new TestAbacService();
        assertTrue(service.enforce("user-001", "document-001", "read"));
        assertFalse(service.enforce("user-001", "document-001", "delete"));
    }

    @Test
    @DisplayName("测试 AbacService 接口定义 - enforce(四参数)")
    void testEnforceFourParams() {
        AbacService service = new TestAbacService();
        assertTrue(service.enforce("user-001", "tenant-001", "document-001", "read"));
        assertFalse(service.enforce("user-001", "tenant-001", "document-001", "delete"));
    }

    @Test
    @DisplayName("测试 AbacService 接口定义 - addPolicy(三参数)")
    void testAddPolicyThreeParams() {
        TestAbacService service = new TestAbacService();
        service.addPolicy("user-001", "document-002", "write");
        assertTrue(service.enforce("user-001", "document-002", "write"));
    }

    @Test
    @DisplayName("测试 AbacService 接口定义 - addPolicy(四参数)")
    void testAddPolicyFourParams() {
        TestAbacService service = new TestAbacService();
        service.addPolicy("user-001", "tenant-001", "document-002", "write");
        assertTrue(service.enforce("user-001", "tenant-001", "document-002", "write"));
    }

    @Test
    @DisplayName("测试 AbacService 接口定义 - removePolicy(三参数)")
    void testRemovePolicyThreeParams() {
        TestAbacService service = new TestAbacService();
        service.removePolicy("user-001", "document-001", "read");
        assertFalse(service.enforce("user-001", "document-001", "read"));
    }

    @Test
    @DisplayName("测试 AbacService 接口定义 - removePolicy(四参数)")
    void testRemovePolicyFourParams() {
        TestAbacService service = new TestAbacService();
        service.removePolicy("user-001", "tenant-001", "document-001", "read");
        assertFalse(service.enforce("user-001", "tenant-001", "document-001", "read"));
    }

    /**
     * 测试用 AbacService 实现。
     */
    private static class TestAbacService implements AbacService {
        // 策略存储：(subject, resource, action)
        private final Set<String> policies = new HashSet<>();
        // 带域的策略存储：(subject, domain, resource, action)
        private final Set<String> domainPolicies = new HashSet<>();

        TestAbacService() {
            policies.add("user-001:document-001:read");
            domainPolicies.add("user-001:tenant-001:document-001:read");
        }

        @Override
        public boolean enforce(String subject, String resource, String action) {
            return policies.contains(subject + ":" + resource + ":" + action);
        }

        @Override
        public boolean enforce(String subject, String domain, String resource, String action) {
            return domainPolicies.contains(subject + ":" + domain + ":" + resource + ":" + action);
        }

        @Override
        public void addPolicy(String subject, String resource, String action) {
            policies.add(subject + ":" + resource + ":" + action);
        }

        @Override
        public void addPolicy(String subject, String domain, String resource, String action) {
            domainPolicies.add(subject + ":" + domain + ":" + resource + ":" + action);
        }

        @Override
        public void removePolicy(String subject, String resource, String action) {
            policies.remove(subject + ":" + resource + ":" + action);
        }

        @Override
        public void removePolicy(String subject, String domain, String resource, String action) {
            domainPolicies.remove(subject + ":" + domain + ":" + resource + ":" + action);
        }
    }
}
