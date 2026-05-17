package io.github.afgprojects.framework.data.core.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonFieldMetadata 单元测试
 */
@DisplayName("CommonFieldMetadata Tests")
class CommonFieldMetadataTest {

    @Nested
    @DisplayName("预定义常量")
    class PredefinedConstantsTests {

        @Test
        @DisplayName("CREATED_AT 应该有正确的属性")
        void createdAtShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.CREATED_AT.getPropertyName()).isEqualTo("createdAt");
            assertThat(CommonFieldMetadata.CREATED_AT.getColumnName()).isEqualTo("created_at");
            assertThat(CommonFieldMetadata.CREATED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
            assertThat(CommonFieldMetadata.CREATED_AT.isId()).isFalse();
            assertThat(CommonFieldMetadata.CREATED_AT.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("UPDATED_AT 应该有正确的属性")
        void updatedAtShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.UPDATED_AT.getPropertyName()).isEqualTo("updatedAt");
            assertThat(CommonFieldMetadata.UPDATED_AT.getColumnName()).isEqualTo("updated_at");
            assertThat(CommonFieldMetadata.UPDATED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
            assertThat(CommonFieldMetadata.UPDATED_AT.isId()).isFalse();
            assertThat(CommonFieldMetadata.UPDATED_AT.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("DELETED 应该有正确的属性")
        void deletedShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.DELETED.getPropertyName()).isEqualTo("deleted");
            assertThat(CommonFieldMetadata.DELETED.getColumnName()).isEqualTo("deleted");
            assertThat(CommonFieldMetadata.DELETED.getFieldType()).isEqualTo(Boolean.class);
            assertThat(CommonFieldMetadata.DELETED.isId()).isFalse();
            assertThat(CommonFieldMetadata.DELETED.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("DELETED_AT 应该有正确的属性")
        void deletedAtShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.DELETED_AT.getPropertyName()).isEqualTo("deletedAt");
            assertThat(CommonFieldMetadata.DELETED_AT.getColumnName()).isEqualTo("deleted_at");
            assertThat(CommonFieldMetadata.DELETED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
            assertThat(CommonFieldMetadata.DELETED_AT.isId()).isFalse();
            assertThat(CommonFieldMetadata.DELETED_AT.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("TENANT_ID 应该有正确的属性")
        void tenantIdShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.TENANT_ID.getPropertyName()).isEqualTo("tenantId");
            assertThat(CommonFieldMetadata.TENANT_ID.getColumnName()).isEqualTo("tenant_id");
            assertThat(CommonFieldMetadata.TENANT_ID.getFieldType()).isEqualTo(String.class);
            assertThat(CommonFieldMetadata.TENANT_ID.isId()).isFalse();
            assertThat(CommonFieldMetadata.TENANT_ID.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("VERSION_LONG 应该有正确的属性")
        void versionLongShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.VERSION_LONG.getPropertyName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_LONG.getColumnName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_LONG.getFieldType()).isEqualTo(Long.class);
            assertThat(CommonFieldMetadata.VERSION_LONG.isId()).isFalse();
            assertThat(CommonFieldMetadata.VERSION_LONG.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("VERSION_INTEGER 应该有正确的属性")
        void versionIntegerShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.VERSION_INTEGER.getPropertyName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_INTEGER.getColumnName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_INTEGER.getFieldType()).isEqualTo(Integer.class);
            assertThat(CommonFieldMetadata.VERSION_INTEGER.isId()).isFalse();
            assertThat(CommonFieldMetadata.VERSION_INTEGER.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("CREATE_BY 应该有正确的属性")
        void createByShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.CREATE_BY.getPropertyName()).isEqualTo("createBy");
            assertThat(CommonFieldMetadata.CREATE_BY.getColumnName()).isEqualTo("create_by");
            assertThat(CommonFieldMetadata.CREATE_BY.getFieldType()).isEqualTo(String.class);
            assertThat(CommonFieldMetadata.CREATE_BY.isId()).isFalse();
            assertThat(CommonFieldMetadata.CREATE_BY.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("UPDATE_BY 应该有正确的属性")
        void updateByShouldHaveCorrectProperties() {
            assertThat(CommonFieldMetadata.UPDATE_BY.getPropertyName()).isEqualTo("updateBy");
            assertThat(CommonFieldMetadata.UPDATE_BY.getColumnName()).isEqualTo("update_by");
            assertThat(CommonFieldMetadata.UPDATE_BY.getFieldType()).isEqualTo(String.class);
            assertThat(CommonFieldMetadata.UPDATE_BY.isId()).isFalse();
            assertThat(CommonFieldMetadata.UPDATE_BY.isGenerated()).isFalse();
        }
    }

    @Nested
    @DisplayName("动态创建方法")
    class DynamicCreationTests {

        @Test
        @DisplayName("id(Long) 应该返回正确的主键字段元数据")
        void idLongShouldReturnCorrectMetadata() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.id(Long.class);
            assertThat(metadata.getPropertyName()).isEqualTo("id");
            assertThat(metadata.getColumnName()).isEqualTo("id");
            assertThat(metadata.getFieldType()).isEqualTo(Long.class);
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("id(String) 应该返回正确的主键字段元数据")
        void idStringShouldReturnCorrectMetadata() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.id(String.class);
            assertThat(metadata.getPropertyName()).isEqualTo("id");
            assertThat(metadata.getColumnName()).isEqualTo("id");
            assertThat(metadata.getFieldType()).isEqualTo(String.class);
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("version(Long) 应该返回 VERSION_LONG")
        void versionLongShouldReturnVersionLong() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.version(Long.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.VERSION_LONG);
        }

        @Test
        @DisplayName("version(Integer) 应该返回 VERSION_INTEGER")
        void versionIntegerShouldReturnVersionInteger() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.version(Integer.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.VERSION_INTEGER);
        }
    }

    @Nested
    @DisplayName("字段匹配检查")
    class MatchTests {

        @Test
        @DisplayName("match 应该正确匹配 createdAt 字段")
        void shouldMatchCreatedAt() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createdAt", LocalDateTime.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.CREATED_AT);
        }

        @Test
        @DisplayName("match 应该正确匹配 updatedAt 字段")
        void shouldMatchUpdatedAt() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("updatedAt", LocalDateTime.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.UPDATED_AT);
        }

