package io.github.afgprojects.framework.data.core.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonFieldMetadata 单元测试
 * <p>
 * 验证通用字段元数据的常量、工厂方法和匹配逻辑。
 */
class CommonFieldMetadataTest {

    // ========== 预定义常量 ==========

    @Nested
    @DisplayName("预定义常量")
    class PredefinedConstants {

        @Test
        @DisplayName("should have createdAt field with correct properties")
        void shouldHaveCreatedAtField_withCorrectProperties() {
            assertThat(CommonFieldMetadata.CREATED_AT.getPropertyName()).isEqualTo("createdAt");
            assertThat(CommonFieldMetadata.CREATED_AT.getColumnName()).isEqualTo("created_at");
            assertThat(CommonFieldMetadata.CREATED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
            assertThat(CommonFieldMetadata.CREATED_AT.isId()).isFalse();
            assertThat(CommonFieldMetadata.CREATED_AT.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("should have updatedAt field with correct properties")
        void shouldHaveUpdatedAtField_withCorrectProperties() {
            assertThat(CommonFieldMetadata.UPDATED_AT.getPropertyName()).isEqualTo("updatedAt");
            assertThat(CommonFieldMetadata.UPDATED_AT.getColumnName()).isEqualTo("updated_at");
            assertThat(CommonFieldMetadata.UPDATED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
            assertThat(CommonFieldMetadata.UPDATED_AT.isId()).isFalse();
        }

        @Test
        @DisplayName("should have deleted field with correct properties")
        void shouldHaveDeletedField_withCorrectProperties() {
            assertThat(CommonFieldMetadata.DELETED.getPropertyName()).isEqualTo("deleted");
            assertThat(CommonFieldMetadata.DELETED.getColumnName()).isEqualTo("deleted");
            assertThat(CommonFieldMetadata.DELETED.getFieldType()).isEqualTo(Boolean.class);
            assertThat(CommonFieldMetadata.DELETED.isId()).isFalse();
        }

        @Test
        @DisplayName("should have deletedAt field with correct properties")
        void shouldHaveDeletedAtField_withCorrectProperties() {
            assertThat(CommonFieldMetadata.DELETED_AT.getPropertyName()).isEqualTo("deletedAt");
            assertThat(CommonFieldMetadata.DELETED_AT.getColumnName()).isEqualTo("deleted_at");
            assertThat(CommonFieldMetadata.DELETED_AT.getFieldType()).isEqualTo(LocalDateTime.class);
        }

        @Test
        @DisplayName("should have tenantId field with correct properties")
        void shouldHaveTenantIdField_withCorrectProperties() {
            assertThat(CommonFieldMetadata.TENANT_ID.getPropertyName()).isEqualTo("tenantId");
            assertThat(CommonFieldMetadata.TENANT_ID.getColumnName()).isEqualTo("tenant_id");
            assertThat(CommonFieldMetadata.TENANT_ID.getFieldType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("should have version fields with correct properties")
        void shouldHaveVersionFields_withCorrectProperties() {
            assertThat(CommonFieldMetadata.VERSION_LONG.getPropertyName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_LONG.getFieldType()).isEqualTo(Long.class);

            assertThat(CommonFieldMetadata.VERSION_INTEGER.getPropertyName()).isEqualTo("version");
            assertThat(CommonFieldMetadata.VERSION_INTEGER.getFieldType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("should have audit fields with correct properties")
        void shouldHaveAuditFields_withCorrectProperties() {
            assertThat(CommonFieldMetadata.CREATE_BY.getPropertyName()).isEqualTo("createBy");
            assertThat(CommonFieldMetadata.CREATE_BY.getColumnName()).isEqualTo("create_by");
            assertThat(CommonFieldMetadata.CREATE_BY.getFieldType()).isEqualTo(String.class);

            assertThat(CommonFieldMetadata.UPDATE_BY.getPropertyName()).isEqualTo("updateBy");
            assertThat(CommonFieldMetadata.UPDATE_BY.getColumnName()).isEqualTo("update_by");
            assertThat(CommonFieldMetadata.UPDATE_BY.getFieldType()).isEqualTo(String.class);
        }
    }

    // ========== id() 工厂方法 ==========

    @Nested
    @DisplayName("id factory method")
    class IdFactoryMethod {

        @Test
        @DisplayName("should create id field with Long type")
        void shouldCreateIdField_withLongType() {
            DatabaseFieldMetadata idField = CommonFieldMetadata.id(Long.class);

            assertThat(idField.getPropertyName()).isEqualTo("id");
            assertThat(idField.getColumnName()).isEqualTo("id");
            assertThat(idField.getFieldType()).isEqualTo(Long.class);
            assertThat(idField.isId()).isTrue();
            assertThat(idField.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("should create id field with String type")
        void shouldCreateIdField_withStringType() {
            DatabaseFieldMetadata idField = CommonFieldMetadata.id(String.class);

            assertThat(idField.getFieldType()).isEqualTo(String.class);
            assertThat(idField.isId()).isTrue();
            assertThat(idField.isGenerated()).isTrue();
        }
    }

    // ========== version() 工厂方法 ==========

    @Nested
    @DisplayName("version factory method")
    class VersionFactoryMethod {

        @Test
        @DisplayName("should return Integer version when Integer type provided")
        void shouldReturnIntegerVersion_whenIntegerTypeProvided() {
            DatabaseFieldMetadata versionField = CommonFieldMetadata.version(Integer.class);

            assertThat(versionField).isSameAs(CommonFieldMetadata.VERSION_INTEGER);
        }

        @Test
        @DisplayName("should return Integer version when int type provided")
        void shouldReturnIntegerVersion_whenIntTypeProvided() {
            DatabaseFieldMetadata versionField = CommonFieldMetadata.version(int.class);

            assertThat(versionField).isSameAs(CommonFieldMetadata.VERSION_INTEGER);
        }

        @Test
        @DisplayName("should return Long version when Long type provided")
        void shouldReturnLongVersion_whenLongTypeProvided() {
            DatabaseFieldMetadata versionField = CommonFieldMetadata.version(Long.class);

            assertThat(versionField).isSameAs(CommonFieldMetadata.VERSION_LONG);
        }
    }

    // ========== match() 方法 ==========

    @Nested
    @DisplayName("match method")
    class MatchMethod {

        @Test
        @DisplayName("should match createdAt with LocalDateTime type")
        void shouldMatchCreatedAt_withLocalDateTimeType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createdAt", LocalDateTime.class);

            assertThat(result).isSameAs(CommonFieldMetadata.CREATED_AT);
        }

        @Test
        @DisplayName("should not match createdAt with wrong type")
        void shouldNotMatchCreatedAt_withWrongType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createdAt", String.class);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should match updatedAt with LocalDateTime type")
        void shouldMatchUpdatedAt_withLocalDateTimeType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("updatedAt", LocalDateTime.class);

            assertThat(result).isSameAs(CommonFieldMetadata.UPDATED_AT);
        }

        @Test
        @DisplayName("should match deleted with Boolean type")
        void shouldMatchDeleted_withBooleanType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("deleted", Boolean.class);

            assertThat(result).isSameAs(CommonFieldMetadata.DELETED);
        }

        @Test
        @DisplayName("should match deleted with primitive boolean type")
        void shouldMatchDeleted_withPrimitiveBooleanType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("deleted", "boolean");

            assertThat(result).isSameAs(CommonFieldMetadata.DELETED);
        }

        @Test
        @DisplayName("should match tenantId with String type")
        void shouldMatchTenantId_withStringType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("tenantId", String.class);

            assertThat(result).isSameAs(CommonFieldMetadata.TENANT_ID);
        }

        @Test
        @DisplayName("should match version with Integer type")
        void shouldMatchVersion_withIntegerType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("version", Integer.class);

            assertThat(result).isSameAs(CommonFieldMetadata.VERSION_INTEGER);
        }

        @Test
        @DisplayName("should match version with Long type")
        void shouldMatchVersion_withLongType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("version", Long.class);

            assertThat(result).isSameAs(CommonFieldMetadata.VERSION_LONG);
        }

        @Test
        @DisplayName("should not match version with wrong type")
        void shouldNotMatchVersion_withWrongType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("version", String.class);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should match createBy with String type")
        void shouldMatchCreateBy_withStringType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createBy", String.class);

            assertThat(result).isSameAs(CommonFieldMetadata.CREATE_BY);
        }

        @Test
        @DisplayName("should match updateBy with String type")
        void shouldMatchUpdateBy_withStringType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("updateBy", String.class);

            assertThat(result).isSameAs(CommonFieldMetadata.UPDATE_BY);
        }

