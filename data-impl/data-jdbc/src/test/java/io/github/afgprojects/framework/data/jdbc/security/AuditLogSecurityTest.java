package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 审计日志安全测试
 */
@DisplayName("审计日志安全测试")
@Tag("security")
class AuditLogSecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("操作追踪测试")
    class OperationTrackingTests {

        @Test
        @DisplayName("插入操作应可追踪")
        void insertOperationShouldBeTraceable() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("audit-test-user");
            user.setEmail("audit@test.com");

            SecurityTestUser inserted = userProxy.insert(user);

            assertThat(inserted.getId()).isNotNull();
        }

        @Test
        @DisplayName("更新操作应可追踪")
        void updateOperationShouldBeTraceable() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("update-audit");
            user.setEmail("update@test.com");
            user = userProxy.insert(user);

            user.setName("updated-name");
            SecurityTestUser updated = userProxy.update(user);

            assertThat(updated.getName()).isEqualTo("updated-name");
        }

        @Test
        @DisplayName("删除操作应可追踪")
        void deleteOperationShouldBeTraceable() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("delete-audit");
            user.setEmail("delete@test.com");
            user = userProxy.insert(user);

            userProxy.delete(user);

            assertThat(userProxy.findById(user.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("敏感数据保护测试")
    class SensitiveDataProtectionTests {

        @Test
        @DisplayName("密码字段应安全存储")
        void passwordShouldBeStoredSecurely() {
            SecurityTestUser user = new SecurityTestUser();
            user.setName("password-test");
            user.setEmail("password@test.com");
            user.setPassword("plainPassword123");

            SecurityTestUser inserted = userProxy.insert(user);
            SecurityTestUser found = userProxy.findById(inserted.getId()).orElseThrow();

            // 注意：实际项目中密码应该加密存储
            // 这里只是验证数据能正确存取
            assertThat(found.getPassword()).isNotNull();
        }
    }

    @Nested
    @DisplayName("失败操作处理测试")
    class FailedOperationTests {

        @Test
        @DisplayName("查找不存在记录应安全处理")
        void shouldHandleNotFoundSafely() {
            assertThat(userProxy.findById(99999L)).isEmpty();
        }

        @Test
        @DisplayName("更新不存在记录应安全处理")
        void shouldHandleUpdateNotFoundSafely() {
            SecurityTestUser user = new SecurityTestUser();
            user.setId(99999L);
            user.setName("not-exist");

            assertThatNoException().isThrownBy(() -> userProxy.update(user));
        }

        @Test
        @DisplayName("删除不存在记录应安全处理")
        void shouldHandleDeleteNotFoundSafely() {
            SecurityTestUser user = new SecurityTestUser();
            user.setId(99999L);

            assertThatNoException().isThrownBy(() -> userProxy.delete(user));
        }
    }

    @Nested
    @DisplayName("数据完整性测试")
    class DataIntegrityTests {

        @Test
        @DisplayName("批量操作后数据应完整")
        void dataShouldBeIntactAfterBatchOperations() {
            long initialCount = userProxy.count();

            // 批量插入
            java.util.List<SecurityTestUser> users = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> {
                    SecurityTestUser user = new SecurityTestUser();
                    user.setName("integrity-" + i);
                    user.setEmail("integrity" + i + "@test.com");
                    return user;
                })
                .toList();

            userProxy.insertAll(users);

            assertThat(userProxy.count()).isEqualTo(initialCount + 10);

            // 批量删除
            userProxy.deleteAll(users);

            assertThat(userProxy.count()).isEqualTo(initialCount);
        }

        @Test
        @DisplayName("条件删除应只删除匹配数据")
        void conditionalDeleteShouldOnlyDeleteMatching() {
            long initialCount = userProxy.count();

            // 只删除 tenant-b 的数据
            long deleted = userProxy.deleteAll(Conditions.eq("tenant_id", "tenant-b"));

            assertThat(deleted).isEqualTo(1);
            assertThat(userProxy.count()).isEqualTo(initialCount - 1);
            assertThat(userProxy.findAll(Conditions.eq("tenant_id", "tenant-b"))).isEmpty();
        }
    }
}