        @Test
        @DisplayName("match 应该正确匹配 deleted 字段")
        void shouldMatchDeleted() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("deleted", Boolean.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.DELETED);
        }

        @Test
        @DisplayName("match 应该正确匹配 tenantId 字段")
        void shouldMatchTenantId() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("tenantId", String.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.TENANT_ID);
        }

        @Test
        @DisplayName("match 应该正确匹配 version Long 字段")
        void shouldMatchVersionLong() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("version", Long.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.VERSION_LONG);
        }

        @Test
        @DisplayName("match 应该正确匹配 version Integer 字段")
        void shouldMatchVersionInteger() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("version", Integer.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.VERSION_INTEGER);
        }

        @Test
        @DisplayName("match 应该正确匹配 createBy 字段")
        void shouldMatchCreateBy() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createBy", String.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.CREATE_BY);
        }

        @Test
        @DisplayName("match 应该正确匹配 createdBy 别名")
        void shouldMatchCreatedByAlias() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createdBy", String.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.CREATE_BY);
        }

        @Test
        @DisplayName("match 应该正确匹配 updateBy 字段")
        void shouldMatchUpdateBy() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("updateBy", String.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.UPDATE_BY);
        }

        @Test
        @DisplayName("match 应该正确匹配 updatedBy 别名")
        void shouldMatchUpdatedByAlias() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("updatedBy", String.class);
            assertThat(metadata).isSameAs(CommonFieldMetadata.UPDATE_BY);
        }

        @Test
        @DisplayName("match 类型不匹配时应该返回 null")
        void shouldReturnNullForMismatchedType() {
            // createdAt 应该是 LocalDateTime，传入 String
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createdAt", String.class);
            assertThat(metadata).isNull();
        }

        @Test
        @DisplayName("match 未知字段名应该返回 null")
        void shouldReturnNullForUnknownFieldName() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("unknownField", String.class);
            assertThat(metadata).isNull();
        }

        @Test
        @DisplayName("match 使用全限定类名应该正确匹配")
        void shouldMatchWithFullyQualifiedName() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createdAt", "java.time.LocalDateTime");
            assertThat(metadata).isSameAs(CommonFieldMetadata.CREATED_AT);
        }

        @Test
        @DisplayName("match 使用简单类名应该正确匹配")
        void shouldMatchWithSimpleName() {
            DatabaseFieldMetadata metadata = CommonFieldMetadata.match("createdAt", "LocalDateTime");
            assertThat(metadata).isSameAs(CommonFieldMetadata.CREATED_AT);
        }
    }

    @Nested
    @DisplayName("字段名称检查")
    class FieldNameCheckTests {

        @Test
        @DisplayName("isCommonFieldName 应该正确识别通用字段名")
        void shouldIdentifyCommonFieldNames() {
            assertThat(CommonFieldMetadata.isCommonFieldName("createdAt")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("updatedAt")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("deleted")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("deletedAt")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("tenantId")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("version")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("createBy")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("updateBy")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("createdBy")).isTrue();
            assertThat(CommonFieldMetadata.isCommonFieldName("updatedBy")).isTrue();
        }

        @Test
        @DisplayName("isCommonFieldName 应该正确识别非通用字段名")
        void shouldIdentifyNonCommonFieldNames() {
            assertThat(CommonFieldMetadata.isCommonFieldName("id")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("name")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("email")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("status")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("createdAtTime")).isFalse();
        }
    }
}