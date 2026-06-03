package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import io.github.afgprojects.framework.ai.core.model.DefaultModelRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultModelRegistry 纯单元测试
 */
@DisplayName("DefaultModelRegistry")
class DefaultModelRegistryTest {

    private final ModelRegistry registry = new DefaultModelRegistry();

    @Nested
    @DisplayName("registerModel + getModel")
    class RegisterAndGet {

        @Test
        @DisplayName("应注册并获取模型")
        void shouldRegisterAndGetModel() {
            var info = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "openai");
            registry.registerModel("gpt-4", info);

            var result = registry.getModel("gpt-4");

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("未注册的模型应返回 empty")
        void shouldReturnEmpty_whenNotRegistered() {
            var result = registry.getModel("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("注册相同名称应覆盖")
        void shouldOverride_whenSameName() {
            var info1 = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "openai");
            var info2 = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "azure");
            registry.registerModel("gpt-4", info1);
            registry.registerModel("gpt-4", info2);

            var result = registry.getModel("gpt-4");

            assertThat(result).isPresent();
            assertThat(result.get().provider()).isEqualTo("azure");
        }
    }

    @Nested
    @DisplayName("listModels")
    class ListModels {

        @Test
        @DisplayName("空注册表应返回空列表")
        void shouldReturnEmptyList_whenNoModels() {
            assertThat(registry.listModels()).isEmpty();
        }

        @Test
        @DisplayName("应列出所有模型")
        void shouldListAllModels() {
            registry.registerModel("gpt-4", DefaultModelInfo.of("gpt-4", ModelType.CHAT));
            registry.registerModel("text-embedding-3", DefaultModelInfo.of("text-embedding-3", ModelType.EMBEDDING));

            assertThat(registry.listModels()).hasSize(2);
        }

        @Test
        @DisplayName("应按类型过滤模型")
        void shouldFilterByType() {
            registry.registerModel("gpt-4", DefaultModelInfo.of("gpt-4", ModelType.CHAT));
            registry.registerModel("gpt-3.5", DefaultModelInfo.of("gpt-3.5", ModelType.CHAT));
            registry.registerModel("text-embedding-3", DefaultModelInfo.of("text-embedding-3", ModelType.EMBEDDING));

            assertThat(registry.listModels(ModelType.CHAT)).hasSize(2);
            assertThat(registry.listModels(ModelType.EMBEDDING)).hasSize(1);
            assertThat(registry.listModels(ModelType.RERANK)).isEmpty();
        }
    }

    @Nested
    @DisplayName("setDefault + getDefault")
    class SetAndGetDefault {

        @Test
        @DisplayName("应设置并获取默认模型")
        void shouldSetAndGetDefault() {
            var info = DefaultModelInfo.of("gpt-4", ModelType.CHAT);
            registry.registerModel("gpt-4", info);
            registry.setDefault("gpt-4", ModelType.CHAT);

            var defaultModel = registry.getDefault(ModelType.CHAT);

            assertThat(defaultModel).isPresent();
            assertThat(defaultModel.get().name()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("未设置默认时应返回该类型第一个模型")
        void shouldReturnFirstModelOfSameType_whenNoDefaultSet() {
            var info = DefaultModelInfo.of("gpt-4", ModelType.CHAT);
            registry.registerModel("gpt-4", info);

            var defaultModel = registry.getDefault(ModelType.CHAT);

            assertThat(defaultModel).isPresent();
            assertThat(defaultModel.get().name()).isEqualTo("gpt-4");
        }

        @Test
        @DisplayName("设置未注册的模型为默认应抛异常")
        void shouldThrow_whenSettingUnregisteredModelAsDefault() {
            assertThatThrownBy(() -> registry.setDefault("nonexistent", ModelType.CHAT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not registered");
        }

        @Test
        @DisplayName("无模型时应返回 empty")
        void shouldReturnEmpty_whenNoModelsOfSameType() {
            assertThat(registry.getDefault(ModelType.CHAT)).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeModel")
    class RemoveModel {

        @Test
        @DisplayName("应移除已注册的模型")
        void shouldRemoveRegisteredModel() {
            registry.registerModel("gpt-4", DefaultModelInfo.of("gpt-4", ModelType.CHAT));
            registry.removeModel("gpt-4");

            assertThat(registry.getModel("gpt-4")).isEmpty();
            assertThat(registry.listModels()).isEmpty();
        }

        @Test
        @DisplayName("移除默认模型时应同时清除默认映射")
        void shouldClearDefaultMapping_whenRemovingDefaultModel() {
            registry.registerModel("gpt-4", DefaultModelInfo.of("gpt-4", ModelType.CHAT));
            registry.setDefault("gpt-4", ModelType.CHAT);
            registry.removeModel("gpt-4");

            assertThat(registry.getModel("gpt-4")).isEmpty();
        }

        @Test
        @DisplayName("移除不存在的模型应不抛异常")
        void shouldNotThrow_whenRemovingNonexistentModel() {
            assertThatCode(() -> registry.removeModel("nonexistent")).doesNotThrowAnyException();
        }
    }
}
