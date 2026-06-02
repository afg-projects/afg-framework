package io.github.afgprojects.framework.data.jdbc.mapper;

import io.github.afgprojects.framework.data.core.mapper.MappingField;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * DtoMapper 改进测试
 * <p>
 * 验证 DtoMapper 的改进功能：
 * - POJO 继承字段映射
 * - Record @MappingField 注解
 * - 列名大小写不敏感
 */
@DisplayName("DtoMapper 改进测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DtoMapperImprovementTest {

    private TypeHandlerRegistry typeHandlerRegistry;

    @BeforeEach
    void setUp() {
        typeHandlerRegistry = TypeHandlerRegistry.defaultRegistry();
    }

    // ==================== POJO 继承字段映射 ====================

    @Nested
    @DisplayName("POJO 继承字段映射测试")
    class InheritanceMappingTests {

        @Data
        @NoArgsConstructor
        static class BaseDto {
            private Long id;
            private String name;
        }

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        static class ChildDto extends BaseDto {
            private String email;
        }

        @Test
        @DisplayName("子类应映射父类字段")
        void shouldMapInheritedFields() throws Exception {
            // Given
            DtoMapper<ChildDto> mapper = new DtoMapper<>(ChildDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"id", "name", "email"},
                    new Object[]{1L, "Alice", "alice@example.com"}
            );

            // When
            ChildDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Alice");
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("子类字段为 null 时不应影响父类字段映射")
        void shouldMapParentFieldsEvenWhenChildFieldIsNull() throws Exception {
            // Given
            DtoMapper<ChildDto> mapper = new DtoMapper<>(ChildDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"id", "name", "email"},
                    new Object[]{2L, "Bob", null}
            );

            // When
            ChildDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("Bob");
            assertThat(result.getEmail()).isNull();
        }

        @Test
        @DisplayName("父类字段为 null 时不应影响子类字段映射")
        void shouldMapChildFieldEvenWhenParentFieldIsNull() throws Exception {
            // Given
            DtoMapper<ChildDto> mapper = new DtoMapper<>(ChildDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"id", "name", "email"},
                    new Object[]{null, null, "charlie@example.com"}
            );

            // When
            ChildDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isNull();
            assertThat(result.getName()).isNull();
            assertThat(result.getEmail()).isEqualTo("charlie@example.com");
        }

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        static class GrandChildDto extends ChildDto {
            private Integer age;
        }

        @Test
        @DisplayName("多层继承应映射所有层级字段")
        void shouldMapAllLevelsOfInheritance() throws Exception {
            // Given
            DtoMapper<GrandChildDto> mapper = new DtoMapper<>(GrandChildDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"id", "name", "email", "age"},
                    new Object[]{1L, "Dave", "dave@example.com", 30}
            );

            // When
            GrandChildDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Dave");
            assertThat(result.getEmail()).isEqualTo("dave@example.com");
            assertThat(result.getAge()).isEqualTo(30);
        }
    }

    // ==================== Record @MappingField 注解 ====================

    @Nested
    @DisplayName("Record @MappingField 注解测试")
    class RecordMappingFieldTests {

        record UserDto(@MappingField(column = "user_name") String name, Integer age) {
        }

        @Test
        @DisplayName("@MappingField column 应指定列名映射")
        void shouldMapWithColumnAnnotation() throws Exception {
            // Given
            DtoMapper<UserDto> mapper = new DtoMapper<>(UserDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_name", "age"},
                    new Object[]{"Alice", 25}
            );

            // When
            UserDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.name()).isEqualTo("Alice");
            assertThat(result.age()).isEqualTo(25);
        }

        record SourceDto(@MappingField(source = "user_name") String name, String email) {
        }

        @Test
        @DisplayName("@MappingField source 应指定列名映射")
        void shouldMapWithSourceAnnotation() throws Exception {
            // Given
            DtoMapper<SourceDto> mapper = new DtoMapper<>(SourceDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_name", "email"},
                    new Object[]{"Bob", "bob@example.com"}
            );

            // When
            SourceDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.name()).isEqualTo("Bob");
            assertThat(result.email()).isEqualTo("bob@example.com");
        }

        record MixedDto(@MappingField(column = "display_name") String name, Integer age, String email) {
        }

        @Test
        @DisplayName("@MappingField 和自动映射应混合使用")
        void shouldMixAnnotationAndAutoMapping() throws Exception {
            // Given
            DtoMapper<MixedDto> mapper = new DtoMapper<>(MixedDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"display_name", "age", "email"},
                    new Object[]{"Charlie", 28, "charlie@example.com"}
            );

            // When
            MixedDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.name()).isEqualTo("Charlie");
            assertThat(result.age()).isEqualTo(28);
            assertThat(result.email()).isEqualTo("charlie@example.com");
        }

        record EmptyRecord() {
        }

        @Test
        @DisplayName("空 Record 应成功创建实例")
        void shouldCreateEmptyRecord() throws Exception {
            // Given
            DtoMapper<EmptyRecord> mapper = new DtoMapper<>(EmptyRecord.class, typeHandlerRegistry);
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(0);

            // When
            EmptyRecord result = mapper.map(rs, 0);

            // Then
            assertThat(result).isNotNull();
        }

        record SnakeCaseDto(String userName, Integer userAge) {
        }

        @Test
        @DisplayName("Record 字段应支持 snake_case 自动映射")
        void shouldAutoMapSnakeCaseForRecord() throws Exception {
            // Given
            DtoMapper<SnakeCaseDto> mapper = new DtoMapper<>(SnakeCaseDto.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_name", "user_age"},
                    new Object[]{"Dave", 35}
            );

            // When
            SnakeCaseDto result = mapper.map(rs, 0);

            // Then
            assertThat(result.userName()).isEqualTo("Dave");
            assertThat(result.userAge()).isEqualTo(35);
        }
    }

    // ==================== 列名大小写不敏感 ====================

    @Nested
    @DisplayName("列名大小写不敏感测试")
    class CaseInsensitiveTests {

        @Data
        @NoArgsConstructor
        static class UserPojo {
            private Long id;
            private String name;
            private String email;
        }

        @Test
        @DisplayName("Oracle 风格大写列名应正确映射")
        void shouldMapWithUppercaseColumnNames() throws Exception {
            // Given: 模拟 Oracle 返回大写列名
            DtoMapper<UserPojo> mapper = new DtoMapper<>(UserPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"ID", "NAME", "EMAIL"},
                    new Object[]{1L, "Alice", "alice@example.com"}
            );

            // When
            UserPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Alice");
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("PostgreSQL 风格小写列名应正确映射")
        void shouldMapWithLowercaseColumnNames() throws Exception {
            // Given: PostgreSQL 默认返回小写列名
            DtoMapper<UserPojo> mapper = new DtoMapper<>(UserPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"id", "name", "email"},
                    new Object[]{2L, "Bob", "bob@example.com"}
            );

            // When
            UserPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("Bob");
            assertThat(result.getEmail()).isEqualTo("bob@example.com");
        }

        @Data
        @NoArgsConstructor
        static class SnakeCasePojo {
            private Long id;
            private String userName;
            private String emailAddress;
        }

        @Test
        @DisplayName("大写 snake_case 列名应映射到 camelCase 字段")
        void shouldMapUppercaseSnakeCaseToCamelCase() throws Exception {
            // Given: 模拟 Oracle 返回大写 snake_case 列名
            DtoMapper<SnakeCasePojo> mapper = new DtoMapper<>(SnakeCasePojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"ID", "USER_NAME", "EMAIL_ADDRESS"},
                    new Object[]{3L, "Charlie", "charlie@example.com"}
            );

            // When
            SnakeCasePojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getUserName()).isEqualTo("Charlie");
            assertThat(result.getEmailAddress()).isEqualTo("charlie@example.com");
        }

        @Test
        @DisplayName("混合大小写列名应正确映射")
        void shouldMapWithMixedCaseColumnNames() throws Exception {
            // Given: MySQL 可能保留原始大小写
            DtoMapper<UserPojo> mapper = new DtoMapper<>(UserPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"Id", "Name", "Email"},
                    new Object[]{4L, "Dave", "dave@example.com"}
            );

            // When
            UserPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(4L);
            assertThat(result.getName()).isEqualTo("Dave");
            assertThat(result.getEmail()).isEqualTo("dave@example.com");
        }
    }

    // ==================== @MappingField 与 POJO ====================

    @Nested
    @DisplayName("@MappingField POJO 注解测试")
    class PojoMappingFieldTests {

        @Data
        @NoArgsConstructor
        static class AnnotatedPojo {
            @MappingField(column = "user_name")
            private String displayName;

            private Integer age;
        }

        @Test
        @DisplayName("POJO @MappingField column 应指定列名映射")
        void shouldMapPojoWithColumnAnnotation() throws Exception {
            // Given
            DtoMapper<AnnotatedPojo> mapper = new DtoMapper<>(AnnotatedPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_name", "age"},
                    new Object[]{"Eve", 22}
            );

            // When
            AnnotatedPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getDisplayName()).isEqualTo("Eve");
            assertThat(result.getAge()).isEqualTo(22);
        }

        @Data
        @NoArgsConstructor
        static class SourceAnnotatedPojo {
            @MappingField(source = "user_name")
            private String name;

            private String email;
        }

        @Test
        @DisplayName("POJO @MappingField source 应指定列名映射")
        void shouldMapPojoWithSourceAnnotation() throws Exception {
            // Given
            DtoMapper<SourceAnnotatedPojo> mapper = new DtoMapper<>(SourceAnnotatedPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_name", "email"},
                    new Object[]{"Frank", "frank@example.com"}
            );

            // When
            SourceAnnotatedPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getName()).isEqualTo("Frank");
            assertThat(result.getEmail()).isEqualTo("frank@example.com");
        }

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = false)
        static class InheritedAnnotatedPojo extends BaseAnnotatedDto {
            @MappingField(column = "contact_email")
            private String email;
        }

        @Data
        @NoArgsConstructor
        static class BaseAnnotatedDto {
            @MappingField(column = "user_id")
            private Long id;

            private String name;
        }

        @Test
        @DisplayName("继承 + @MappingField 注解应同时生效")
        void shouldMapInheritedFieldsWithAnnotation() throws Exception {
            // Given
            DtoMapper<InheritedAnnotatedPojo> mapper = new DtoMapper<>(InheritedAnnotatedPojo.class, typeHandlerRegistry);
            ResultSet rs = mockResultSet(
                    new String[]{"user_id", "name", "contact_email"},
                    new Object[]{100L, "Grace", "grace@example.com"}
            );

            // When
            InheritedAnnotatedPojo result = mapper.map(rs, 0);

            // Then
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("Grace");
            assertThat(result.getEmail()).isEqualTo("grace@example.com");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 mock ResultSet，模拟指定列名和值
     *
     * @param columnNames 列名数组
     * @param values      对应列的值数组
     * @return mock ResultSet
     */
    private ResultSet mockResultSet(String[] columnNames, Object[] values) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columnNames.length);

        for (int i = 0; i < columnNames.length; i++) {
            when(metaData.getColumnLabel(i + 1)).thenReturn(columnNames[i]);
        }

        for (int i = 0; i < values.length; i++) {
            when(rs.getObject(i + 1)).thenReturn(values[i]);
        }

        return rs;
    }
}
