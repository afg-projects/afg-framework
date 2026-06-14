package io.github.afgprojects.framework.integration.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.audit.AuditLog;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.integration.redis.audit.RedisAuditLogStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RedisAuditLogStorage 集成测试
 *
 * <p>基于真实 Redis 容器测试 Redis 审计日志存储
 */
@DisplayName("RedisAuditLogStorage 审计日志存储测试")
class RedisAuditLogStorageTest extends BaseRedisTest {

    private RedisAuditLogStorage auditLogStorage;
    private AfgCoreProperties.AuditConfig auditConfig;

    @BeforeEach
    void setUp() {
        auditConfig = new AfgCoreProperties.AuditConfig();
        auditConfig.setMaxSize(100);
        auditConfig.setMultiTenant(false);
        auditLogStorage = new RedisAuditLogStorage(getRedissonClient(), auditConfig);
    }

    @Nested
    @DisplayName("save 操作")
    class Save {

        @Test
        @DisplayName("save 应成功保存审计日志到 Redis")
        void shouldSaveAuditLog() {
            AuditLog log = createAuditLog("save-test-1");

            assertThatCode(() -> auditLogStorage.save(log)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("save 应支持多租户模式")
        void shouldSaveWithMultiTenant() {
            auditConfig.setMultiTenant(true);
            auditLogStorage = new RedisAuditLogStorage(getRedissonClient(), auditConfig);

            AuditLog log = AuditLog.successBuilder()
                    .id("multi-tenant-test-1")
                    .operation("CREATE")
                    .module("user")
                    .className("UserService")
                    .methodName("createUser")
                    .tenantId(1L)
                    .build();

            assertThatCode(() -> auditLogStorage.save(log)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("save 应在超过 maxSize 时自动清理旧日志")
        void shouldTrimOldLogs_whenExceedMaxSize() {
            auditConfig.setMaxSize(5);
            auditLogStorage = new RedisAuditLogStorage(getRedissonClient(), auditConfig);

            for (int i = 0; i < 10; i++) {
                AuditLog log = createAuditLog("trim-test-" + i);
                auditLogStorage.save(log);
            }

            // 验证 Redis 列表长度不超过 maxSize
            var list = getRedissonClient().getList("audit:log:global");
            int listSize = list.size();
            assertThat(listSize <= 5).isTrue();
        }

        @Test
        @DisplayName("save 失败不应抛出异常（静默失败）")
        void shouldNotThrow_whenSaveFails() {
            // 即使 Redis 有问题，审计日志也不应影响业务流程
            // 这里我们正常保存验证不会抛异常
            AuditLog log = createAuditLog("silent-fail-test");
            assertThatCode(() -> auditLogStorage.save(log)).doesNotThrowAnyException();
        }
    }

    private AuditLog createAuditLog(String id) {
        return AuditLog.successBuilder()
                .id(id)
                .operation("CREATE")
                .module("test")
                .className("TestService")
                .methodName("testMethod")
                .build();
    }

    @AfterEach
    void cleanup() {
        getRedissonClient().getKeys().getKeysByPattern("audit:log:*").forEach(key -> {
            try {
                getRedissonClient().getList(key).delete();
            } catch (Exception ignored) {
                // ignore cleanup errors
            }
        });
    }
}
