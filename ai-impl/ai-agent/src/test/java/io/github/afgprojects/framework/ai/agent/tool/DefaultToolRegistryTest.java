package io.github.afgprojects.framework.ai.agent.tool;

import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultToolRegistry 单元测试
 */
class DefaultToolRegistryTest {

    private DefaultToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultToolRegistry();
    }

    @Test
    @DisplayName("注册工具成功")
    void register_success() {
        Tool<String, String> tool = createTestTool("test_tool", "Test tool");

        registry.register(tool);

        assertThat(registry.exists("test_tool")).isTrue();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("注册重复工具抛出异常")
    void register_duplicate_throwsException() {
        Tool<String, String> tool = createTestTool("test_tool", "Test tool");
        registry.register(tool);

        assertThatThrownBy(() -> registry.register(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tool already registered");
    }

    @Test
    @DisplayName("registerOrReplace 替换已存在的工具")
    void registerOrReplace_replacesExisting() {
        Tool<String, String> tool1 = createTestTool("test_tool", "Description 1");
        Tool<String, String> tool2 = createTestTool("test_tool", "Description 2");

        registry.register(tool1);
        registry.registerOrReplace(tool2);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.getTool("test_tool"))
                .isPresent()
                .get()
                .extracting(Tool::description)
                .isEqualTo("Description 2");
    }

    @Test
    @DisplayName("获取不存在的工具返回空")
    void getTool_notFound_returnsEmpty() {
        assertThat(registry.getTool("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("注销工具成功")
    void unregister_success() {
        Tool<String, String> tool = createTestTool("test_tool", "Test tool");
        registry.register(tool);

        boolean result = registry.unregister("test_tool");

        assertThat(result).isTrue();
        assertThat(registry.exists("test_tool")).isFalse();
    }

    @Test
    @DisplayName("注销不存在的工具返回 false")
    void unregister_notFound_returnsFalse() {
        boolean result = registry.unregister("nonexistent");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("获取所有工具定义")
    void getAllToolDefinitions() {
        Tool<String, String> tool1 = createTestTool("tool1", "Tool 1");
        Tool<String, String> tool2 = createTestTool("tool2", "Tool 2");

        registry.register(tool1);
        registry.register(tool2);

        var definitions = registry.getAllToolDefinitions();

        assertThat(definitions).hasSize(2);
        assertThat(definitions)
                .extracting(ToolDefinition::name)
                .containsExactlyInAnyOrder("tool1", "tool2");
    }

    @Test
    @DisplayName("清空注册表")
    void clear() {
        registry.register(createTestTool("tool1", "Tool 1"));
        registry.register(createTestTool("tool2", "Tool 2"));

        registry.clear();

        assertThat(registry.size()).isZero();
        assertThat(registry.size()).isZero();
    }

    @Test
    @DisplayName("带初始工具创建注册表")
    void createWithInitialTools() {
        Tool<?, ?> tool1 = createTestTool("tool1", "Tool 1");
        Tool<?, ?> tool2 = createTestTool("tool2", "Tool 2");

        Map<String, Tool<?, ?>> initialTools = new java.util.HashMap<>();
        initialTools.put("tool1", tool1);
        initialTools.put("tool2", tool2);

        var registryWithTools = new DefaultToolRegistry(initialTools);

        assertThat(registryWithTools.size()).isEqualTo(2);
    }

    private Tool<String, String> createTestTool(String name, String description) {
        return new Tool<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String execute(String input) {
                return "result: " + input;
            }
        };
    }
}
