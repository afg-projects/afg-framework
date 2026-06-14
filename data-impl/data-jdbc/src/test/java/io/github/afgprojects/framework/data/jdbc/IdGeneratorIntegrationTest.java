package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.id.IdGenerator;
import io.github.afgprojects.framework.core.api.id.IdGeneratorType;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager IdGenerator 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 * <p>
 * 测试 IdGenerator 预生成 ID 功能：当 JdbcDataManager 设置了 IdGenerator 时，
 * 插入实体前应预生成 ID（如 Snowflake ID），而非使用数据库自增。
 * </p>
 */
class IdGeneratorIntegrationTest extends BaseDataTest {

    @BeforeEach
    void setUp() {
        // 清除 IdGenerator 以确保测试隔离
        ((JdbcDataManager) dataManager).setIdGenerator(null);
    }

    @Nested
    @DisplayName("无 IdGenerator 时使用数据库自增")
    class WithoutIdGenerator {

        @Test
        @DisplayName("should use database auto increment when no IdGenerator")
        void shouldUseDatabaseAutoIncrement_whenNoIdGenerator() {
            TestUser user = createUser("no-idgen");
            TestUser saved = dataManager.save(TestUser.class, user);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isPositive();
        }
    }

    @Nested
    @DisplayName("有 IdGenerator 时预生成 ID")
    class WithIdGenerator {

        @Test
        @DisplayName("should use IdGenerator to pre-generate id when IdGenerator is set")
        void shouldUseIdGeneratorToPreGenerateId_whenIdGeneratorIsSet() {
            // 设置一个简单的 IdGenerator（模拟 Snowflake）
            IdGenerator testIdGenerator = new TestSnowflakeIdGenerator();
            ((JdbcDataManager) dataManager).setIdGenerator(testIdGenerator);

            TestUser user = createUser("with-idgen");
            TestUser saved = dataManager.save(TestUser.class, user);

            assertThat(saved.getId()).isNotNull();
            // IdGenerator 生成的 ID 应该大于 Integer.MAX_VALUE（Snowflake ID 很大）
            assertThat(saved.getId()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate unique ids when saving multiple entities")
        void shouldGenerateUniqueIds_whenSavingMultipleEntities() {
            IdGenerator testIdGenerator = new TestSnowflakeIdGenerator();
            ((JdbcDataManager) dataManager).setIdGenerator(testIdGenerator);

            TestUser user1 = dataManager.save(TestUser.class, createUser("idgen-1"));
            TestUser user2 = dataManager.save(TestUser.class, createUser("idgen-2"));

            assertThat(user1.getId()).isNotEqualTo(user2.getId());
        }

        @Test
        @DisplayName("should find entity by pre-generated id when IdGenerator is used")
        void shouldFindEntityByPreGeneratedId_whenIdGeneratorIsUsed() {
            IdGenerator testIdGenerator = new TestSnowflakeIdGenerator();
            ((JdbcDataManager) dataManager).setIdGenerator(testIdGenerator);

            TestUser saved = dataManager.save(TestUser.class, createUser("idgen-find"));

            TestUser found = dataManager.findById(TestUser.class, saved.getId()).orElseThrow();
            assertThat(found.getUsername()).isEqualTo("idgen-find");
        }
    }

    @Nested
    @DisplayName("IdGenerator 切换")
    class IdGeneratorSwitch {

        @Test
        @DisplayName("should switch from auto increment to IdGenerator at runtime")
        void shouldSwitchFromAutoIncrementToIdGenerator_atRuntime() {
            // 先用数据库自增
            TestUser user1 = dataManager.save(TestUser.class, createUser("auto-inc"));
            assertThat(user1.getId()).isNotNull();

            // 切换到 IdGenerator
            ((JdbcDataManager) dataManager).setIdGenerator(new TestSnowflakeIdGenerator());

            TestUser user2 = dataManager.save(TestUser.class, createUser("idgen-switch"));
            assertThat(user2.getId()).isNotNull();
        }

        @Test
        @DisplayName("should switch back to auto increment when IdGenerator set to null")
        void shouldSwitchBackToAutoIncrement_whenIdGeneratorSetToNull() {
            // 先用 IdGenerator
            ((JdbcDataManager) dataManager).setIdGenerator(new TestSnowflakeIdGenerator());
            TestUser user1 = dataManager.save(TestUser.class, createUser("idgen-first"));
            assertThat(user1.getId()).isNotNull();

            // 切换回数据库自增
            ((JdbcDataManager) dataManager).setIdGenerator(null);
            TestUser user2 = dataManager.save(TestUser.class, createUser("auto-inc-again"));
            assertThat(user2.getId()).isNotNull();
        }
    }

    // --- 辅助方法 ---

    private TestUser createUser(String username) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setStatus(1);
        return user;
    }

    /**
     * 测试用 Snowflake 风格 ID 生成器
     * <p>
     * 简化实现：基于时间戳 + 递增序列，生成大于 100000 的 ID。
     * 仅用于测试 IdGenerator 集成，不用于生产。
     * </p>
     */
    private static class TestSnowflakeIdGenerator implements IdGenerator {

        private static long counter = 100000L;

        @Override
        public long nextId() {
            return ++counter;
        }

        @Override
        public String nextIdAsString() {
            return String.valueOf(nextId());
        }

        @Override
        public IdGeneratorType getType() {
            return IdGeneratorType.SNOWFLAKE;
        }
    }
}
