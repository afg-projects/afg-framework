package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import io.github.afgprojects.framework.data.core.event.EntityChangedEvent;
import io.github.afgprojects.framework.data.core.event.EntityChangedEvent.ChangeType;
import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.event.SpringEntityChangedEventPublisher;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager EntityChangedEvent 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 * <p>
 * 通过 @TestConfiguration 注册 TestEventListener Bean 和
 * SpringEntityChangedEventPublisher Bean，确保 DataManager 发布的
 * EntityChangedEvent 能被 @EventListener 捕获。
 * </p>
 */
@org.springframework.context.annotation.Import(EntityChangedEventPublisherTest.TestEventConfig.class)
class EntityChangedEventPublisherTest extends BaseDataTest {

    @Autowired
    private TestEventListener testEventListener;

    @BeforeEach
    void setUp() {
        testEventListener.clear();
    }

    @Nested
    @DisplayName("CREATED 事件")
    class CreatedEvent {

        @Test
        @DisplayName("should publish CREATED event when save new entity")
        void shouldPublishCreatedEvent_whenSaveNewEntity() {
            TestUser user = createUser("event-create");
            dataManager.save(TestUser.class, user);

            List<EntityChangedEvent<?>> events = testEventListener.getEvents(ChangeType.CREATED);
            assertThat(events).hasSize(1);

            EntityChangedEvent<?> event = events.get(0);
            assertThat(event.getChangeType()).isEqualTo(ChangeType.CREATED);
            assertThat(event.getEntityType()).isEqualTo(TestUser.class);
            assertThat(event.getEntity()).isNotNull();
            assertThat(event.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("UPDATED 事件")
    class UpdatedEvent {

        @Test
        @DisplayName("should publish UPDATED event when save existing entity")
        void shouldPublishUpdatedEvent_whenSaveExistingEntity() {
            TestUser saved = dataManager.save(TestUser.class, createUser("event-update"));
            testEventListener.clear();

            saved.setEmail("updated@test.com");
            dataManager.save(TestUser.class, saved);

            List<EntityChangedEvent<?>> events = testEventListener.getEvents(ChangeType.UPDATED);
            assertThat(events).hasSize(1);

            EntityChangedEvent<?> event = events.get(0);
            assertThat(event.getChangeType()).isEqualTo(ChangeType.UPDATED);
            assertThat(event.getEntityType()).isEqualTo(TestUser.class);
        }
    }

    @Nested
    @DisplayName("DELETED 事件")
    class DeletedEvent {

        @Test
        @DisplayName("should publish DELETED event when deleteById")
        void shouldPublishDeletedEvent_whenDeleteById() {
            TestUser saved = dataManager.save(TestUser.class, createUser("event-delete"));
            testEventListener.clear();

            dataManager.deleteById(TestUser.class, saved.getId());

            List<EntityChangedEvent<?>> events = testEventListener.getEvents(ChangeType.DELETED);
            assertThat(events).hasSize(1);

            EntityChangedEvent<?> event = events.get(0);
            assertThat(event.getChangeType()).isEqualTo(ChangeType.DELETED);
            assertThat(event.getEntityType()).isEqualTo(TestUser.class);
        }

        @Test
        @DisplayName("should publish DELETED event when soft delete entity")
        void shouldPublishDeletedEvent_whenSoftDeleteEntity() {
            TestSoftDeleteItem item = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("event-soft-delete", 10));
            testEventListener.clear();

            dataManager.deleteById(TestSoftDeleteItem.class, item.getId());

            List<EntityChangedEvent<?>> events = testEventListener.getEvents(ChangeType.DELETED);
            assertThat(events).hasSize(1);

            EntityChangedEvent<?> event = events.get(0);
            assertThat(event.getChangeType()).isEqualTo(ChangeType.DELETED);
            assertThat(event.getEntityType()).isEqualTo(TestSoftDeleteItem.class);
        }
    }

    @Nested
    @DisplayName("RESTORED 事件")
    class RestoredEvent {

        @Test
        @DisplayName("should publish RESTORED event when restoreById")
        void shouldPublishRestoredEvent_whenRestoreById() {
            TestSoftDeleteItem item = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("event-restore", 20));
            dataManager.deleteById(TestSoftDeleteItem.class, item.getId());
            testEventListener.clear();

            dataManager.restoreById(TestSoftDeleteItem.class, item.getId());

            List<EntityChangedEvent<?>> events = testEventListener.getEvents(ChangeType.RESTORED);
            assertThat(events).hasSize(1);

            EntityChangedEvent<?> event = events.get(0);
            assertThat(event.getChangeType()).isEqualTo(ChangeType.RESTORED);
            assertThat(event.getEntityType()).isEqualTo(TestSoftDeleteItem.class);
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
     * 测试事件配置
     * <p>
     * 注册 SpringEntityChangedEventPublisher 和 TestEventListener Bean。
     * SpringEntityChangedEventPublisher 替代默认的 NoOp 实现，
     * 使 DataManager 通过 Spring ApplicationEventPublisher 发布事件。
     * TestEventListener 捕获事件用于测试断言。
     */
    @TestConfiguration
    static class TestEventConfig {

        @Bean
        SpringEntityChangedEventPublisher springEntityChangedEventPublisher(
                ApplicationEventPublisher applicationEventPublisher) {
            return new SpringEntityChangedEventPublisher(applicationEventPublisher);
        }

        @Bean
        TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    /**
     * 测试事件监听器，捕获 EntityChangedEvent 事件
     */
    static class TestEventListener {

        private final List<EntityChangedEvent<?>> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void onEntityChanged(EntityChangedEvent<?> event) {
            events.add(event);
        }

        List<EntityChangedEvent<?>> getEvents(ChangeType type) {
            return events.stream()
                .filter(e -> e.getChangeType() == type)
                .toList();
        }

        void clear() {
            events.clear();
        }
    }
}
