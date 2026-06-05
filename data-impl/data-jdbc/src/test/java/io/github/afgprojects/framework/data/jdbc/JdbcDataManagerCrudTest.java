package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;
import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.entity.TestVersionedItem;

/**
 * JdbcDataManager CRUD 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerCrudTest extends BaseDataTest {

    @Nested
    @DisplayName("CRUD 生命周期")
    class CrudLifecycle {

        @Test
        @DisplayName("should persist entity when save new entity")
        void shouldPersistEntity_whenSaveNewEntity() {
            TestUser user = new TestUser();
            user.setUsername("crud-test");
            user.setEmail("crud@test.com");

            TestUser saved = dataManager.save(TestUser.class, user);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUsername()).isEqualTo("crud-test");
            assertThat(saved.getEmail()).isEqualTo("crud@test.com");
            assertThat(saved.getStatus()).isEqualTo(1);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update fields when save existing entity")
        void shouldUpdateFields_whenSaveExistingEntity() {
            TestUser user = new TestUser();
            user.setUsername("original");
            user.setEmail("original@test.com");
            TestUser saved = dataManager.save(TestUser.class, user);

            saved.setUsername("updated");
            saved.setEmail("updated@test.com");
            TestUser updated = dataManager.save(TestUser.class, saved);

            assertThat(updated.getId()).isEqualTo(saved.getId());
            assertThat(updated.getUsername()).isEqualTo("updated");
            assertThat(updated.getEmail()).isEqualTo("updated@test.com");
        }

        @Test
        @DisplayName("should find entity when findById with existing id")
        void shouldFindEntity_whenFindByIdWithExistingId() {
            TestUser user = new TestUser();
            user.setUsername("find-test");
            TestUser saved = dataManager.save(TestUser.class, user);

            TestUser found = dataManager.findById(TestUser.class, saved.getId()).orElse(null);

            assertThat(found).isNotNull();
            assertThat(found.getUsername()).isEqualTo("find-test");
        }

        @Test
        @DisplayName("should return empty when findById with non-existing id")
        void shouldReturnEmpty_whenFindByIdWithNonExistingId() {
            assertThat(dataManager.findById(TestUser.class, 99999L)).isEmpty();
        }

        @Test
        @DisplayName("should return all entities when findAll")
        void shouldReturnAllEntities_whenFindAll() {
            dataManager.save(TestUser.class, createUser("user1"));
            dataManager.save(TestUser.class, createUser("user2"));
            dataManager.save(TestUser.class, createUser("user3"));

            List<TestUser> users = dataManager.findAll(TestUser.class);

            assertThat(users).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("should remove entity when deleteById")
        void shouldRemoveEntity_whenDeleteById() {
            TestUser saved = dataManager.save(TestUser.class, createUser("delete-test"));

            dataManager.deleteById(TestUser.class, saved.getId());

            assertThat(dataManager.findById(TestUser.class, saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("条件查询")
    class ConditionQuery {

        @Test
        @DisplayName("should filter by eq condition when query with condition")
        void shouldFilterByEqCondition_whenQueryWithCondition() {
            dataManager.save(TestUser.class, createUser("active", 1));
            dataManager.save(TestUser.class, createUser("inactive", 0));

            List<TestUser> activeUsers = dataManager.entity(TestUser.class)
                .query()
                .where(Conditions.eq("status", 1))
                .list();

            assertThat(activeUsers).allMatch(u -> u.getStatus() == 1);
        }

        @Test
        @DisplayName("should filter by multiple conditions when query with AND")
        void shouldFilterByMultipleConditions_whenQueryWithAnd() {
            dataManager.save(TestUser.class, createUser("zhang", 1));
            dataManager.save(TestUser.class, createUser("wang", 0));
            dataManager.save(TestUser.class, createUser("zhao", 1));

            List<TestUser> result = dataManager.entity(TestUser.class)
                .query()
                .where(Conditions.builder(TestUser.class)
                    .likeStartsWith(TestUser::getUsername, "zh")
                    .eq(TestUser::getStatus, 1)
                    .build())
                .list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("zhang");
        }

        @Test
        @DisplayName("should paginate results when query with page request")
        void shouldPaginateResults_whenQueryWithPageRequest() {
            for (int i = 0; i < 15; i++) {
                dataManager.save(TestUser.class, createUser("page-user-" + i));
            }

            Page<TestUser> page = dataManager.entity(TestUser.class)
                .query()
                .page(PageRequest.of(1, 10));

            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotal()).isGreaterThanOrEqualTo(15);
            assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(2);
            assertThat(page.isFirst()).isTrue();
        }
    }

    @Nested
    @DisplayName("软删除")
    class SoftDelete {

        @Test
        @DisplayName("should set deleted flag when delete soft delete entity")
        void shouldSetDeletedFlag_whenDeleteSoftDeleteEntity() {
            TestSoftDeleteItem item = new TestSoftDeleteItem();
            item.setName("soft-delete-test");
            item.setQuantity(10);
            TestSoftDeleteItem saved = dataManager.save(TestSoftDeleteItem.class, item);

            dataManager.deleteById(TestSoftDeleteItem.class, saved.getId());

            // 软删除后 findAll 不应包含已删除记录
            List<TestSoftDeleteItem> activeItems = dataManager.findAll(TestSoftDeleteItem.class);
            assertThat(activeItems).noneMatch(i -> i.getId().equals(saved.getId()));
        }
    }

    @Nested
    @DisplayName("乐观锁")
    class OptimisticLock {

        @Test
        @DisplayName("should throw exception when version conflict on update")
        void shouldThrowException_whenVersionConflictOnUpdate() {
            TestVersionedItem item = new TestVersionedItem();
            item.setName("lock-test");
            item.setStock(100);
            TestVersionedItem saved = dataManager.save(TestVersionedItem.class, item);
            assertThat(saved.getVersion()).isEqualTo(0);

            // 正常更新版本号递增
            saved.setStock(80);
            TestVersionedItem updated = dataManager.save(TestVersionedItem.class, saved);
            assertThat(updated.getVersion()).isEqualTo(1);
        }
    }

    // --- 辅助方法 ---

    private TestUser createUser(String username) {
        return createUser(username, 1);
    }

    private TestUser createUser(String username, int status) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setStatus(status);
        return user;
    }
}