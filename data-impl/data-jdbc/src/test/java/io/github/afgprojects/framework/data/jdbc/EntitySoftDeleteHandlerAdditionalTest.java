package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCache;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EntitySoftDeleteHandler 补充测试
 * <p>
 * 使用 H2 内存数据库进行集成测试，覆盖 restoreById、restoreAllById、hardDeleteById、hardDeleteAllById 和缓存失效逻辑
 */
@DisplayName("EntitySoftDeleteHandler Additional Tests")
class EntitySoftDeleteHandlerAdditionalTest {

    private DataSource dataSource;
    private JdbcClient jdbcClient;
    private Dialect dialect;
    private EntityCacheManager cacheManager;
    private EntityCache<TimestampSoftDeleteEntity> timestampCache;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        jdbcClient = JdbcClient.create(dataSource);
        dialect = new H2Dialect();
        createTestTables();
        cacheManager = createEntityCacheManager();
    }

    @AfterEach
    void tearDown() {
        dropTestTables();
    }

    @Nested
    @DisplayName("Restore By ID Tests")
    class RestoreByIdTests {

        @Test
        @DisplayName("restoreById should set deleted_at to NULL for timestamp entity")
        void testRestoreByIdTimestampEntity() {
            // Given
            insertTimestampEntity(1L, "test", null); // Not deleted
            insertTimestampEntity(2L, "deleted_entity", java.time.LocalDateTime.now()); // Deleted

            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.restoreById(2L);

            // Then
            var result = jdbcClient.sql("SELECT deleted_at FROM timestamp_soft_delete_entity WHERE id = ?")
                    .param(2L)
                    .query(java.time.LocalDateTime.class)
                    .optional();
            assertThat(result).isEmpty(); // deleted_at should be NULL
        }

        @Test
        @DisplayName("restoreById should set deleted to false for boolean entity")
        void testRestoreByIdBooleanEntity() {
            // Given
            insertBooleanEntity(1L, "test", false);
            insertBooleanEntity(2L, "deleted_entity", true);

            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.restoreById(2L);

            // Then
            var result = jdbcClient.sql("SELECT deleted FROM boolean_soft_delete_entity WHERE id = ?")
                    .param(2L)
                    .query(Boolean.class)
                    .single();
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("restoreById should evict cache when cacheManager is present")
        void testRestoreByIdEvictsCache() {
            // Given
            insertTimestampEntity(1L, "cached_entity", java.time.LocalDateTime.now());
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            timestampCache = cacheManager.getCache(TimestampSoftDeleteEntity.class);

            // Pre-populate cache
            var entity = new TimestampSoftDeleteEntity();
            entity.setId(1L);
            entity.setName("cached_entity");
            timestampCache.put("timestamp_soft_delete_entity:1", entity);
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:1")).isTrue();

            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, cacheManager
            );

            // When
            handler.restoreById(1L);

            // Then - cache should be evicted
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:1")).isFalse();
        }

        @Test
        @DisplayName("restoreById should throw UnsupportedOperationException for non-soft-deletable entity")
        void testRestoreByIdNonSoftDeletableThrows() {
            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThatThrownBy(() -> handler.restoreById(1L))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support soft delete");
        }
    }

    @Nested
    @DisplayName("Restore All By ID Tests")
    class RestoreAllByIdTests {

        @Test
        @DisplayName("restoreAllById should restore multiple records")
        void testRestoreAllById() {
            // Given
            insertTimestampEntity(1L, "entity1", java.time.LocalDateTime.now());
            insertTimestampEntity(2L, "entity2", java.time.LocalDateTime.now());
            insertTimestampEntity(3L, "entity3", java.time.LocalDateTime.now());

            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.restoreAllById(List.of(1L, 2L, 3L));

            // Then
            var count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_soft_delete_entity WHERE deleted_at IS NULL")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("restoreAllById with empty list should do nothing")
        void testRestoreAllByIdEmpty() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When - should not throw
            handler.restoreAllById(List.of());

            // Then - no error
        }
    }

    @Nested
    @DisplayName("Hard Delete By ID Tests")
    class HardDeleteByIdTests {

        @Test
        @DisplayName("hardDeleteById should physically delete the record")
        void testHardDeleteById() {
            // Given
            insertTimestampEntity(1L, "to_delete", null);
            insertTimestampEntity(2L, "keep", null);

            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.hardDeleteById(1L);

            // Then
            var count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_soft_delete_entity")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(1L);

            var remaining = jdbcClient.sql("SELECT name FROM timestamp_soft_delete_entity WHERE id = ?")
                    .param(2L)
                    .query(String.class)
                    .single();
            assertThat(remaining).isEqualTo("keep");
        }

        @Test
        @DisplayName("hardDeleteById should work for non-soft-deletable entity")
        void testHardDeleteByIdNonSoftDeletable() {
            // Given
            insertNonSoftDeleteEntity(1L, "to_delete");
            insertNonSoftDeleteEntity(2L, "keep");

            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.hardDeleteById(1L);

            // Then
            var count = jdbcClient.sql("SELECT COUNT(*) FROM non_soft_delete_entity")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("hardDeleteById should evict cache when cacheManager is present")
        void testHardDeleteByIdEvictsCache() {
            // Given
            insertTimestampEntity(1L, "cached_entity", null);
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            timestampCache = cacheManager.getCache(TimestampSoftDeleteEntity.class);

            // Pre-populate cache
            var entity = new TimestampSoftDeleteEntity();
            entity.setId(1L);
            entity.setName("cached_entity");
            timestampCache.put("timestamp_soft_delete_entity:1", entity);
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:1")).isTrue();

            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, cacheManager
            );

            // When
            handler.hardDeleteById(1L);

            // Then - cache should be evicted
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:1")).isFalse();
        }

        @Test
        @DisplayName("hardDeleteById should not fail when cache does not exist")
        void testHardDeleteByIdNoCache() {
            // Given
            insertTimestampEntity(1L, "test", null);
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);

            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, cacheManager
            );

            // When - should not throw even though cache doesn't have this entity
            handler.hardDeleteById(1L);

            // Then - no error
        }
    }

    @Nested
    @DisplayName("Hard Delete All By ID Tests")
    class HardDeleteAllByIdTests {

        @Test
        @DisplayName("hardDeleteAllById should delete multiple records")
        void testHardDeleteAllById() {
            // Given
            insertTimestampEntity(1L, "entity1", null);
            insertTimestampEntity(2L, "entity2", null);
            insertTimestampEntity(3L, "entity3", null);

            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When
            handler.hardDeleteAllById(List.of(1L, 2L));

            // Then
            var count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_soft_delete_entity")
                    .query(Long.class)
                    .single();
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("hardDeleteAllById with empty list should do nothing")
        void testHardDeleteAllByIdEmpty() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            // When - should not throw
            handler.hardDeleteAllById(List.of());

            // Then - no error
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("restoreById should build correct cache key for boolean entity")
        void testCacheKeyBuildBoolean() {
            // Given
            insertBooleanEntity(42L, "test", true);
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var booleanCache = cacheManager.getCache(BooleanSoftDeleteEntity.class);

            // Pre-populate cache
            var entity = new BooleanSoftDeleteEntity();
            entity.setId(42L);
            entity.setName("test");
            booleanCache.put("boolean_soft_delete_entity:42", entity);
            assertThat(booleanCache.containsKey("boolean_soft_delete_entity:42")).isTrue();

            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, cacheManager
            );

            // When
            handler.restoreById(42L);

            // Then
            assertThat(booleanCache.containsKey("boolean_soft_delete_entity:42")).isFalse();
        }

        @Test
        @DisplayName("hardDeleteById should build correct cache key")
        void testHardDeleteCacheKeyBuild() {
            // Given
            insertTimestampEntity(99L, "test", null);
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            timestampCache = cacheManager.getCache(TimestampSoftDeleteEntity.class);

            // Pre-populate cache
            var entity = new TimestampSoftDeleteEntity();
            entity.setId(99L);
            entity.setName("test");
            timestampCache.put("timestamp_soft_delete_entity:99", entity);
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:99")).isTrue();

            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, cacheManager
            );

            // When
            handler.hardDeleteById(99L);

            // Then
            assertThat(timestampCache.containsKey("timestamp_soft_delete_entity:99")).isFalse();
        }
    }

    @Nested
    @DisplayName("Append Soft Delete Filter Additional Tests")
    class AppendSoftDeleteFilterAdditionalTests {

        @Test
        @DisplayName("appendSoftDeleteFilter should not modify SQL for non-soft-deletable with includeDeleted=false")
        void testAppendSoftDeleteFilterNonSoftDeletable() {
            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, false);

            // Non-soft-deletable entity should not add filter
            assertThat(sql.toString()).isEqualTo("SELECT * FROM table");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter should append correct clause for timestamp entity with WHERE")
        void testAppendSoftDeleteFilterTimestampWithWhereClause() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE status = 1");
            handler.appendSoftDeleteFilter(sql, true, false);

            assertThat(sql.toString()).isEqualTo("SELECT * FROM table WHERE status = 1 AND deleted_at IS NULL");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter should append correct clause for timestamp entity without WHERE")
        void testAppendSoftDeleteFilterTimestampWithoutWhereClause() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, false);

            assertThat(sql.toString()).isEqualTo("SELECT * FROM table WHERE deleted_at IS NULL");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter should append correct clause for boolean entity with WHERE")
        void testAppendSoftDeleteFilterBooleanWithWhereClause() {
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE status = 1");
            handler.appendSoftDeleteFilter(sql, true, false);

            assertThat(sql.toString()).isEqualTo("SELECT * FROM table WHERE status = 1 AND deleted = false");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter should append correct clause for boolean entity without WHERE")
        void testAppendSoftDeleteFilterBooleanWithoutWhereClause() {
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, false);

            assertThat(sql.toString()).isEqualTo("SELECT * FROM table WHERE deleted = false");
        }
    }

    @Nested
    @DisplayName("Build Soft Delete Set Clause Tests")
    class BuildSoftDeleteSetClauseTests {

        @Test
        @DisplayName("buildSoftDeleteSetClause should return correct clause for timestamp entity")
        void testBuildSoftDeleteSetClauseTimestamp() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.buildSoftDeleteSetClause()).isEqualTo("deleted_at = NOW()");
        }

        @Test
        @DisplayName("buildSoftDeleteSetClause should return correct clause for boolean entity")
        void testBuildSoftDeleteSetClauseBoolean() {
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.buildSoftDeleteSetClause()).isEqualTo("deleted = true");
        }

        @Test
        @DisplayName("buildSoftDeleteSetClause should throw for non-soft-deletable entity")
        void testBuildSoftDeleteSetClauseNonSoftDeletable() {
            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThatThrownBy(handler::buildSoftDeleteSetClause)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("NonSoftDeleteEntity")
                    .hasMessageContaining("does not support soft delete");
        }
    }

    @Nested
    @DisplayName("Strategy Detection Tests")
    class StrategyDetectionTests {

        @Test
        @DisplayName("getSoftDeleteStrategy should return TIMESTAMP for TimestampSoftDeletable")
        void testGetStrategyTimestamp() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isEqualTo(SoftDeleteStrategy.TIMESTAMP);
        }

        @Test
        @DisplayName("getSoftDeleteStrategy should return BOOLEAN for SoftDeletable")
        void testGetStrategyBoolean() {
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isEqualTo(SoftDeleteStrategy.BOOLEAN);
        }

        @Test
        @DisplayName("getSoftDeleteStrategy should return null for non-soft-deletable")
        void testGetStrategyNull() {
            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isNull();
        }
    }

    @Nested
    @DisplayName("Is Soft Deletable Tests")
    class IsSoftDeletableTests {

        @Test
        @DisplayName("isSoftDeletable should return true for timestamp entity")
        void testIsSoftDeletableTimestamp() {
            var metadata = new SimpleEntityMetadata<>(TimestampSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("isSoftDeletable should return true for boolean entity")
        void testIsSoftDeletableBoolean() {
            var metadata = new SimpleEntityMetadata<>(BooleanSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("isSoftDeletable should return false for non-soft-deletable entity")
        void testIsSoftDeletableFalse() {
            var metadata = new SimpleEntityMetadata<>(NonSoftDeleteEntity.class);
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, metadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isFalse();
        }
    }

    // ==================== Helper Methods ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:soft_delete_testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private EntityCacheManager createEntityCacheManager() {
        CacheProperties cacheProperties = new CacheProperties();
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
        EntityCacheProperties properties = new EntityCacheProperties();
        properties.setEnabled(true);
        return new EntityCacheManager(defaultCacheManager, properties);
    }

    private void createTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE timestamp_soft_delete_entity (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100),
                    deleted_at TIMESTAMP
                )
                """);
            stmt.execute("""
                CREATE TABLE boolean_soft_delete_entity (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100),
                    deleted BOOLEAN
                )
                """);
            stmt.execute("""
                CREATE TABLE non_soft_delete_entity (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test tables", e);
        }
    }

    private void dropTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS timestamp_soft_delete_entity");
            stmt.execute("DROP TABLE IF EXISTS boolean_soft_delete_entity");
            stmt.execute("DROP TABLE IF EXISTS non_soft_delete_entity");
        } catch (Exception ignored) {
        }
    }

    private void insertTimestampEntity(Long id, String name, java.time.LocalDateTime deletedAt) {
        jdbcClient.sql("INSERT INTO timestamp_soft_delete_entity (id, name, deleted_at) VALUES (?, ?, ?)")
                .params(id, name, deletedAt)
                .update();
    }

    private void insertBooleanEntity(Long id, String name, boolean deleted) {
        jdbcClient.sql("INSERT INTO boolean_soft_delete_entity (id, name, deleted) VALUES (?, ?, ?)")
                .params(id, name, deleted)
                .update();
    }

    private void insertNonSoftDeleteEntity(Long id, String name) {
        jdbcClient.sql("INSERT INTO non_soft_delete_entity (id, name) VALUES (?, ?)")
                .params(id, name)
                .update();
    }

    // Test entity classes

    static class TimestampSoftDeleteEntity implements TimestampSoftDeletable {
        private Long id;
        private String name;
        private java.time.LocalDateTime deletedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean isDeleted() { return deletedAt != null; }

        @Override
        public java.time.LocalDateTime getDeletedAt() { return deletedAt; }

        @Override
        public void setDeletedAt(java.time.LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    }

    static class BooleanSoftDeleteEntity implements SoftDeletable {
        private Long id;
        private String name;
        private boolean deleted;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean isDeleted() { return deleted; }

        @Override
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }

    static class NonSoftDeleteEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
