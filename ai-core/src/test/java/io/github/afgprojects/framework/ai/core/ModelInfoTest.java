package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ModelInfo / DefaultModelInfo 纯单元测试
 */
@DisplayName("ModelInfo")
class ModelInfoTest {

    @Nested
    @DisplayName("DefaultModelInfo 构造")
    class DefaultModelInfoConstruction {

        @Test
        @DisplayName("of(name, type) 应创建最小模型信息")
        void shouldCreateMinimalModel_whenNameAndType() {
            var info = DefaultModelInfo.of("gpt-4", ModelType.CHAT);

            assertThat(info.name()).isEqualTo("gpt-4");
            assertThat(info.type()).isEqualTo(ModelType.CHAT);
            assertThat(info.provider()).isNull();
            assertThat(info.displayName()).isNull();
            assertThat(info.contextWindow()).isNull();
            assertThat(info.dimensions()).isNull();
            assertThat(info.maxOutputTokens()).isNull();
            assertThat(info.inputPricePer1kTokens()).isNull();
            assertThat(info.outputPricePer1kTokens()).isNull();
            assertThat(info.available()).isTrue();
            assertThat(info.capabilities()).isEmpty();
        }

        @Test
        @DisplayName("of(name, type, provider) 应创建带提供商的模型信息")
        void shouldCreateModelWithProvider() {
            var info = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "openai");

            assertThat(info.name()).isEqualTo("gpt-4");
            assertThat(info.type()).isEqualTo(ModelType.CHAT);
            assertThat(info.provider()).isEqualTo("openai");
        }

        @Test
        @DisplayName("完整构造方法应创建完整模型信息")
        void shouldCreateFullModel() {
            var info = new DefaultModelInfo(
                    "text-embedding-3-large",
                    ModelType.EMBEDDING,
                    "openai",
                    "Text Embedding 3 Large",
                    8192,
                    3072,
                    null,
                    0.00013,
                    null,
                    true,
                    Map.of("dimension_tuning", true)
            );

            assertThat(info.name()).isEqualTo("text-embedding-3-large");
            assertThat(info.type()).isEqualTo(ModelType.EMBEDDING);
            assertThat(info.provider()).isEqualTo("openai");
            assertThat(info.displayName()).isEqualTo("Text Embedding 3 Large");
            assertThat(info.contextWindow()).isEqualTo(8192);
            assertThat(info.dimensions()).isEqualTo(3072);
            assertThat(info.maxOutputTokens()).isNull();
            assertThat(info.inputPricePer1kTokens()).isEqualTo(0.00013);
            assertThat(info.available()).isTrue();
            assertThat(info.capabilities()).containsEntry("dimension_tuning", true);
        }

        @Test
        @DisplayName("null capabilities 应转为空 Map")
        void shouldConvertNullCapabilitiesToEmptyMap() {
            var info = new DefaultModelInfo(
                    "model", ModelType.CHAT, null, null, null, null, null, null, null, true, null
            );

            assertThat(info.capabilities()).isEmpty();
        }

        @Test
        @DisplayName("capabilities 应为不可变副本")
        void shouldCreateImmutableCopyOfCapabilities() {
            var original = Map.<String, Object>of("key", "value");
            var info = new DefaultModelInfo("model", ModelType.CHAT, null, null, null, null, null, null, null, true, original);

            assertThat(info.capabilities()).containsEntry("key", "value");
            assertThatThrownBy(() -> info.capabilities().put("new", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("ModelType 枚举")
    class ModelTypeTest {

        @Test
        @DisplayName("应有 5 种模型类型")
        void shouldHaveFiveModelTypes() {
            assertThat(ModelType.values()).hasSize(5);
            assertThat(ModelType.values())
                    .containsExactlyInAnyOrder(
                            ModelType.CHAT,
                            ModelType.EMBEDDING,
                            ModelType.RERANK,
                            ModelType.IMAGE,
                            ModelType.AUDIO
                    );
        }
    }

    @Nested
    @DisplayName("record 相等性")
    class RecordEquality {

        @Test
        @DisplayName("相同参数的 ModelInfo 应相等")
        void shouldBeEqual_whenSameParameters() {
            var info1 = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "openai");
            var info2 = DefaultModelInfo.of("gpt-4", ModelType.CHAT, "openai");

            assertThat(info1).isEqualTo(info2);
            assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
        }

        @Test
        @DisplayName("不同参数的 ModelInfo 应不等")
        void shouldNotBeEqual_whenDifferentParameters() {
            var info1 = DefaultModelInfo.of("gpt-4", ModelType.CHAT);
            var info2 = DefaultModelInfo.of("gpt-3.5", ModelType.CHAT);

            assertThat(info1).isNotEqualTo(info2);
        }
    }
}
