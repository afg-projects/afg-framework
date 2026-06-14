package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * MappingNode 纯单元测试
 *
 * <p>测试映射节点的字段映射和默认值逻辑：
 * 字段重命名、默认值填充、参数校验。
 * 不需要 Spring 上下文或外部依赖。
 */
@DisplayName("MappingNode")
class MappingNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            MappingNode node = new MappingNode("map-1");

            assertThat(node.getNodeId()).isEqualTo("map-1");
            assertThat(node.getType()).isEqualTo("mapping");
        }
    }

    @Nested
    @DisplayName("字段映射")
    class FieldMapping {

        @Test
        @DisplayName("应将源字段映射到目标字段名")
        void shouldMapSourceToTargetFields() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            Map<String, Object> input = Map.of(
                "firstName", "John",
                "lastName", "Doe"
            );

            NodeOutput output = node.execute(context, Map.of(
                "input", input,
                "mapping", Map.of("firstName", "givenName", "lastName", "familyName")
            ));

            assertThat(output.data())
                .containsEntry("givenName", "John")
                .containsEntry("familyName", "Doe")
                .doesNotContainKey("firstName")
                .doesNotContainKey("lastName");
        }

        @Test
        @DisplayName("源字段不存在时目标字段应不出现")
        void shouldNotIncludeTargetField_whenSourceMissing() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            Map<String, Object> input = Map.of("firstName", "John");

            NodeOutput output = node.execute(context, Map.of(
                "input", input,
                "mapping", Map.of("firstName", "givenName", "lastName", "familyName")
            ));

            assertThat(output.data())
                .containsEntry("givenName", "John")
                .doesNotContainKey("familyName");
        }

        @Test
        @DisplayName("源字段值为 null 时目标字段应不出现")
        void shouldNotIncludeTargetField_whenSourceValueIsNull() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            // Use HashMap to allow null values (Map.of does not allow nulls)
            Map<String, Object> input = new java.util.HashMap<>();
            input.put("firstName", "John");
            input.put("lastName", null);

            NodeOutput output = node.execute(context, Map.of(
                "input", input,
                "mapping", Map.of("firstName", "givenName", "lastName", "familyName")
            ));

            assertThat(output.data())
                .containsEntry("givenName", "John")
                .doesNotContainKey("familyName");
        }
    }

    @Nested
    @DisplayName("默认值填充")
    class DefaultValues {

        @Test
        @DisplayName("缺失字段应使用默认值")
        void shouldUseDefaultsForMissingFields() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            Map<String, Object> input = Map.of("name", "John");

            NodeOutput output = node.execute(context, Map.of(
                "input", input,
                "mapping", Map.of("name", "userName"),
                "defaults", Map.of("status", "active", "role", "user")
            ));

            assertThat(output.data())
                .containsEntry("userName", "John")
                .containsEntry("status", "active")
                .containsEntry("role", "user");
        }

        @Test
        @DisplayName("映射已提供的字段不应被默认值覆盖")
        void shouldNotOverrideMappedFieldsWithDefaults() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            Map<String, Object> input = Map.of("name", "John", "status", "premium");

            NodeOutput output = node.execute(context, Map.of(
                "input", input,
                "mapping", Map.of("name", "userName", "status", "accountStatus"),
                "defaults", Map.of("accountStatus", "active")
            ));

            assertThat(output.data()).containsEntry("accountStatus", "premium");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("缺少 input 参数时应返回错误输出")
        void shouldReturnErrorOutput_whenInputMissing() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "mapping", Map.of("a", "b")
            ));

            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("缺少 mapping 参数时应返回错误输出")
        void shouldReturnErrorOutput_whenMappingMissing() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "input", Map.of("key", "value")
            ));

            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("mapping 为空 map 时应返回错误输出")
        void shouldReturnErrorOutput_whenMappingEmpty() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "input", Map.of("key", "value"),
                "mapping", Map.of()
            ));

            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("null 参数应视为空 map 处理")
        void shouldHandleNullParams() {
            MappingNode node = new MappingNode("map-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, null);

            assertThat(output.data()).containsKey("error");
        }
    }
}
