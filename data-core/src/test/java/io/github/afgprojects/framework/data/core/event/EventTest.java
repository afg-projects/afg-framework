package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Event 包测试
 */
@DisplayName("Event 包测试")
class EventTest {

    // 创建简单的 mock EntityMetadata
    private static class MockEntityMetadata implements EntityMetadata<TestEntity> {
        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public String getTableName() {
            return "test_entity";
        }

        @Override
        public FieldMetadata getIdField() {
            return null;
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of();
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return null;
        }

        @Override
        public boolean isSoftDeletable() {
            return false;
        }

        @Override
        public boolean isTenantAware() {
            return false;
        }

        @Override
        public boolean isAuditable() {
            return false;
        }

        @Override
        public boolean isVersioned() {
            return false;
        }

        @Override
        public List<RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }

    // 测试实体类
    private static class TestEntity {
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    // ==================== EntityCreatedEvent 测试 ====================

    @Nested
    @DisplayName("EntityCreatedEvent 测试")
    class EntityCreatedEventTest {

        @Test
        @DisplayName("应正确创建实体创建事件")
        void shouldCreateEntityCreatedEvent() {
            TestEntity entity = new TestEntity(1L, "test");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();

            EntityCreatedEvent<TestEntity> event = new EntityCreatedEvent<>(entity, metadata, timestamp);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
            assertThat(event.getSource()).isEqualTo(entity);
        }
    }

    // ==================== EntityUpdatedEvent 测试 ====================

    @Nested
    @DisplayName("EntityUpdatedEvent 测试")
    class EntityUpdatedEventTest {

        @Test
        @DisplayName("应正确创建实体更新事件")
        void shouldCreateEntityUpdatedEvent() {
            TestEntity entity = new TestEntity(1L, "updated");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();

            EntityUpdatedEvent<TestEntity> event = new EntityUpdatedEvent<>(entity, metadata, timestamp);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
        }
    }

    // ==================== EntityDeletedEvent 测试 ====================

    @Nested
    @DisplayName("EntityDeletedEvent 测试")
    class EntityDeletedEventTest {

        @Test
        @DisplayName("应正确创建实体删除事件")
        void shouldCreateEntityDeletedEvent() {
            TestEntity entity = new TestEntity(1L, "deleted");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();

            EntityDeletedEvent<TestEntity> event = new EntityDeletedEvent<>(entity, metadata, timestamp);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
        }
    }

    // ==================== EntityBatchCreatedEvent 测试 ====================

    @Nested
    @DisplayName("EntityBatchCreatedEvent 测试")
    class EntityBatchCreatedEventTest {

        @Test
        @DisplayName("应正确创建批量创建事件")
        void shouldCreateEntityBatchCreatedEvent() {
            TestEntity entity = new TestEntity(1L, "first");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();
            List<TestEntity> entities = List.of(
                    new TestEntity(1L, "first"),
                    new TestEntity(2L, "second"),
                    new TestEntity(3L, "third")
            );

            EntityBatchCreatedEvent<TestEntity> event = new EntityBatchCreatedEvent<>(entity, metadata, timestamp, entities);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
            assertThat(event.getEntities()).isEqualTo(entities);
            assertThat(event.getEntities()).hasSize(3);
        }
    }

    // ==================== EntityBatchUpdatedEvent 测试 ====================

    @Nested
    @DisplayName("EntityBatchUpdatedEvent 测试")
    class EntityBatchUpdatedEventTest {

        @Test
        @DisplayName("应正确创建批量更新事件")
        void shouldCreateEntityBatchUpdatedEvent() {
            TestEntity entity = new TestEntity(1L, "first");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();
            List<TestEntity> entities = List.of(
                    new TestEntity(1L, "updated1"),
                    new TestEntity(2L, "updated2")
            );

            EntityBatchUpdatedEvent<TestEntity> event = new EntityBatchUpdatedEvent<>(entity, metadata, timestamp, entities);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
            assertThat(event.getEntities()).isEqualTo(entities);
            assertThat(event.getEntities()).hasSize(2);
        }
    }

    // ==================== EntityBatchDeletedEvent 测试 ====================

    @Nested
    @DisplayName("EntityBatchDeletedEvent 测试")
    class EntityBatchDeletedEventTest {

        @Test
        @DisplayName("应正确创建批量删除事件")
        void shouldCreateEntityBatchDeletedEvent() {
            TestEntity entity = new TestEntity(1L, "first");
            MockEntityMetadata metadata = new MockEntityMetadata();
            Instant timestamp = Instant.now();
            List<TestEntity> entities = List.of(
                    new TestEntity(1L, "deleted1"),
                    new TestEntity(2L, "deleted2")
            );

            EntityBatchDeletedEvent<TestEntity> event = new EntityBatchDeletedEvent<>(entity, metadata, timestamp, entities);

            assertThat(event.getEntity()).isEqualTo(entity);
            assertThat(event.getMetadata()).isEqualTo(metadata);
            assertThat(event.getOccurredAt()).isEqualTo(timestamp);
            assertThat(event.getEntities()).isEqualTo(entities);
            assertThat(event.getEntities()).hasSize(2);
        }
    }

    // ==================== EntityEvent 基类测试 ====================

    @Nested
    @DisplayName("EntityEvent 基类测试")
    class EntityEventBaseTest {

        @Test
        @DisplayName("所有事件类应继承 EntityEvent")
        void allEventsShouldExtendEntityEvent() {
            assertThat(EntityCreatedEvent.class).isAssignableTo(EntityEvent.class);
            assertThat(EntityUpdatedEvent.class).isAssignableTo(EntityEvent.class);
            assertThat(EntityDeletedEvent.class).isAssignableTo(EntityEvent.class);
            assertThat(EntityBatchCreatedEvent.class).isAssignableTo(EntityEvent.class);
            assertThat(EntityBatchUpdatedEvent.class).isAssignableTo(EntityEvent.class);
            assertThat(EntityBatchDeletedEvent.class).isAssignableTo(EntityEvent.class);
        }

        @Test
        @DisplayName("所有事件类应继承 ApplicationEvent")
        void allEventsShouldExtendApplicationEvent() {
            // EntityEvent 继承 ApplicationEvent
            assertThat(EntityEvent.class).isAssignableTo(org.springframework.context.ApplicationEvent.class);
        }

        @Test
        @DisplayName("事件应使用 sealed 类限制子类")
        void entityEventShouldBeSealed() {
            assertThat(EntityEvent.class).isSealed();
        }
    }
}