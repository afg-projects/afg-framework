package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.DatabaseFieldMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReflectiveEntityMetadata 测试
 */
@DisplayName("ReflectiveEntityMetadata Tests")
class ReflectiveEntityMetadataTest {

    @Nested
    @DisplayName("表名解析")
    class TableNameTests {

        @Test
        @DisplayName("应该从 @Table 注解读取表名")
        void shouldReadTableNameFromAnnotation() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getTableName()).isEqualTo("sys_user");
        }

        @Test
        @DisplayName("没有 @Table 注解时应该使用类名转 snake_case")
        void shouldUseClassNameAsTableName() {
            ReflectiveEntityMetadata<NoTableEntity> metadata = ReflectiveEntityMetadata.create(NoTableEntity.class);
            assertThat(metadata.getTableName()).isEqualTo("no_table_entity");
        }

        @Test
        @DisplayName("@Table 注解 name 为空时应该使用类名")
        void shouldUseClassNameWhenTableNameEmpty() {
            ReflectiveEntityMetadata<EmptyTableNameEntity> metadata = ReflectiveEntityMetadata.create(EmptyTableNameEntity.class);
            assertThat(metadata.getTableName()).isEqualTo("empty_table_name_entity");
        }
    }

    @Nested
    @DisplayName("列名解析")
    class ColumnNameTests {

        @Test
        @DisplayName("应该从 @Column 注解读取列名")
        void shouldReadColumnNameFromAnnotation() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getColumnName("deleted")).isEqualTo("is_deleted");
        }

        @Test
        @DisplayName("应该将 camelCase 转换为 snake_case")
        void shouldConvertCamelCaseToSnakeCase() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getColumnName("userName")).isEqualTo("user_name");
        }

        @Test
        @DisplayName("应该处理单个单词属性名")
        void shouldHandleSingleWordPropertyName() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getColumnName("status")).isEqualTo("status");
        }

        @Test
        @DisplayName("应该处理连续大写字母")
        void shouldHandleConsecutiveUppercase() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getColumnName("URL")).isEqualTo("u_r_l");
        }

        @Test
        @DisplayName("Boolean 类型字段通过 @Column 指定列名")
        void shouldHandleBooleanWithColumnAnnotation() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            // deleted 属性通过 @Column(name="is_deleted") 指定列名
            assertThat(metadata.getColumnName("deleted")).isEqualTo("is_deleted");
        }
    }

    @Nested
    @DisplayName("主键识别")
    class IdFieldTests {

        @Test
        @DisplayName("应该识别 @Id 注解标记的主键")
        void shouldDetectIdField() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            DatabaseFieldMetadata idField = metadata.getIdField();
            assertThat(idField).isNotNull();
            assertThat(idField.getPropertyName()).isEqualTo("id");
            assertThat(idField.isId()).isTrue();
        }

        @Test
        @DisplayName("主键字段应该标记为自动生成")
        void idFieldShouldBeGenerated() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            DatabaseFieldMetadata idField = metadata.getIdField();
            assertThat(idField.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("没有 @Id 注解时应该查找名为 id 的字段")
        void shouldFallbackToIdFieldName() {
            ReflectiveEntityMetadata<NoIdAnnotationEntity> metadata = ReflectiveEntityMetadata.create(NoIdAnnotationEntity.class);
            DatabaseFieldMetadata idField = metadata.getIdField();
            assertThat(idField).isNotNull();
            assertThat(idField.getPropertyName()).isEqualTo("id");
        }
    }

    @Nested
    @DisplayName("字段列表")
    class FieldListTests {

        @Test
        @DisplayName("应该获取所有字段")
        void shouldGetAllFields() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            List<FieldMetadata> fields = metadata.getFields();
            assertThat(fields).isNotEmpty();
        }

        @Test
        @DisplayName("应该跳过静态字段")
        void shouldSkipStaticFields() {
            ReflectiveEntityMetadata<WithStaticFieldEntity> metadata = ReflectiveEntityMetadata.create(WithStaticFieldEntity.class);
            List<FieldMetadata> fields = metadata.getFields();
            // 静态字段不应该出现在字段列表中
            assertThat(fields.stream().anyMatch(f -> f.getPropertyName().equals("STATIC_FIELD"))).isFalse();
        }

        @Test
        @DisplayName("应该根据属性名获取字段")
        void shouldGetFieldByName() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            DatabaseFieldMetadata field = metadata.getField("userName");
            assertThat(field).isNotNull();
            assertThat(field.getPropertyName()).isEqualTo("userName");
            assertThat(field.getFieldType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("不存在的字段应该返回 null")
        void shouldReturnNullForNonExistentField() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            DatabaseFieldMetadata field = metadata.getField("nonExistent");
            assertThat(field).isNull();
        }
    }

    @Nested
    @DisplayName("继承支持")
    class InheritanceTests {

        @Test
        @DisplayName("应该包含父类字段")
        void shouldIncludeParentFields() {
            ReflectiveEntityMetadata<ChildEntity> metadata = ReflectiveEntityMetadata.create(ChildEntity.class);
            List<FieldMetadata> fields = metadata.getFields();

            // 应该包含父类字段
            assertThat(fields.stream().anyMatch(f -> f.getPropertyName().equals("id"))).isTrue();
            assertThat(fields.stream().anyMatch(f -> f.getPropertyName().equals("createdAt"))).isTrue();

            // 应该包含子类字段
            assertThat(fields.stream().anyMatch(f -> f.getPropertyName().equals("childName"))).isTrue();
        }
    }

    @Nested
    @DisplayName("缓存机制")
    class CacheTests {

        @Test
        @DisplayName("相同实体类应该返回缓存的实例")
        void shouldReturnCachedInstance() {
            ReflectiveEntityMetadata<TestUser> metadata1 = ReflectiveEntityMetadata.create(TestUser.class);
            ReflectiveEntityMetadata<TestUser> metadata2 = ReflectiveEntityMetadata.create(TestUser.class);

            assertThat(metadata1).isSameAs(metadata2);
        }

        @Test
        @DisplayName("清除缓存后应该创建新实例")
        void shouldCreateNewInstanceAfterCacheClear() {
            ReflectiveEntityMetadata<TestUser> metadata1 = ReflectiveEntityMetadata.create(TestUser.class);
            ReflectiveEntityMetadata.clearCache();
            ReflectiveEntityMetadata<TestUser> metadata2 = ReflectiveEntityMetadata.create(TestUser.class);

            assertThat(metadata1).isNotSameAs(metadata2);
        }
    }

    @Nested
    @DisplayName("实体类信息")
    class EntityClassTests {

        @Test
        @DisplayName("应该返回正确的实体类")
        void shouldReturnEntityClass() {
            ReflectiveEntityMetadata<TestUser> metadata = ReflectiveEntityMetadata.create(TestUser.class);
            assertThat(metadata.getEntityClass()).isEqualTo(TestUser.class);
        }
    }

    // ==================== 测试实体类 ====================

    @Table(name = "sys_user")
    static class TestUser {
        @Id
        private Long id;

        @Column(name = "is_deleted")
        private Boolean deleted;

        private String userName;
        private String URL;
        private Integer status;

        public Long getId() { return id; }
        public Boolean getDeleted() { return deleted; }
        public String getUserName() { return userName; }
        public String getURL() { return URL; }
        public Integer getStatus() { return status; }
    }

    static class NoTableEntity {
        @Id
        private Long id;
        private String name;
    }

    @Table(name = "")
    static class EmptyTableNameEntity {
        @Id
        private Long id;
    }

    static class NoIdAnnotationEntity {
        private Long id;
        private String name;
    }

    static class WithStaticFieldEntity {
        @Id
        private Long id;
        private static final String STATIC_FIELD = "constant";
    }

    static class ParentEntity {
        @Id
        private Long id;
        private java.time.LocalDateTime createdAt;
    }

    static class ChildEntity extends ParentEntity {
        private String childName;
    }
}
