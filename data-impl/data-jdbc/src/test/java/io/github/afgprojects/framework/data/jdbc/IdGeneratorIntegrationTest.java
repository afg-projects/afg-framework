package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
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
 * 插入实体前应预生成 ID（如 Snowflake ID）。
 * </p>
 * <p>
 * 注意：测试表 id 列为 VARCHAR，框架实体 ID 为 String（{@code BaseEntity.id}），
 * 统一通过 IdGenerator 生成字符型 ID，不依赖数据库自增。
 * 测试通过 {@code @BeforeEach}/{@code @AfterEach} 保存并恢复 DataManager 原有的
 * IdGenerator，避免污染共享 Spring 上下文中的单例。
 * </p>
 */
class IdGeneratorIntegrationTest extends BaseDataTest {

    private IdGenerator originalIdGenerator;

    @BeforeEach
    void setUp() {
        // 保存原 IdGenerator 以便测试后恢复，避免污染共享上下文中的单例 DataManager
        originalIdGenerator = ((JdbcDataManager) dataManager).getIdGenerator();
    }

    @AfterEach
    void tearDown() {
        ((JdbcDataManager) dataManager).setIdGenerator(originalIdGenerator);
    }

    @Nested
    @DisplayName("默认 IdGenerator 预生成 ID")
    class DefaultIdGenerator {

        @Test
        @DisplayName("should pre-generate id when using default IdGenerator")
        void shouldPreGenerateId_whenUsingDefaultIdGenerator() {
            TestUser user = createUser("default-idgen");
            TestUser saved = dataManager.save(TestUser.class, user);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("should find entity by pre-generated id when default IdGenerator used")
        void shouldFindEntityByPreGeneratedId_whenDefaultIdGeneratorUsed() {
            TestUser saved = dataManager.save(TestUser.class, createUser("default-find"));

            TestUser found = dataManager.findById(TestUser.class, saved.getId()).orElseThrow();
            assertThat(found.getUsername()).isEqualTo("default-find");
        }
    }

    @Nested
    @DisplayName("自定义 IdGenerator 预生成 ID")
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
    @DisplayName("IdGenerator 运行时切换")
    class IdGeneratorSwitch {

        @Test
        @DisplayName("should switch between IdGenerators at runtime")
        void shouldSwitchBetweenIdGenerators_atRuntime() {
            // 先用默认 IdGenerator
            TestUser user1 = dataManager.save(TestUser.class, createUser("default-first"));
            assertThat(user1.getId()).isNotNull();

            // 运行时切换到另一个 IdGenerator
            ((JdbcDataManager) dataManager).setIdGenerator(new TestSnowflakeIdGenerator());

            TestUser user2 = dataManager.save(TestUser.class, createUser("idgen-switch"));
            assertThat(user2.getId()).isNotNull();
            assertThat(user1.getId()).isNotEqualTo(user2.getId());
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
