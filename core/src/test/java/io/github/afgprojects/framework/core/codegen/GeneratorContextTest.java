package io.github.afgprojects.framework.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GeneratorContext 测试
 */
@DisplayName("GeneratorContext 测试")
class GeneratorContextTest {

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该构建基本上下文")
        void shouldBuildBasicContext() {
            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .build();

            assertThat(context.getClassName()).isEqualTo("User");
            assertThat(context.getPackageName()).isEqualTo("com.example.entity");
        }

        @Test
        @DisplayName("应该构建完整上下文")
        void shouldBuildFullContext() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("id")
                    .type("Long")
                    .primaryKey(true)
                    .build();

            GeneratorContext.MethodDefinition method = GeneratorContext.MethodDefinition.builder()
                    .name("getId")
                    .returnType("Long")
                    .build();

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example.entity")
                    .tableName("t_user")
                    .classComment("用户实体")
                    .fields(List.of(field))
                    .methods(List.of(method))
                    .imports(List.of("java.util.List"))
                    .superClass("BaseEntity")
                    .interfaces(List.of("Serializable"))
                    .annotations(List.of("@Data"))
                    .build();

            assertThat(context.getClassName()).isEqualTo("User");
            assertThat(context.getTableName()).isEqualTo("t_user");
            assertThat(context.getClassComment()).isEqualTo("用户实体");
            assertThat(context.getFields()).hasSize(1);
            assertThat(context.getMethods()).hasSize(1);
            assertThat(context.getSuperClass()).isEqualTo("BaseEntity");
            assertThat(context.getInterfaces()).contains("Serializable");
        }

        @Test
        @DisplayName("应该支持额外属性")
        void shouldSupportExtraProperties() {
            Map<String, Object> extra = new HashMap<>();
            extra.put("custom", "value");

            GeneratorContext context = GeneratorContext.builder()
                    .className("User")
                    .packageName("com.example")
                    .extraProperties(extra)
                    .build();

            assertThat(context.getExtraProperties()).containsEntry("custom", "value");
        }
    }

    @Nested
    @DisplayName("FieldDefinition 测试")
    class FieldDefinitionTests {

        @Test
        @DisplayName("应该构建字段定义")
        void shouldBuildFieldDefinition() {
            GeneratorContext.FieldDefinition field = GeneratorContext.FieldDefinition.builder()
                    .name("userName")
                    .type("String")
                    .comment("用户名")
                    .primaryKey(false)
                    .required(true)
                    .defaultValue("guest")
                    .length(50)
                    .columnName("user_name")
                    .annotations(List.of("@NotBlank"))
                    .build();

            assertThat(field.getName()).isEqualTo("userName");
            assertThat(field.getType()).isEqualTo("String");
            assertThat(field.getComment()).isEqualTo("用户名");
            assertThat(field.isRequired()).isTrue();
            assertThat(field.getDefaultValue()).isEqualTo("guest");
            assertThat(field.getLength()).isEqualTo(50);
            assertThat(field.getColumnName()).isEqualTo("user_name");
            assertThat(field.getAnnotations()).contains("@NotBlank");
        }
    }

    @Nested
    @DisplayName("MethodDefinition 测试")
    class MethodDefinitionTests {

        @Test
        @DisplayName("应该构建方法定义")
        void shouldBuildMethodDefinition() {
            GeneratorContext.ParameterDefinition param = GeneratorContext.ParameterDefinition.builder()
                    .name("id")
                    .type("Long")
                    .build();

            GeneratorContext.MethodDefinition method = GeneratorContext.MethodDefinition.builder()
                    .name("findById")
                    .returnType("User")
                    .parameters(List.of(param))
                    .body("return repository.findById(id);")
                    .comment("根据ID查找用户")
                    .annotations(List.of("@Override"))
                    .build();

            assertThat(method.getName()).isEqualTo("findById");
            assertThat(method.getReturnType()).isEqualTo("User");
            assertThat(method.getParameters()).hasSize(1);
            assertThat(method.getBody()).isEqualTo("return repository.findById(id);");
            assertThat(method.getComment()).isEqualTo("根据ID查找用户");
            assertThat(method.getAnnotations()).contains("@Override");
        }
    }

    @Nested
    @DisplayName("ParameterDefinition 测试")
    class ParameterDefinitionTests {

        @Test
        @DisplayName("应该构建参数定义")
        void shouldBuildParameterDefinition() {
            GeneratorContext.ParameterDefinition param = GeneratorContext.ParameterDefinition.builder()
                    .name("userId")
                    .type("Long")
                    .build();

            assertThat(param.getName()).isEqualTo("userId");
            assertThat(param.getType()).isEqualTo("Long");
        }
    }
}
