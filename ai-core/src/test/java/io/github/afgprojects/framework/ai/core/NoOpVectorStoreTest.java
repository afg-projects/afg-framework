package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.NoOpVectorStore;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * NoOpVectorStore 纯单元测试
 */
@DisplayName("NoOpVectorStore")
class NoOpVectorStoreTest {

    private final VectorStore store = new NoOpVectorStore();

    @Nested
    @DisplayName("add + exists")
    class AddAndExists {

        @Test
        @DisplayName("添加文档后应存在")
        void shouldExistAfterAdd() {
            var doc = new Document("doc-1", "content", null, Map.of());
            store.add(doc);

            assertThat(store.exists("doc-1")).isTrue();
        }

        @Test
        @DisplayName("未添加的文档应不存在")
        void shouldNotExist_whenNotAdded() {
            assertThat(store.exists("nonexistent")).isFalse();
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("search 应始终返回空列表")
        void shouldReturnEmptyList_whenSearch() {
            store.add(new Document("doc-1", "content", null, Map.of()));

            var results = store.search("query", 5, 0.7);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("带 filter 的 search 应始终返回空列表")
        void shouldReturnEmptyList_whenSearchWithFilter() {
            store.add(new Document("doc-1", "content", null, Map.of()));

            var results = store.search("query", 5, 0.7, Map.of("key", "value"));

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("删除后应不存在")
        void shouldNotExistAfterDelete() {
            store.add(new Document("doc-1", "content", null, Map.of()));
            store.delete("doc-1");

            assertThat(store.exists("doc-1")).isFalse();
        }

        @Test
        @DisplayName("删除不存在的文档应不抛异常")
        void shouldNotThrow_whenDeletingNonexistent() {
            assertThatCode(() -> store.delete("nonexistent")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("addBatch + deleteBatch")
    class BatchOperations {

        @Test
        @DisplayName("addBatch 应添加所有文档")
        void shouldAddAllDocuments() {
            var docs = List.of(
                    new Document("doc-1", "content 1", null, Map.of()),
                    new Document("doc-2", "content 2", null, Map.of())
            );
            store.addBatch(docs);

            assertThat(store.exists("doc-1")).isTrue();
            assertThat(store.exists("doc-2")).isTrue();
        }

        @Test
        @DisplayName("deleteBatch 应删除所有文档")
        void shouldDeleteAllDocuments() {
            store.add(new Document("doc-1", "content 1", null, Map.of()));
            store.add(new Document("doc-2", "content 2", null, Map.of()));
            store.deleteBatch(List.of("doc-1", "doc-2"));

            assertThat(store.exists("doc-1")).isFalse();
            assertThat(store.exists("doc-2")).isFalse();
        }
    }
}