        @Test
        @DisplayName("should match createdBy alias with String type")
        void shouldMatchCreatedByAlias_withStringType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createdBy", String.class);

            assertThat(result).isSameAs(CommonFieldMetadata.CREATE_BY);
        }

        @Test
        @DisplayName("should match updatedBy alias with String type")
        void shouldMatchUpdatedByAlias_withStringType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("updatedBy", String.class);

            assertThat(result).isSameAs(CommonFieldMetadata.UPDATE_BY);
        }

        @Test
        @DisplayName("should return null for unknown property name")
        void shouldReturnNull_forUnknownPropertyName() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("customField", String.class);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should match using full qualified type name")
        void shouldMatchUsingFullQualifiedTypeName() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createdAt", "java.time.LocalDateTime");

            assertThat(result).isSameAs(CommonFieldMetadata.CREATED_AT);
        }

        @Test
        @DisplayName("should match using simple type name")
        void shouldMatchUsingSimpleTypeName() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("createdAt", "LocalDateTime");

            assertThat(result).isSameAs(CommonFieldMetadata.CREATED_AT);
        }

        @Test
        @DisplayName("should match deletedAt with LocalDateTime type")
        void shouldMatchDeletedAt_withLocalDateTimeType() {
            DatabaseFieldMetadata result = CommonFieldMetadata.match("deletedAt", LocalDateTime.class);

            assertThat(result).isSameAs(CommonFieldMetadata.DELETED_AT);
        }
    }

    // ========== isCommonFieldName() ==========

    @Nested
    @DisplayName("isCommonFieldName method")
    class IsCommonFieldName {

        @Test
        @DisplayName("should return true for common field names")
        void shouldReturnTrue_forCommonFieldNames() {
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
        @DisplayName("should return false for non-common field names")
        void shouldReturnFalse_forNonCommonFieldNames() {
            assertThat(CommonFieldMetadata.isCommonFieldName("username")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("status")).isFalse();
            assertThat(CommonFieldMetadata.isCommonFieldName("id")).isFalse();
        }
    }

    // ========== DatabaseFieldMetadata default methods ==========

    @Nested
    @DisplayName("DatabaseFieldMetadata default methods")
    class DatabaseFieldMetadataDefaults {

        @Test
        @DisplayName("should return default values for DatabaseFieldMetadata methods")
        void shouldReturnDefaultValues_forDatabaseFieldMetadataMethods() {
            DatabaseFieldMetadata idField = CommonFieldMetadata.id(Long.class);

            assertThat(idField.getJdbcType()).isEqualTo(java.sql.Types.NULL);
            assertThat(idField.isNullable()).isTrue();
            assertThat(idField.getDefaultValue()).isNull();
            assertThat(idField.getLength()).isEqualTo(0);
            assertThat(idField.getPrecision()).isEqualTo(0);
            assertThat(idField.getScale()).isEqualTo(0);
            assertThat(idField.isUnique()).isFalse();
            assertThat(idField.isInsertable()).isTrue();
            assertThat(idField.isUpdatable()).isTrue();
            assertThat(idField.getColumnDefinition()).isNull();
        }
    }
}