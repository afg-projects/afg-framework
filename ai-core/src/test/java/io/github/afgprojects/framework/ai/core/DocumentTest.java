package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Document record 纯单元测试
 */
@DisplayName("Document")
class DocumentTest {

    @Nested
    @DisplayName("构造方法验证")
    class ConstructorValidation {

        @Test
        @DisplayName("空白 ID 应抛异常")
        void shouldThrow_whenBlankId() {
            assertThatThrownBy(() -> new Document("  ", "content", null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("空白 content 应抛异常")
        void shouldThrow_whenBlankContent() {
            assertThatThrownBy(() -> new Document("id", "  ", null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("content");
        }

        @Test
        @DisplayName("null metadata 应转为空 Map")
        void shouldConvertNullMetadataToEmptyMap() {
            var doc = new Document("id", "content", null, null);

            assertThat(doc.metadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("简化构造方法")
    class SimplifiedConstructor {

        @Test
        @DisplayName("of(content) 应自动生成 ID")
        void shouldAutoGenerateId() {
            var doc = Document.of("some content");

            assertThat(doc.id()).isNotBlank();
            assertThat(doc.content()).isEqualTo("some content");
            assertThat(doc.embedding()).isNull();
            assertThat(doc.metadata()).isEmpty();
        }

        @Test
        @DisplayName("of(content, metadata) 应自动生成 ID")
        void shouldAutoGenerateIdWithMetadata() {
            var doc = Document.of("content", Map.of("source", "web"));

            assertThat(doc.id()).isNotBlank();
            assertThat(doc.metadata()).containsEntry("source", "web");
        }
    }

    @Nested
    @DisplayName("with* 方法")
    class WithMethods {

        @Test
        @DisplayName("withId 应创建新 ID 的文档")
        void shouldCreateWithNewId() {
            var doc = new Document("old-id", "content", null, Map.of());
            var newDoc = doc.withId("new-id");

            assertThat(newDoc.id()).isEqualTo("new-id");
            assertThat(newDoc.content()).isEqualTo("content");
            assertThat(doc.id()).isEqualTo("old-id"); // 原文档不变
        }

        @Test
        @DisplayName("withContent 应创建新内容的文档")
        void shouldCreateWithNewContent() {
            var doc = new Document("id", "old content", null, Map.of());
            var newDoc = doc.withContent("new content");

            assertThat(newDoc.content()).isEqualTo("new content");
            assertThat(doc.content()).isEqualTo("old content");
        }

        @Test
        @DisplayName("withEmbedding 应创建新嵌入的文档")
        void shouldCreateWithNewEmbedding() {
            var doc = new Document("id", "content", null, Map.of());
            var embedding = List.of(0.1, 0.2, 0.3);
            var newDoc = doc.withEmbedding(embedding);

            assertThat(newDoc.embedding()).containsExactly(0.1, 0.2, 0.3);
            assertThat(doc.embedding()).isNull();
        }

        @Test
        @DisplayName("withMetadata 应添加元数据")
        void shouldCreateWithAdditionalMetadata() {
            var doc = new Document("id", "content", null, Map.of("a", "1"));
            var newDoc = doc.withMetadata("b", "2");

            assertThat(newDoc.metadata()).containsEntry("a", "1");
            assertThat(newDoc.metadata()).containsEntry("b", "2");
            assertThat(doc.metadata()).doesNotContainKey("b");
        }
    }

    @Nested
    @DisplayName("hasEmbedding")
    class HasEmbedding {

        @Test
        @DisplayName("无嵌入应返回 false")
        void shouldReturnFalse_whenNoEmbedding() {
            var doc = new Document("id", "content", null, Map.of());

            assertThat(doc.hasEmbedding()).isFalse();
        }

        @Test
        @DisplayName("空嵌入列表应返回 false")
        void shouldReturnFalse_whenEmptyEmbedding() {
            var doc = new Document("id", "content", List.of(), Map.of());

            assertThat(doc.hasEmbedding()).isFalse();
        }

        @Test
        @DisplayName("有嵌入应返回 true")
        void shouldReturnTrue_whenHasEmbedding() {
            var doc = new Document("id", "content", List.of(0.1, 0.2), Map.of());

            assertThat(doc.hasEmbedding()).isTrue();
        }
    }

    @Nested
    @DisplayName("getMetadata")
    class GetMetadata {

        @Test
        @DisplayName("应按键获取元数据值")
        void shouldGetMetadataByKey() {
            var doc = new Document("id", "content", null, Map.of("source", "web"));

            assertThat(doc.getMetadata("source")).isEqualTo("web");
            assertThat(doc.getMetadata("nonexistent")).isNull();
        }

        @Test
        @DisplayName("应按键获取元数据值带默认值")
        void shouldGetMetadataWithDefault() {
            var doc = new Document("id", "content", null, Map.of("source", "web"));

            assertThat(doc.getMetadata("source", "default")).isEqualTo("web");
            assertThat(doc.getMetadata("nonexistent", "default")).isEqualTo("default");
        }
    }
}
