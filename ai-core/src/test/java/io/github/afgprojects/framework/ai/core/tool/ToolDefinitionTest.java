package io.github.afgprojects.framework.ai.core.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolDefinition 单元测试
 */
class ToolDefinitionTest {

    @Test
    @DisplayName("创建工具定义")
    void create_success() {
        ToolDefinition def = new ToolDefinition("test_tool", "A test tool", "{}");

        assertThat(def.name()).isEqualTo("test_tool");
        assertThat(def.description()).isEqualTo("A test tool");
        assertThat(def.inputSchema()).isEqualTo("{}");
    }

    @Test
    @DisplayName("创建带输入 Schema 的工具定义")
    void create_withSchema_success() {
        String schema = "{\"type\":\"object\",\"properties\":{\"input\":{\"type\":\"string\"}}}";
        ToolDefinition def = new ToolDefinition("tool", "Description", schema);

        assertThat(def.name()).isEqualTo("tool");
        assertThat(def.description()).isEqualTo("Description");
        assertThat(def.inputSchema()).isEqualTo(schema);
    }

    @Test
    @DisplayName("空 inputSchema 默认为空对象")
    void nullInputSchema_defaultsToEmptyObject() {
        ToolDefinition def = new ToolDefinition("tool", "Description", null);

        assertThat(def.inputSchema()).isEqualTo("{}");
    }

    @Test
    @DisplayName("空 description 默认为空字符串")
    void nullDescription_defaultsToEmptyString() {
        ToolDefinition def = new ToolDefinition("tool", null, null);

        assertThat(def.description()).isEqualTo("");
    }

    @Test
    @DisplayName("使用静态工厂方法创建")
    void staticFactory_success() {
        ToolDefinition def = ToolDefinition.of("tool", "Description");

        assertThat(def.name()).isEqualTo("tool");
        assertThat(def.description()).isEqualTo("Description");
        assertThat(def.inputSchema()).isEqualTo("{}");
    }
}