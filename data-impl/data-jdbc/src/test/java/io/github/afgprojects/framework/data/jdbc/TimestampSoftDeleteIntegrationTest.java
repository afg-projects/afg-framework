package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.entity.TimestampSoftDeleteEntity;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 时间戳软删除集成测试
 * <p>
 * 测试 TimestampSoftDeleteEntity 的软删除功能。
 */
@DisplayName("时间戳软删除集成测试")
class TimestampSoftDeleteIntegrationTest {

    private JdbcDataManager dataManager;
    private EntityProxy<TimestampUser> userProxy;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
        createTestTable();
        userProxy = dataManager.entity(TimestampUser.class);
    }

    @AfterEach
    void tearDown() {
        dropTestTable();
    }

    @Nested
    @DisplayName("软删除策略检测测试")
    class StrategyDetectionTests {

        @Test
        @DisplayName("应该正确检测时间戳软删除策略")
        void shouldDetectTimestampStrategy() {
            // Given
            JdbcEntityProxy<TimestampUser> proxy = (JdbcEntityProxy<TimestampUser>) userProxy;

            // When & Then
            assertThat(proxy.isSoftDeletable()).isTrue();
            assertThat(proxy.getSoftDeleteStrategy()).isEqualTo(SoftDeleteStrategy.TIMESTAMP);
        }
    }

    @Nested
    @DisplayName("软删除操作测试")
    class SoftDeleteTests {

        @Test
        @DisplayName("应该正确执行软删除")
        void shouldSoftDeleteEntity() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("soft-delete-user");
            user.setEmail("soft@example.com");
            user = userProxy.insert(user);
            Long userId = user.getId();

            // When
            userProxy.delete(user);

            // Then - 普通查询找不到
            assertThat(userProxy.findById(userId)).isEmpty();

            // 使用 includeDeleted 可以查到
            Optional<TimestampUser> deleted = userProxy.includeDeleted().findById(userId);
            assertThat(deleted).isPresent();
            assertThat(deleted.get().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该正确按 ID 软删除")
        void shouldSoftDeleteById() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("delete-by-id");
            user.setEmail("byid@example.com");
            user = userProxy.insert(user);
            Long userId = user.getId();

            // When
            userProxy.deleteById(userId);

            // Then
            assertThat(userProxy.findById(userId)).isEmpty();
            assertThat(userProxy.includeDeleted().findById(userId)).isPresent();
        }

        @Test
        @DisplayName("应该正确批量软删除")
        void shouldSoftDeleteAll() {
            // Given
            TimestampUser u1 = new TimestampUser();
            u1.setName("batch1"); u1.setEmail("b1@example.com");
            u1 = userProxy.insert(u1);
            TimestampUser u2 = new TimestampUser();
            u2.setName("batch2"); u2.setEmail("b2@example.com");
            u2 = userProxy.insert(u2);

            // When
            userProxy.deleteAll(List.of(u1, u2));

            // Then
            assertThat(userProxy.findById(u1.getId())).isEmpty();
            assertThat(userProxy.findById(u2.getId())).isEmpty();
            assertThat(userProxy.includeDeleted().findById(u1.getId())).isPresent();
            assertThat(userProxy.includeDeleted().findById(u2.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("恢复操作测试")
    class RestoreTests {

        @Test
        @DisplayName("应该正确恢复已删除记录")
        void shouldRestoreDeletedEntity() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("restore-user");
            user.setEmail("restore@example.com");
            user = userProxy.insert(user);
            userProxy.delete(user);

            // 验证已删除
            assertThat(userProxy.findById(user.getId())).isEmpty();

            // When
            userProxy.restoreById(user.getId());

            // Then
            Optional<TimestampUser> restored = userProxy.findById(user.getId());
            assertThat(restored).isPresent();
            assertThat(restored.get().getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("应该正确批量恢复已删除记录")
        void shouldRestoreAllById() {
            // Given
            TimestampUser u1 = new TimestampUser();
            u1.setName("restore-batch1"); u1.setEmail("rb1@example.com");
            u1 = userProxy.insert(u1);
            TimestampUser u2 = new TimestampUser();
            u2.setName("restore-batch2"); u2.setEmail("rb2@example.com");
            u2 = userProxy.insert(u2);
            userProxy.deleteAll(List.of(u1, u2));

            // When
            userProxy.restoreAllById(List.of(u1.getId(), u2.getId()));

            // Then
            assertThat(userProxy.findById(u1.getId())).isPresent();
            assertThat(userProxy.findById(u2.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("查询自动过滤测试")
    class QueryFilterTests {

        @Test
        @DisplayName("findAll 应该自动过滤已删除记录")
        void shouldFilterDeletedInFindAll() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("active-user");
            active.setEmail("active@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("deleted-user");
            deleted.setEmail("deleted@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            List<TimestampUser> all = userProxy.findAll();

            // Then
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getName()).isEqualTo("active-user");
        }

        @Test
        @DisplayName("findAllById 应该自动过滤已删除记录")
        void shouldFilterDeletedInFindAllById() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("active");
            active.setEmail("active@example.com");
            active = userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("deleted");
            deleted.setEmail("deleted@example.com");
            deleted = userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            List<TimestampUser> found = userProxy.findAllById(List.of(active.getId(), deleted.getId()));

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getName()).isEqualTo("active");
        }

        @Test
        @DisplayName("count 应该自动过滤已删除记录")
        void shouldFilterDeletedInCount() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("active");
            active.setEmail("active@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("deleted");
            deleted.setEmail("deleted@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            long count = userProxy.count();

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("count(Condition) 应该自动过滤已删除记录")
        void shouldFilterDeletedInCountWithCondition() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("active");
            active.setEmail("active@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("deleted");
            deleted.setEmail("deleted@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When - 使用条件查询 count
            long count = userProxy.count(Conditions.like("email", "example"));

            // Then - 应该只统计未删除的记录
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("条件查询应该自动过滤已删除记录")
        void shouldFilterDeletedInConditionQuery() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("user-1");
            active.setEmail("user1@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("user-2");
            deleted.setEmail("user2@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            List<TimestampUser> found = userProxy.findAll(Conditions.like("email", "example"));

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getName()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("分页查询应该自动过滤已删除记录")
        void shouldFilterDeletedInPagedQuery() {
            // Given
            for (int i = 1; i <= 5; i++) {
                TimestampUser user = new TimestampUser();
                user.setName("user-" + i);
                user.setEmail("user" + i + "@example.com");
                userProxy.insert(user);
            }
            // 删除 2 条
            TimestampUser toDelete1 = userProxy.findAll().get(0);
            TimestampUser toDelete2 = userProxy.findAll().get(1);
            userProxy.delete(toDelete1);
            userProxy.delete(toDelete2);

            // When
            Page<TimestampUser> page = userProxy.findAll(
                Conditions.like("email", "example"),
                PageRequest.of(1, 10)
            );

            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotal()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("includeDeleted 测试")
    class IncludeDeletedTests {

        @Test
        @DisplayName("includeDeleted 应该查询到已删除记录")
        void shouldIncludeDeletedRecords() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("active");
            active.setEmail("active@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("deleted");
            deleted.setEmail("deleted@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            List<TimestampUser> allWithDeleted = userProxy.includeDeleted().findAll();

            // Then
            assertThat(allWithDeleted).hasSize(2);
        }

        @Test
        @DisplayName("includeDeleted 条件查询应该包含已删除记录")
        void shouldIncludeDeletedInConditionQuery() {
            // Given
            TimestampUser active = new TimestampUser();
            active.setName("user-active");
            active.setEmail("active@example.com");
            userProxy.insert(active);

            TimestampUser deleted = new TimestampUser();
            deleted.setName("user-deleted");
            deleted.setEmail("deleted@example.com");
            userProxy.insert(deleted);
            userProxy.delete(deleted);

            // When
            List<TimestampUser> found = userProxy.includeDeleted()
                .findAll(Conditions.like("name", "user"));

            // Then
            assertThat(found).hasSize(2);
        }
    }

    @Nested
    @DisplayName("物理删除测试")
    class HardDeleteTests {

        @Test
        @DisplayName("hardDelete 应该物理删除记录")
        void shouldHardDeleteEntity() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("hard-delete");
            user.setEmail("hard@example.com");
            user = userProxy.insert(user);
            Long userId = user.getId();

            // When
            ((JdbcEntityProxy<TimestampUser>) userProxy).hardDelete(user);

            // Then - 即使使用 includeDeleted 也找不到
            assertThat(userProxy.includeDeleted().findById(userId)).isEmpty();
        }

        @Test
        @DisplayName("hardDeleteById 应该物理删除记录")
        void shouldHardDeleteById() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("hard-delete-by-id");
            user.setEmail("hardbyid@example.com");
            user = userProxy.insert(user);
            Long userId = user.getId();

            // When
            ((JdbcEntityProxy<TimestampUser>) userProxy).hardDeleteById(userId);

            // Then
            assertThat(userProxy.includeDeleted().findById(userId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("实体标记删除测试")
    class EntityMarkDeleteTests {

        @Test
        @DisplayName("实体 markDeleted 方法应该设置删除时间")
        void shouldMarkDeletedOnEntity() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("mark-deleted");
            user.setEmail("mark@example.com");

            // When
            user.markDeleted();

            // Then
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("实体 restore 方法应该清除删除时间")
        void shouldRestoreOnEntity() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("restore");
            user.setEmail("restore@example.com");
            user.markDeleted();

            // When
            user.restore();

            // Then
            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("应该支持指定删除时间")
        void shouldSupportCustomDeletedAt() {
            // Given
            TimestampUser user = new TimestampUser();
            user.setName("custom-time");
            user.setEmail("custom@example.com");
            LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // When
            user.markDeleted(customTime);

            // Then
            assertThat(user.getDeletedAt()).isEqualTo(customTime);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:timestamp_testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE timestamp_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    deleted_at TIMESTAMP,
                    create_time TIMESTAMP,
                    update_time TIMESTAMP
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }

    private void dropTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS timestamp_user");
        } catch (Exception ignored) {
        }
    }

    /**
     * 时间戳软删除测试用户实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimestampUser extends TimestampSoftDeleteEntity<Long> {
        private String name;
        private String email;
    }
}
