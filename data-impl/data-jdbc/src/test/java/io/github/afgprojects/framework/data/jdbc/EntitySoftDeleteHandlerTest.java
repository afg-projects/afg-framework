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
import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * EntitySoftDeleteHandler 测试
 */
@DisplayName("EntitySoftDeleteHandler Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntitySoftDeleteHandlerTest {

    @Mock
    private SimpleEntityMetadata<TimestampSoftDeleteEntity> timestampMetadata;

    @Mock
    private SimpleEntityMetadata<BooleanSoftDeleteEntity> booleanMetadata;

    @Mock
    private SimpleEntityMetadata<NonSoftDeleteEntity> nonSoftDeleteMetadata;

    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private EntityCacheManager cacheManager;

    @Mock
    private EntityCache<TimestampSoftDeleteEntity> cache;

    private Dialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new H2Dialect();

        when(timestampMetadata.getTableName()).thenReturn("timestamp_entity");
        when(timestampMetadata.getEntityClass()).thenReturn(TimestampSoftDeleteEntity.class);

        when(booleanMetadata.getTableName()).thenReturn("boolean_entity");
        when(booleanMetadata.getEntityClass()).thenReturn(BooleanSoftDeleteEntity.class);

        when(nonSoftDeleteMetadata.getTableName()).thenReturn("non_soft_delete_entity");
        when(nonSoftDeleteMetadata.getEntityClass()).thenReturn(NonSoftDeleteEntity.class);
    }

    @Nested
    @DisplayName("Timestamp Soft Delete Tests")
    class TimestampSoftDeleteTests {

        @Test
        @DisplayName("detectSoftDeleteStrategy should detect TIMESTAMP strategy")
        void testDetectTimestampStrategy() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isEqualTo(SoftDeleteStrategy.TIMESTAMP);
        }

        @Test
        @DisplayName("isSoftDeletable should return true for TimestampSoftDeletable")
        void testIsSoftDeletableTimestamp() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("buildSoftDeleteSetClause should return deleted_at = NOW()")
        void testBuildSoftDeleteSetClauseTimestamp() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            assertThat(handler.buildSoftDeleteSetClause()).isEqualTo("deleted_at = NOW()");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter with WHERE clause")
        void testAppendSoftDeleteFilterWithWhereTimestamp() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE status = 1");
            handler.appendSoftDeleteFilter(sql, true, false);

            assertThat(sql.toString()).contains("AND deleted_at IS NULL");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter without WHERE clause")
        void testAppendSoftDeleteFilterWithoutWhereTimestamp() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, false);

            assertThat(sql.toString()).contains("WHERE deleted_at IS NULL");
        }
    }

    @Nested
    @DisplayName("Boolean Soft Delete Tests")
    class BooleanSoftDeleteTests {

        @Test
        @DisplayName("detectSoftDeleteStrategy should detect BOOLEAN strategy")
        void testDetectBooleanStrategy() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isEqualTo(SoftDeleteStrategy.BOOLEAN);
        }

        @Test
        @DisplayName("isSoftDeletable should return true for SoftDeletable")
        void testIsSoftDeletableBoolean() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("buildSoftDeleteSetClause should return deleted = true")
        void testBuildSoftDeleteSetClauseBoolean() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            assertThat(handler.buildSoftDeleteSetClause()).isEqualTo("deleted = true");
        }
    }

    @Nested
    @DisplayName("Non-Soft-Deletable Entity Tests")
    class NonSoftDeletableTests {

        @Test
        @DisplayName("detectSoftDeleteStrategy should return null")
        void testDetectNoStrategy() {
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
            );

            assertThat(handler.getSoftDeleteStrategy()).isNull();
        }

        @Test
        @DisplayName("isSoftDeletable should return false")
        void testIsNotSoftDeletable() {
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
            );

            assertThat(handler.isSoftDeletable()).isFalse();
        }

        @Test
        @DisplayName("restoreById should throw UnsupportedOperationException")
        void testRestoreByIdThrowsException() {
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
            );

            assertThatThrownBy(() -> handler.restoreById(1L))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support soft delete");
        }

        @Test
        @DisplayName("buildSoftDeleteSetClause should throw UnsupportedOperationException")
        void testBuildSoftDeleteSetClauseThrowsException() {
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
            );

            assertThatThrownBy(handler::buildSoftDeleteSetClause)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support soft delete");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter with includeDeleted=true should do nothing")
        void testAppendSoftDeleteFilterIncludeDeleted() {
            var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, true);

            assertThat(sql.toString()).isEqualTo("SELECT * FROM table");
        }
    }

    @Nested
    @DisplayName("Append Soft Delete Filter Edge Cases Tests")
    class AppendSoftDeleteFilterEdgeCasesTests {

        @Test
        @DisplayName("appendSoftDeleteFilter with includeDeleted=true for timestamp entity")
        void testAppendSoftDeleteFilterIncludeDeletedTimestamp() {
            var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, true);

            // includeDeleted=true should not add filter
            assertThat(sql.toString()).isEqualTo("SELECT * FROM table");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter with includeDeleted=true for boolean entity")
        void testAppendSoftDeleteFilterIncludeDeletedBoolean() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, true);

            // includeDeleted=true should not add filter
            assertThat(sql.toString()).isEqualTo("SELECT * FROM table");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter for boolean entity with WHERE")
        void testAppendSoftDeleteFilterBooleanWithWhere() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE status = 1");
            handler.appendSoftDeleteFilter(sql, true, false);

            assertThat(sql.toString()).contains("AND deleted = false");
        }

        @Test
        @DisplayName("appendSoftDeleteFilter for boolean entity without WHERE")
        void testAppendSoftDeleteFilterBooleanWithoutWhere() {
            var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
            );

            StringBuilder sql = new StringBuilder("SELECT * FROM table");
            handler.appendSoftDeleteFilter(sql, false, false);

            assertThat(sql.toString()).contains("WHERE deleted = false");
        }
    }

    // 测试实体类

    // 测试实体类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimestampSoftDeleteEntity implements TimestampSoftDeletable {
        private Long id;
        private String name;
        private java.time.LocalDateTime deletedAt;

        @Override
        public boolean isDeleted() {
            return deletedAt != null;
        }

        @Override
        public java.time.LocalDateTime getDeletedAt() {
            return deletedAt;
        }

        @Override
        public void setDeletedAt(java.time.LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class BooleanSoftDeleteEntity implements SoftDeletable {
        private Long id;
        private String name;
        private boolean deleted;

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NonSoftDeleteEntity {
        private Long id;
        private String name;
    }

    // ==================== 集成测试 ====================

    /**
     * 集成测试 - 使用真实 H2 数据库
     */
    @Nested
    @DisplayName("Integration Tests with H2 Database")
    class IntegrationTests {

        private DataSource dataSource;
        private JdbcClient jdbcClient;
        private Dialect dialect;
        private SimpleEntityMetadata<TimestampSoftDeleteEntity> timestampMetadata;
        private SimpleEntityMetadata<BooleanSoftDeleteEntity> booleanMetadata;
        private SimpleEntityMetadata<NonSoftDeleteEntity> nonSoftDeleteMetadata;

        @BeforeEach
        void setUp() {
            dataSource = createDataSource();
            jdbcClient = JdbcClient.create(dataSource);
            dialect = new H2Dialect();

            // Create metadata mocks with proper stubbing
            timestampMetadata = mock(SimpleEntityMetadata.class);
            when(timestampMetadata.getTableName()).thenReturn("timestamp_entity");
            when(timestampMetadata.getEntityClass()).thenReturn(TimestampSoftDeleteEntity.class);

            booleanMetadata = mock(SimpleEntityMetadata.class);
            when(booleanMetadata.getTableName()).thenReturn("boolean_entity");
            when(booleanMetadata.getEntityClass()).thenReturn(BooleanSoftDeleteEntity.class);

            nonSoftDeleteMetadata = mock(SimpleEntityMetadata.class);
            when(nonSoftDeleteMetadata.getTableName()).thenReturn("non_soft_delete_entity");
            when(nonSoftDeleteMetadata.getEntityClass()).thenReturn(NonSoftDeleteEntity.class);

            createTestTables();
        }

        @AfterEach
        void tearDown() {
            dropTestTables();
        }

        // ==================== restoreById 测试 ====================

        @Nested
        @DisplayName("restoreById Tests")
        class RestoreByIdTests {

            @Test
            @DisplayName("should restore soft-deleted record with timestamp strategy")
            void shouldRestoreTimestampSoftDeletedRecord() {
                // Insert a soft-deleted record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test', NOW())")
                    .update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                handler.restoreById(1L);

                // Verify record is restored
                Optional<LocalDateTime> deletedAt = jdbcClient.sql("SELECT deleted_at FROM timestamp_entity WHERE id = 1")
                    .query(LocalDateTime.class)
                    .optional();
                assertThat(deletedAt).isEmpty();
            }

            @Test
            @DisplayName("should restore soft-deleted record with boolean strategy")
            void shouldRestoreBooleanSoftDeletedRecord() {
                // Insert a soft-deleted record
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (1, 'test', true)")
                    .update();

                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                handler.restoreById(1L);

                // Verify record is restored
                Boolean deleted = jdbcClient.sql("SELECT deleted FROM boolean_entity WHERE id = 1")
                    .query(Boolean.class)
                    .single();
                assertThat(deleted).isFalse();
            }

            @Test
            @DisplayName("should throw UnsupportedOperationException for non-soft-deletable entity")
            void shouldThrowForNonSoftDeletableEntity() {
                var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
                );

                assertThatThrownBy(() -> handler.restoreById(1L))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support soft delete");
            }

            @Test
            @DisplayName("should handle restore of non-existent record gracefully")
            void shouldHandleNonExistentRecord() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Should not throw - just updates 0 rows
                handler.restoreById(99999L);
            }
        }

        // ==================== restoreAllById 测试 ====================

        @Nested
        @DisplayName("restoreAllById Tests")
        class RestoreAllByIdTests {

            @Test
            @DisplayName("should restore multiple soft-deleted records with timestamp strategy")
            void shouldRestoreMultipleTimestampSoftDeletedRecords() {
                // Insert soft-deleted records
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test1', NOW())").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (2, 'test2', NOW())").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (3, 'test3', NOW())").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                handler.restoreAllById(List.of(1L, 2L, 3L));

                // Verify all records are restored
                long deletedCount = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_entity WHERE deleted_at IS NOT NULL")
                    .query(Long.class)
                    .single();
                assertThat(deletedCount).isZero();
            }

            @Test
            @DisplayName("should restore multiple soft-deleted records with boolean strategy")
            void shouldRestoreMultipleBooleanSoftDeletedRecords() {
                // Insert soft-deleted records
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (1, 'test1', true)").update();
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (2, 'test2', true)").update();

                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                handler.restoreAllById(List.of(1L, 2L));

                // Verify all records are restored
                long deletedCount = jdbcClient.sql("SELECT COUNT(*) FROM boolean_entity WHERE deleted = true")
                    .query(Long.class)
                    .single();
                assertThat(deletedCount).isZero();
            }

            @Test
            @DisplayName("should handle empty iterable gracefully")
            void shouldHandleEmptyIterable() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Should not throw
                handler.restoreAllById(List.of());
            }

            @Test
            @DisplayName("should restore with mixed existing and non-existing IDs")
            void shouldRestoreMixedIds() {
                // Insert only two records
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test1', NOW())").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (2, 'test2', NOW())").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Restore with one non-existing ID
                handler.restoreAllById(List.of(1L, 2L, 99999L));

                // Verify existing records are restored
                long deletedCount = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_entity WHERE deleted_at IS NOT NULL")
                    .query(Long.class)
                    .single();
                assertThat(deletedCount).isZero();
            }
        }

        // ==================== hardDeleteById 测试 ====================

        @Nested
        @DisplayName("hardDeleteById Tests")
        class HardDeleteByIdTests {

            @Test
            @DisplayName("should permanently delete record with timestamp strategy")
            void shouldHardDeleteTimestampRecord() {
                // Insert a record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test', NULL)").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                handler.hardDeleteById(1L);

                // Verify record is permanently deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_entity WHERE id = 1")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("should permanently delete soft-deleted record")
            void shouldHardDeleteSoftDeletedRecord() {
                // Insert a soft-deleted record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test', NOW())").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                handler.hardDeleteById(1L);

                // Verify record is permanently deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_entity WHERE id = 1")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("should permanently delete record with boolean strategy")
            void shouldHardDeleteBooleanRecord() {
                // Insert a record
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (1, 'test', false)").update();

                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                handler.hardDeleteById(1L);

                // Verify record is permanently deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM boolean_entity WHERE id = 1")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("should work for non-soft-deletable entity")
            void shouldHardDeleteNonSoftDeletableEntity() {
                // Insert a record
                jdbcClient.sql("INSERT INTO non_soft_delete_entity (id, name) VALUES (1, 'test')").update();

                var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
                );

                handler.hardDeleteById(1L);

                // Verify record is permanently deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM non_soft_delete_entity WHERE id = 1")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("should handle non-existent record gracefully")
            void shouldHandleNonExistentRecord() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Should not throw - just deletes 0 rows
                handler.hardDeleteById(99999L);
            }
        }

        // ==================== hardDeleteAllById 测试 ====================

        @Nested
        @DisplayName("hardDeleteAllById Tests")
        class HardDeleteAllByIdTests {

            @Test
            @DisplayName("should permanently delete multiple records")
            void shouldHardDeleteMultipleRecords() {
                // Insert records
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test1', NULL)").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (2, 'test2', NOW())").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (3, 'test3', NULL)").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                handler.hardDeleteAllById(List.of(1L, 2L, 3L));

                // Verify all records are permanently deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM timestamp_entity")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }

            @Test
            @DisplayName("should handle empty iterable gracefully")
            void shouldHandleEmptyIterable() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Should not throw
                handler.hardDeleteAllById(List.of());
            }

            @Test
            @DisplayName("should handle mixed existing and non-existing IDs")
            void shouldHandleMixedIds() {
                // Insert only one record
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (1, 'test', false)").update();

                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                // Delete with non-existing IDs
                handler.hardDeleteAllById(List.of(1L, 99998L, 99999L));

                // Verify existing record is deleted
                long count = jdbcClient.sql("SELECT COUNT(*) FROM boolean_entity")
                    .query(Long.class)
                    .single();
                assertThat(count).isZero();
            }
        }

        // ==================== 缓存失效测试 ====================

        @Nested
        @DisplayName("Cache Invalidation Tests")
        class CacheInvalidationTests {

            private EntityCacheManager cacheManager;

            @BeforeEach
            void setUpCache() {
                CacheProperties cacheProperties = new CacheProperties();
                DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);

                EntityCacheProperties properties = new EntityCacheProperties();
                properties.setEnabled(true);
                properties.setTtl(300000);
                properties.setMaxSize(1000);
                properties.setCacheNull(true);

                cacheManager = new EntityCacheManager(defaultCacheManager, properties);
            }

            @Test
            @DisplayName("restoreById should evict cache")
            void shouldEvictCacheOnRestore() {
                // Insert a soft-deleted record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'cached', NOW())").update();

                // Pre-populate cache
                EntityCache<TimestampSoftDeleteEntity> cache = cacheManager.getCache(TimestampSoftDeleteEntity.class);
                TimestampSoftDeleteEntity cachedEntity = new TimestampSoftDeleteEntity(1L, "cached", null);
                cache.put("timestamp_entity:1", cachedEntity);
                assertThat(cache.containsKey("timestamp_entity:1")).isTrue();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, cacheManager
                );

                handler.restoreById(1L);

                // Verify cache is evicted
                assertThat(cache.containsKey("timestamp_entity:1")).isFalse();
            }

            @Test
            @DisplayName("hardDeleteById should evict cache")
            void shouldEvictCacheOnHardDelete() {
                // Insert a record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'cached', NULL)").update();

                // Pre-populate cache
                EntityCache<TimestampSoftDeleteEntity> cache = cacheManager.getCache(TimestampSoftDeleteEntity.class);
                TimestampSoftDeleteEntity cachedEntity = new TimestampSoftDeleteEntity(1L, "cached", null);
                cache.put("timestamp_entity:1", cachedEntity);
                assertThat(cache.containsKey("timestamp_entity:1")).isTrue();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, cacheManager
                );

                handler.hardDeleteById(1L);

                // Verify cache is evicted
                assertThat(cache.containsKey("timestamp_entity:1")).isFalse();
            }

            @Test
            @DisplayName("restoreAllById should evict all caches")
            void shouldEvictAllCachesOnRestoreAll() {
                // Insert soft-deleted records
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (1, 'test1', true)").update();
                jdbcClient.sql("INSERT INTO boolean_entity (id, name, deleted) VALUES (2, 'test2', true)").update();

                // Pre-populate cache
                EntityCache<BooleanSoftDeleteEntity> cache = cacheManager.getCache(BooleanSoftDeleteEntity.class);
                cache.put("boolean_entity:1", new BooleanSoftDeleteEntity(1L, "test1", true));
                cache.put("boolean_entity:2", new BooleanSoftDeleteEntity(2L, "test2", true));
                assertThat(cache.containsKey("boolean_entity:1")).isTrue();
                assertThat(cache.containsKey("boolean_entity:2")).isTrue();

                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, cacheManager
                );

                handler.restoreAllById(List.of(1L, 2L));

                // Verify all caches are evicted
                assertThat(cache.containsKey("boolean_entity:1")).isFalse();
                assertThat(cache.containsKey("boolean_entity:2")).isFalse();
            }

            @Test
            @DisplayName("hardDeleteAllById should evict all caches")
            void shouldEvictAllCachesOnHardDeleteAll() {
                // Insert records
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test1', NULL)").update();
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (2, 'test2', NULL)").update();

                // Pre-populate cache
                EntityCache<TimestampSoftDeleteEntity> cache = cacheManager.getCache(TimestampSoftDeleteEntity.class);
                cache.put("timestamp_entity:1", new TimestampSoftDeleteEntity(1L, "test1", null));
                cache.put("timestamp_entity:2", new TimestampSoftDeleteEntity(2L, "test2", null));
                assertThat(cache.containsKey("timestamp_entity:1")).isTrue();
                assertThat(cache.containsKey("timestamp_entity:2")).isTrue();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, cacheManager
                );

                handler.hardDeleteAllById(List.of(1L, 2L));

                // Verify all caches are evicted
                assertThat(cache.containsKey("timestamp_entity:1")).isFalse();
                assertThat(cache.containsKey("timestamp_entity:2")).isFalse();
            }

            @Test
            @DisplayName("should not fail when cacheManager is null")
            void shouldNotFailWhenCacheManagerIsNull() {
                // Insert a record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test', NOW())").update();

                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                // Should not throw NPE
                handler.restoreById(1L);
                handler.hardDeleteById(1L);
            }

            @Test
            @DisplayName("should not fail when cache is not yet created")
            void shouldNotFailWhenCacheNotCreated() {
                // Insert a record
                jdbcClient.sql("INSERT INTO timestamp_entity (id, name, deleted_at) VALUES (1, 'test', NOW())").update();

                // cacheManager exists but no cache for the entity type
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, cacheManager
                );

                // Should not throw - getCacheIfPresent returns null
                handler.restoreById(1L);
                handler.hardDeleteById(1L);
            }
        }

        // ==================== appendSoftDeleteFilter 集成测试 ====================

        @Nested
        @DisplayName("appendSoftDeleteFilter Integration Tests")
        class AppendSoftDeleteFilterIntegrationTests {

            @Test
            @DisplayName("should not append filter when softDeleteStrategy is null")
            void shouldNotAppendFilterWhenStrategyIsNull() {
                var handler = new EntitySoftDeleteHandler<>(
                    NonSoftDeleteEntity.class, dialect, nonSoftDeleteMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM non_soft_delete_entity");
                handler.appendSoftDeleteFilter(sql, false, false);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM non_soft_delete_entity");
            }

            @Test
            @DisplayName("should not append filter when includeDeleted is true")
            void shouldNotAppendFilterWhenIncludeDeletedIsTrue() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM timestamp_entity WHERE status = 1");
                handler.appendSoftDeleteFilter(sql, true, true);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM timestamp_entity WHERE status = 1");
            }

            @Test
            @DisplayName("should append timestamp filter with AND clause")
            void shouldAppendTimestampFilterWithAnd() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM timestamp_entity WHERE status = 1");
                handler.appendSoftDeleteFilter(sql, true, false);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM timestamp_entity WHERE status = 1 AND deleted_at IS NULL");
            }

            @Test
            @DisplayName("should append timestamp filter with WHERE clause")
            void shouldAppendTimestampFilterWithWhere() {
                var handler = new EntitySoftDeleteHandler<>(
                    TimestampSoftDeleteEntity.class, dialect, timestampMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM timestamp_entity");
                handler.appendSoftDeleteFilter(sql, false, false);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM timestamp_entity WHERE deleted_at IS NULL");
            }

            @Test
            @DisplayName("should append boolean filter with AND clause")
            void shouldAppendBooleanFilterWithAnd() {
                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM boolean_entity WHERE status = 1");
                handler.appendSoftDeleteFilter(sql, true, false);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM boolean_entity WHERE status = 1 AND deleted = false");
            }

            @Test
            @DisplayName("should append boolean filter with WHERE clause")
            void shouldAppendBooleanFilterWithWhere() {
                var handler = new EntitySoftDeleteHandler<>(
                    BooleanSoftDeleteEntity.class, dialect, booleanMetadata, jdbcClient, null
                );

                StringBuilder sql = new StringBuilder("SELECT * FROM boolean_entity");
                handler.appendSoftDeleteFilter(sql, false, false);

                assertThat(sql.toString()).isEqualTo("SELECT * FROM boolean_entity WHERE deleted = false");
            }
        }

        // ==================== 辅助方法 ====================

        private DataSource createDataSource() {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:testdb_soft_delete;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }

        private void createTestTables() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE timestamp_entity (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100),
                        deleted_at TIMESTAMP
                    )
                    """);
                stmt.execute("""
                    CREATE TABLE boolean_entity (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100),
                        deleted BOOLEAN DEFAULT FALSE
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
                stmt.execute("DROP TABLE IF EXISTS timestamp_entity");
                stmt.execute("DROP TABLE IF EXISTS boolean_entity");
                stmt.execute("DROP TABLE IF EXISTS non_soft_delete_entity");
            } catch (Exception ignored) {
            }
        }
    }
}
