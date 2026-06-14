package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * FilterNode 纯单元测试
 *
 * <p>测试过滤节点的各种过滤操作：
 * eq、ne、contains、gt、lt、regex 操作符。
 * 使用真实数据，不使用 mock。
 */
@DisplayName("FilterNode")
class FilterNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            FilterNode node = new FilterNode("filter-1");

            assertThat(node.getNodeId()).isEqualTo("filter-1");
            assertThat(node.getType()).isEqualTo("filter");
        }
    }

    @Nested
    @DisplayName("eq 操作符")
    class EqOperator {

        @Test
        @DisplayName("应保留字段值等于指定值的项")
        void shouldKeepItems_whenFieldEqualsValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("status", "active", "name", "A"),
                Map.of("status", "inactive", "name", "B"),
                Map.of("status", "active", "name", "C")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "status",
                "operator", "eq",
                "value", "active"
            ));

            assertThat(output.data()).containsEntry("originalCount", 3);
            assertThat(output.data()).containsEntry("filteredCount", 2);
            @SuppressWarnings("unchecked")
            List<Object> filtered = (List<Object>) output.data().get("filteredItems");
            assertThat(filtered).hasSize(2);
        }
    }

    @Nested
    @DisplayName("ne 操作符")
    class NeOperator {

        @Test
        @DisplayName("应保留字段值不等于指定值的项")
        void shouldKeepItems_whenFieldNotEqualsValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("role", "admin", "name", "A"),
                Map.of("role", "user", "name", "B")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "role",
                "operator", "ne",
                "value", "admin"
            ));

            assertThat(output.data()).containsEntry("filteredCount", 1);
        }
    }

    @Nested
    @DisplayName("contains 操作符")
    class ContainsOperator {

        @Test
        @DisplayName("应保留字段值包含指定字符串的项")
        void shouldKeepItems_whenFieldContainsValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("email", "admin@example.com", "name", "A"),
                Map.of("email", "user@test.org", "name", "B")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "email",
                "operator", "contains",
                "value", "example.com"
            ));

            assertThat(output.data()).containsEntry("filteredCount", 1);
        }
    }

    @Nested
    @DisplayName("gt/lt 操作符")
    class ComparisonOperators {

        @Test
        @DisplayName("应保留字段值大于指定值的项")
        void shouldKeepItems_whenFieldGreaterThanValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("score", "90", "name", "A"),
                Map.of("score", "50", "name", "B"),
                Map.of("score", "80", "name", "C")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "score",
                "operator", "gt",
                "value", "60"
            ));

            assertThat(output.data()).containsEntry("filteredCount", 2);
        }

        @Test
        @DisplayName("应保留字段值小于指定值的项")
        void shouldKeepItems_whenFieldLessThanValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("score", "90", "name", "A"),
                Map.of("score", "50", "name", "B"),
                Map.of("score", "80", "name", "C")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "score",
                "operator", "lt",
                "value", "60"
            ));

            assertThat(output.data()).containsEntry("filteredCount", 1);
        }
    }

    @Nested
    @DisplayName("regex 操作符")
    class RegexOperator {

        @Test
        @DisplayName("应保留字段值匹配正则表达式的项")
        void shouldKeepItems_whenFieldMatchesRegex() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("code", "ABC-123", "name", "A"),
                Map.of("code", "XYZ", "name", "B"),
                Map.of("code", "DEF-456", "name", "C")
            );

            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "code",
                "operator", "regex",
                "value", "[A-Z]+-[0-9]+"
            ));

            assertThat(output.data()).containsEntry("filteredCount", 2);
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("缺少 items 参数时应返回错误输出")
        void shouldReturnErrorOutput_whenItemsMissing() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("null 参数应视为空 map 处理")
        void shouldHandleNullParams() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, null);

            assertThat(output.data()).containsKey("error");
        }
    }

    @Nested
    @DisplayName("空值处理")
    class NullValueHandling {

        @Test
        @DisplayName("eq 操作符 value=null 时应保留字段为 null 的项")
        void shouldKeepItemsWithNullField_whenEqNullValue() {
            FilterNode node = new FilterNode("filter-1");
            ExecutionContext context = createContext();

            List<Map<String, Object>> items = List.of(
                Map.of("name", "A"),
                Map.of("name", "B")
 );

            // value=null → eq means field IS NULL
            NodeOutput output = node.execute(context, Map.of(
                "items", items,
                "field", "missingField",
                "operator", "eq"
                // no "value" key → value is null
            ));

            assertThat(output.data()).containsEntry("filteredCount", 2);
        }
    }
}
