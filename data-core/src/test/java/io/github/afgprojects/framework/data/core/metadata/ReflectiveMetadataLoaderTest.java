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

import io.github.afgprojects.framework.data.core.exception.MetadataLoadException;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * ReflectiveMetadataLoader 单元测试
 * <p>
 * 测试反射元数据加载器的优先级、名称、provider 注入和加载行为。
 */
class ReflectiveMetadataLoaderTest {

    // ==================== 优先级和名称 ====================

    @Nested
    @DisplayName("优先级和名称")
    class PriorityAndName {

        @Test
        @DisplayName("should return 1000 when getPriority called")
        void shouldReturn1000_whenGetPriorityCalled() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();

            assertThat(loader.getPriority()).isEqualTo(1000);
        }

        @Test
        @DisplayName("should return Reflective when getName called")
        void shouldReturnReflective_whenGetNameCalled() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();

            assertThat(loader.getName()).isEqualTo("Reflective");
        }
    }

    // ==================== 无 provider 时 ====================

    @Nested
    @DisplayName("无 provider 时")
    class WithoutProvider {

        @Test
        @DisplayName("should return false when supports called without provider")
        void shouldReturnFalse_whenSupportsCalledWithoutProvider() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();

            boolean result = loader.supports(TestEntity.class);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return null when load called without provider")
        void shouldReturnNull_whenLoadCalledWithoutProvider() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();

            EntityMetadata<TestEntity> result = loader.load(TestEntity.class);

            assertThat(result).isNull();
        }
    }

    // ==================== 注入 provider 后 ====================

    @Nested
    @DisplayName("注入 provider 后")
    class WithProvider {

        @Test
        @DisplayName("should return true when supports called with provider")
        void shouldReturnTrue_whenSupportsCalledWithProvider() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
            loader.setMetadataProvider(new StubMetadataProvider());

            boolean result = loader.supports(TestEntity.class);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return metadata when load called with provider")
        void shouldReturnMetadata_whenLoadCalledWithProvider() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
            loader.setMetadataProvider(new StubMetadataProvider());

            EntityMetadata<TestEntity> result = loader.load(TestEntity.class);

            assertThat(result).isNotNull();
            assertThat(result.getEntityClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("should throw MetadataLoadException when provider throws exception")
        void shouldThrowMetadataLoadException_whenProviderThrowsException() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
            loader.setMetadataProvider(new FailingMetadataProvider());

            assertThatThrownBy(() -> loader.load(TestEntity.class))
                .isInstanceOf(MetadataLoadException.class)
                .hasMessageContaining("Failed to create reflective metadata");
        }

        @Test
        @DisplayName("should use injected provider when setMetadataProvider called")
        void shouldUseInjectedProvider_whenSetMetadataProviderCalled() {
            ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
            CountingMetadataProvider provider = new CountingMetadataProvider();
            loader.setMetadataProvider(provider);

            loader.supports(TestEntity.class);
            loader.load(TestEntity.class);

            assertThat(provider.getSupportsCount()).isEqualTo(1);
            assertThat(provider.getCreateCount()).isEqualTo(1);
        }
    }

    // ==================== Helper Methods and Classes ====================

    private static class TestEntity {
        // Test entity class
    }

    private static class StubMetadataProvider implements MetadataProvider {
        @Override
        @SuppressWarnings("unchecked")
        public <T> EntityMetadata<T> createMetadata(Class<T> entityClass) {
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

    private static class FailingMetadataProvider implements MetadataProvider {
        @Override
        public <T> EntityMetadata<T> createMetadata(Class<T> entityClass) {
            throw new RuntimeException("Simulated provider failure");
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return true;
        }
    }

    private static class CountingMetadataProvider implements MetadataProvider {
        private int supportsCount = 0;
        private int createCount = 0;

        @Override
        @SuppressWarnings("unchecked")
        public <T> EntityMetadata<T> createMetadata(Class<T> entityClass) {
            createCount++;
            return (EntityMetadata<T>) new StubEntityMetadata(entityClass);
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            supportsCount++;
            return true;
        }

        int getSupportsCount() {
            return supportsCount;
        }

        int getCreateCount() {
            return createCount;
        }
    }
}
