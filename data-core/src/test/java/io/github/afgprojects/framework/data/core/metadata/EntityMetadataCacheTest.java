/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * EntityMetadataCache 单元测试
 * <p>
 * 测试实体元数据缓存的加载器链、缓存行为和并发安全。
 */
class EntityMetadataCacheTest {

    // ==================== 默认构造器 ====================

    @Nested
    @DisplayName("默认构造器")
    class DefaultConstructor {

        @Test
        @DisplayName("should create instance with non-empty loaders when default constructor used")
        void shouldCreateInstanceWithNonEmptyLoaders_whenDefaultConstructorUsed() {
            EntityMetadataCache cache = new EntityMetadataCache();

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            assertThat(loaders).isNotEmpty();
        }

        @Test
        @DisplayName("should register AptMetadataLoader when no SPI loaders found")
        void shouldRegisterAptMetadataLoader_whenNoSpiLoadersFound() {
            EntityMetadataCache cache = new EntityMetadataCache();

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            assertThat(loaders.stream().map(EntityMetadataLoader::getName))
                .contains("APT");
        }

        @Test
        @DisplayName("should register ReflectiveMetadataLoader when no SPI loaders found")
        void shouldRegisterReflectiveMetadataLoader_whenNoSpiLoadersFound() {
            EntityMetadataCache cache = new EntityMetadataCache();

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            assertThat(loaders.stream().map(EntityMetadataLoader::getName))
                .contains("Reflective");
        }

        @Test
        @DisplayName("should sort loaders by priority when default constructor used")
        void shouldSortLoadersByPriority_whenDefaultConstructorUsed() {
            EntityMetadataCache cache = new EntityMetadataCache();

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            // APT (priority 0) should come before Reflective (priority 1000)
            int aptIndex = -1;
            int reflectiveIndex = -1;
            for (int i = 0; i < loaders.size(); i++) {
                if (loaders.get(i).getName().equals("APT")) aptIndex = i;
                if (loaders.get(i).getName().equals("Reflective")) reflectiveIndex = i;
            }
            assertThat(aptIndex).isLessThan(reflectiveIndex);
        }
    }

    // ==================== 自定义加载器列表 ====================

    @Nested
    @DisplayName("自定义加载器列表")
    class CustomLoaderList {

        @Test
        @DisplayName("should accept custom loaders when provided via constructor")
        void shouldAcceptCustomLoaders_whenProvidedViaConstructor() {
            EntityMetadataLoader loader1 = createMockLoader("Loader1", 200);
            EntityMetadataLoader loader2 = createMockLoader("Loader2", 100);

            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader1, loader2));

