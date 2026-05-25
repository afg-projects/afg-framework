package io.github.afgprojects.framework.ai.agent.tool;

import io.github.afgprojects.framework.ai.core.tool.Tool;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultToolRegistry} 单元测试
 */
@DisplayName("DefaultToolRegistry")
class DefaultToolRegistryTest {

    private DefaultToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultToolRegistry();
    }

    // ── 辅助方法 ───────────────────────────────────────────────────────────────

    private Tool<Map<String, Object>, String> createTool(String name, String description) {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return name;
            }

            @Override
            public @NonNull String description() {
                return description;
            }

            @Override
            public @NonNull String execute(Map<String, Object> input) {
                return name + "-result";
            }
        };
    }

    // ── 注册 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("应能注册工具并查询")
        void shouldRegisterAndFind() {
            Tool<?, ?> tool = createTool("weather", "Get weather info");
            registry.register(tool);

            Optional<Tool<?, ?>> found = registry.getTool("weather");
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("weather");
        }

        @Test
        @DisplayName("注册重复名称应覆盖旧工具")
        void shouldOverrideOnDuplicate() {
            registry.register(createTool("calc", "v1"));
            registry.register(createTool("calc", "v2"));

            assertThat(registry.getTool("calc").get().description()).isEqualTo("v2");
        }
    }

    // ── 查询 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("查询")
    class Query {

        @Test
        @DisplayName("查询不存在的工具返回空")
        void shouldReturnEmptyForMissing() {
            assertThat(registry.getTool("missing")).isEmpty();
        }

        @Test
        @DisplayName("exists 对已注册工具返回 true")
        void shouldReturnTrueForExisting() {
            registry.register(createTool("a", "desc"));
            assertThat(registry.exists("a")).isTrue();
        }

        @Test
        @DisplayName("exists 对未注册工具返回 false")
        void shouldReturnFalseForNonExisting() {
            assertThat(registry.exists("nope")).isFalse();
        }

        @Test
        @DisplayName("getAllTools 返回所有已注册工具")
        void shouldReturnAllTools() {
            registry.register(createTool("t1", "d1"));
            registry.register(createTool("t2", "d2"));

            Collection<Tool<?, ?>> all = registry.getAllTools();
            assertThat(all).hasSize(2);
            assertThat(all.stream().map(Tool::name)).containsExactlyInAnyOrder("t1", "t2");
        }
    }

    // ── 注销 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("应能注销已注册工具")
        void shouldUnregister() {
            registry.register(createTool("x", "desc"));
            registry.unregister("x");
            assertThat(registry.getTool("x")).isEmpty();
        }

        @Test
        @DisplayName("注销不存在的工具不抛异常")
        void shouldNotThrowOnMissing() {
            registry.unregister("ghost");
        }
    }

    // ── 清空 ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("clear 后所有工具都不存在")
        void shouldClearAll() {
            registry.register(createTool("a", "d"));
            registry.register(createTool("b", "d"));
            registry.clear();
            assertThat(registry.getAllTools()).isEmpty();
        }
    }
}