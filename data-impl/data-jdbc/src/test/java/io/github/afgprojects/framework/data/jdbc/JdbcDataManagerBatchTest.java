package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 批量操作集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerBatchTest extends BaseDataTest {

    @Nested
    @DisplayName("saveAll 批量保存")
    class SaveAll {

        @Test
        @DisplayName("should save all entities when saveAll with new entities")
        void shouldSaveAllEntities_whenSaveAllWithNewEntities() {
            List<TestUser> users = List.of(
                createUser("batch-1"),
                createUser("batch-2"),
                createUser("batch-3")
            );

            List<TestUser> saved = dataManager.saveAll(TestUser.class, users);

            assertThat(saved).hasSize(3);
            assertThat(saved).allMatch(u -> u.getId() != null);
            assertThat(saved).extracting(TestUser::getUsername)
                .containsExactly("batch-1", "batch-2", "batch-3");
        }

        @Test
        @DisplayName("should update existing entities when saveAll with entities having id")
        void shouldUpdateExistingEntities_whenSaveAllWithEntitiesHavingId() {
            TestUser user1 = dataManager.save(TestUser.class, createUser("batch-update-1"));
            TestUser user2 = dataManager.save(TestUser.class, createUser("batch-update-2"));

            user1.setEmail("updated1@test.com");
            user2.setEmail("updated2@test.com");

            List<TestUser> updated = dataManager.saveAll(TestUser.class, List.of(user1, user2));

            assertThat(updated).hasSize(2);
            assertThat(updated).extracting(TestUser::getEmail)
                .containsExactly("updated1@test.com", "updated2@test.com");
        }
    }

    @Nested
    @DisplayName("insertAll 批量插入")
    class InsertAll {

        @Test
        @DisplayName("should insert all entities when insertAll")
        void shouldInsertAllEntities_whenInsertAll() {
            List<TestUser> users = List.of(
                createUser("insert-1"),
                createUser("insert-2")
            );

            List<TestUser> inserted = dataManager.insertAll(TestUser.class, users);

            assertThat(inserted).hasSize(2);
            assertThat(inserted).allMatch(u -> u.getId() != null);
            assertThat(inserted).extracting(TestUser::getUsername)
                .containsExactly("insert-1", "insert-2");
        }
    }

    @Nested
    @DisplayName("deleteAllById 批量删除")
    class DeleteAllById {

        @Test
        @DisplayName("should delete all entities by ids when deleteAllById")
        void shouldDeleteAllEntitiesByIds_whenDeleteAllById() {
            TestUser user1 = dataManager.save(TestUser.class, createUser("del-batch-1"));
            TestUser user2 = dataManager.save(TestUser.class, createUser("del-batch-2"));
            TestUser user3 = dataManager.save(TestUser.class, createUser("del-batch-3"));

            dataManager.deleteAllById(TestUser.class, List.of(user1.getId(), user2.getId()));

            assertThat(dataManager.findById(TestUser.class, user1.getId())).isEmpty();
            assertThat(dataManager.findById(TestUser.class, user2.getId())).isEmpty();
            assertThat(dataManager.findById(TestUser.class, user3.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("findAllById 批量查询")
    class FindAllById {

        @Test
        @DisplayName("should find all entities by ids when findAllById")
        void shouldFindAllEntitiesByIds_whenFindAllById() {
            TestUser user1 = dataManager.save(TestUser.class, createUser("find-batch-1"));
            TestUser user2 = dataManager.save(TestUser.class, createUser("find-batch-2"));
            TestUser user3 = dataManager.save(TestUser.class, createUser("find-batch-3"));

            List<TestUser> found = dataManager.findAllById(TestUser.class,
                List.of(user1.getId(), user3.getId()));

            assertThat(found).hasSize(2);
            assertThat(found).extracting(TestUser::getUsername)
                .containsExactlyInAnyOrder("find-batch-1", "find-batch-3");
        }

        @Test
        @DisplayName("should return empty list when findAllById with non-existing ids")
        void shouldReturnEmptyList_whenFindAllByIdWithNonExistingIds() {
            List<TestUser> found = dataManager.findAllById(TestUser.class, List.of(99999L, 99998L));

            assertThat(found).isEmpty();
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
}