            List<EntityMetadataLoader> loaders = cache.getLoaders();
            assertThat(loaders).hasSize(2);
            assertThat(loaders.stream().map(EntityMetadataLoader::getName))
                .containsExactly("Loader2", "Loader1"); // sorted by priority
        }

        @Test
        @DisplayName("should sort loaders by priority when custom list provided")
        void shouldSortLoadersByPriority_whenCustomListProvided() {
            EntityMetadataLoader lowPriority = createMockLoader("Low", 1000);
            EntityMetadataLoader highPriority = createMockLoader("High", 0);
            EntityMetadataLoader mediumPriority = createMockLoader("Medium", 500);

            EntityMetadataCache cache = new EntityMetadataCache(
                List.of(lowPriority, highPriority, mediumPriority));

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            assertThat(loaders.stream().map(EntityMetadataLoader::getName))
                .containsExactly("High", "Medium", "Low");
        }

        @Test
        @DisplayName("should not modify original list when custom list provided")
        void shouldNotModifyOriginalList_whenCustomListProvided() {
            List<EntityMetadataLoader> original = new ArrayList<>();
            original.add(createMockLoader("Loader1", 100));
            original.add(createMockLoader("Loader2", 50));

            new EntityMetadataCache(original);

            // Original list order should not be changed (cache sorts its own copy)
            assertThat(original.get(0).getName()).isEqualTo("Loader1");
            assertThat(original.get(1).getName()).isEqualTo("Loader2");
        }
    }

    // ==================== 缓存命中 ====================

    @Nested
    @DisplayName("缓存命中")
    class CacheHit {

        @Test
        @DisplayName("should return same instance when get called twice")
        void shouldReturnSameInstance_whenGetCalledTwice() {
            EntityMetadataLoader loader = new StubMetadataLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));

            EntityMetadata<TestEntity> first = cache.get(TestEntity.class);
            EntityMetadata<TestEntity> second = cache.get(TestEntity.class);

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should use cached value when multiple get calls")
        void shouldUseCachedValue_whenMultipleGetCalls() {
            CountingLoader loader = new CountingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));

            cache.get(TestEntity.class);
            cache.get(TestEntity.class);
            cache.get(TestEntity.class);

            assertThat(loader.getLoadCount()).isEqualTo(1); // only loaded once
        }
    }

    // ==================== 加载失败降级 ====================

    @Nested
    @DisplayName("加载失败降级")
    class LoadFailureFallback {

        @Test
        @DisplayName("should return EmptyEntityMetadata when all loaders fail")
        void shouldReturnEmptyEntityMetadata_whenAllLoadersFail() {
            EntityMetadataLoader failingLoader = new FailingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(failingLoader));

            EntityMetadata<TestEntity> metadata = cache.get(TestEntity.class);

            // EmptyEntityMetadata returns simple class name in snake case as table name
            assertThat(metadata.getTableName()).isEqualTo("test_entity");
        }

        @Test
        @DisplayName("should return EmptyEntityMetadata when no loader supports class")
        void shouldReturnEmptyEntityMetadata_whenNoLoaderSupportsClass() {
            EntityMetadataLoader unsupportedLoader = new UnsupportedLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(unsupportedLoader));

            EntityMetadata<TestEntity> metadata = cache.get(TestEntity.class);

            assertThat(metadata.getTableName()).isEqualTo("test_entity");
        }

        @Test
        @DisplayName("should cache failure and not retry when get called again")
        void shouldCacheFailureAndNotRetry_whenGetCalledAgain() {
            CountingFailingLoader loader = new CountingFailingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));

            cache.get(TestEntity.class);
            cache.get(TestEntity.class);

            // Should only attempt load once, then cache the failure
            assertThat(loader.getAttemptCount()).isEqualTo(1);
        }
    }

    // ==================== clear 方法 ====================

    @Nested
    @DisplayName("clear 方法")
    class ClearMethod {

        @Test
        @DisplayName("should allow reload when clear called")
        void shouldAllowReload_whenClearCalled() {
            CountingLoader loader = new CountingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));

            cache.get(TestEntity.class);
            cache.clear();
            cache.get(TestEntity.class);

            assertThat(loader.getLoadCount()).isEqualTo(2); // loaded twice after clear
        }

        @Test
        @DisplayName("should clear failure cache when clear called")
        void shouldClearFailureCache_whenClearCalled() {
            CountingFailingLoader loader = new CountingFailingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));

            cache.get(TestEntity.class); // fails and caches failure
            cache.clear();
            cache.get(TestEntity.class); // should retry

            assertThat(loader.getAttemptCount()).isEqualTo(2);
        }
    }

    // ==================== getLoaders 方法 ====================

    @Nested
    @DisplayName("getLoaders 方法")
    class GetLoadersMethod {

        @Test
        @DisplayName("should return immutable list when getLoaders called")
        void shouldReturnImmutableList_whenGetLoadersCalled() {
            EntityMetadataCache cache = new EntityMetadataCache();

            List<EntityMetadataLoader> loaders = cache.getLoaders();

            assertThatThrownBy(() -> loaders.add(createMockLoader("New", 100)))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== 并发访问 ====================

    @Nested
    @DisplayName("并发访问")
    class ConcurrentAccess {

        @Test
        @DisplayName("should not throw exception when concurrent access")
        void shouldNotThrowException_whenConcurrentAccess() throws InterruptedException {
            EntityMetadataCache cache = new EntityMetadataCache();
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        cache.get(TestEntity.class);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(errorCount.get()).isZero();
        }

        @Test
        @DisplayName("should return same instance when concurrent get calls")
        void shouldReturnSameInstance_whenConcurrentGetCalls() throws InterruptedException {
            CountingLoader loader = new CountingLoader();
            EntityMetadataCache cache = new EntityMetadataCache(List.of(loader));
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<EntityMetadata<TestEntity>> results = new CopyOnWriteArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        results.add(cache.get(TestEntity.class));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // All results should be the same instance
            assertThat(results).allMatch(m -> m == results.get(0));
        }
    }

    // ==================== Helper Methods and Classes ====================

    private EntityMetadataLoader createMockLoader(String name, int priority) {
        return new EntityMetadataLoader() {
            @Override
            public <T> EntityMetadata<T> load(Class<T> entityClass) {
                return null;
            }

            @Override
            public boolean supports(Class<?> entityClass) {
                return false;
            }

            @Override
            public int getPriority() {
                return priority;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    private static class TestEntity {
        // Test entity class
    }

    private static class StubMetadataLoader implements EntityMetadataLoader {
        @Override
        @SuppressWarnings("unchecked")
        public <T> EntityMetadata<T> load(Class<T> entityClass) {
            return (EntityMetadata<T>) new StubEntityMetadata(entityClass);
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getName() {
            return "Stub";
        }
    }

    private record StubEntityMetadata<T>(Class<T> entityClass) implements EntityMetadata<T> {
        @Override
        public Class<T> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return "stub_table";
        }

        @Override
        public List<? extends FieldMetadata> getFields() {
            return List.of();
        }

        @Override
        public FieldMetadata getField(String fieldName) {
            return null;
        }

        @Override
        public FieldMetadata getIdField() {
            return null;
        }

        @Override
        public String getIdFieldName() {
            return "id";
        }

        @Override
        public FieldMetadata getSoftDeleteField() {
            return null;
        }

        @Override
        public FieldMetadata getTenantField() {
            return null;
        }

        @Override
        public Map<String, String> getColumnToFieldMap() {
            return Map.of();
        }

        @Override
        public Map<String, String> getFieldToColumnMap() {
            return Map.of();
        }

        @Override
        public boolean hasTrait(EntityTrait trait) {
            return false;
        }

        @Override
        public Set<EntityTrait> getTraits() {
            return Set.of();
        }

        @Override
        public Condition getDefaultCondition() {
            return Condition.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }

        @Override
        public List<RelationMetadata> getRelations() {
            return List.of();
        }
    }

    private static class CountingLoader implements EntityMetadataLoader {
        private int loadCount = 0;

        @Override
        @SuppressWarnings("unchecked")
        public <T> EntityMetadata<T> load(Class<T> entityClass) {
            loadCount++;
            return (EntityMetadata<T>) new StubEntityMetadata(entityClass);
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getName() {
            return "Counting";
        }

        int getLoadCount() {
            return loadCount;
        }
    }

    private static class FailingLoader implements EntityMetadataLoader {
        @Override
        public <T> EntityMetadata<T> load(Class<T> entityClass) {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getName() {
            return "Failing";
        }
    }

    private static class CountingFailingLoader implements EntityMetadataLoader {
        private int attemptCount = 0;

        @Override
        public <T> EntityMetadata<T> load(Class<T> entityClass) {
            attemptCount++;
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getName() {
            return "CountingFailing";
        }

        int getAttemptCount() {
            return attemptCount;
        }
    }

    private static class UnsupportedLoader implements EntityMetadataLoader {
        @Override
        public <T> EntityMetadata<T> load(Class<T> entityClass) {
            return null;
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return false;
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public String getName() {
            return "Unsupported";
        }
    }
}
