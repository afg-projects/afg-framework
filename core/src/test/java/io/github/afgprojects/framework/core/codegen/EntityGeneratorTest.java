package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * EntityGenerator 测试
 */
@DisplayName("EntityGenerator 测试")
class EntityGeneratorTest {

    private EntityGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new EntityGenerator();
    }

    @Nested
    @DisplayName("基本信息测试")
    class BasicInfoTests {

        @Test
        @DisplayName("应该返回正确的名称")
        void shouldReturnCorrectName() {
            assertThat(generator.getName()).isEqualTo("EntityGenerator");
        }

        @Test
        @DisplayName("应该返回正确的模板类型")
        void shouldReturnCorrectTemplateType() {
            assertThat(generator.getTemplateType()).isEqualTo("entity");
        }
    }

    @Nested
    @DisplayName("代码生成测试")
    class GenerateTests {

        @Test
        @DisplayName("应该生成基本的 Entity 类")
        void shouldGenerateBasicEntityClass() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("package com.example.entity;");
            assertThat(code).contains("public class User {");
            assertThat(code).contains("}");
        }

        @Test
        @DisplayName("应该生成带表名的 Entity")
        void shouldGenerateEntityWithTableName() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .tableName("t_user")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@TableName(\"t_user\")");
            assertThat(code).contains("import com.baomidou.mybatisplus.annotation.TableName");
        }

        @Test
        @DisplayName("应该生成带类注释的 Entity")
        void shouldGenerateEntityWithComment() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .classComment("用户实体")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("/**");
            assertThat(code).contains(" * 用户实体");
            assertThat(code).contains(" */");
        }

        @Test
        @DisplayName("应该生成带字段的 Entity")
        void shouldGenerateEntityWithFields() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("id")
                    .type("Long")
                    .comment("主键ID")
                    .primaryKey(true)
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .tableName("t_user")
                    .fields(List.of(field))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("private Long id;");
            assertThat(code).contains("主键ID");
            assertThat(code).contains("@TableId");
        }

        @Test
        @DisplayName("应该生成带列名的字段")
        void shouldGenerateFieldWithColumnName() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("userName")
                    .type("String")
                    .columnName("user_name")
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .tableName("t_user")
                    .fields(List.of(field))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@TableField(\"user_name\")");
        }

        @Test
        @DisplayName("应该生成 Getter 和 Setter 方法")
        void shouldGenerateGetterAndSetter() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("name")
                    .type("String")
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .fields(List.of(field))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("public String getName()");
            assertThat(code).contains("public void setName(String name)");
        }

        @Test
        @DisplayName("应该生成带父类的 Entity")
        void shouldGenerateEntityWithSuperClass() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .superClass("BaseEntity")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("public class User extends BaseEntity");
        }

        @Test
        @DisplayName("应该生成带接口的 Entity")
        void shouldGenerateEntityWithInterfaces() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .interfaces(List.of("Serializable", "Cloneable"))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("public class User implements Serializable, Cloneable");
        }

        @Test
        @DisplayName("应该添加日期类型的导入")
        void shouldAddDateTypeImports() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("createTime")
                    .type("LocalDateTime")
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .fields(List.of(field))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("import java.time.LocalDateTime");
        }

        @Test
        @DisplayName("应该添加 BigDecimal 类型的导入")
        void shouldAddBigDecimalImport() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("amount")
                    .type("BigDecimal")
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("Order")
                    .packageName("com.example.entity")
                    .fields(List.of(field))
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("import java.math.BigDecimal");
        }

        @Test
        @DisplayName("应该添加 Lombok 注解")
        void shouldAddLombokAnnotation() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .build();

            String code = generator.generate(context);

            assertThat(code).contains("@Data");
            assertThat(code).contains("import lombok.Data");
        }
    }
}