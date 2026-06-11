package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.knowledge.CreateKnowledgeBaseRequest;
import io.github.afgprojects.framework.ai.core.dto.knowledge.KnowledgeSearchRequest;
import io.github.afgprojects.framework.ai.core.dto.knowledge.UpdateKnowledgeBaseRequest;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentChunkEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.KnowledgeBaseEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiKnowledgeController 集成测试
 *
 * <p>测试知识库、文档、分块的 CRUD 和搜索接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiKnowledgeControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Knowledge Base CRUD ====================

    @Test
    void shouldCreateKnowledgeBase_whenPostValidRequest() {
        // Arrange
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest();
        request.setName("test-kb-" + UUID.randomUUID());
        request.setDescription("Test knowledge base description");

        // Act
        KnowledgeBaseEntity created = restClient().post()
            .uri("/knowledge/bases")
            .body(request)
            .retrieve()
            .body(KnowledgeBaseEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(request.getName());
        assertThat(created.getDescription()).isEqualTo(request.getDescription());
        assertThat(created.getDeleted()).isFalse();

        // Cleanup
        dataManager.deleteById(KnowledgeBaseEntity.class, created.getId());
    }

    @Test
    void shouldListKnowledgeBases_whenGetAll() {
        // Arrange - create 2 knowledge bases via DataManager
        String prefix = "list-kb-" + UUID.randomUUID();

        KnowledgeBaseEntity kb1 = new KnowledgeBaseEntity();
        kb1.setName(prefix + "-1");
        kb1 = dataManager.save(KnowledgeBaseEntity.class, kb1);

        KnowledgeBaseEntity kb2 = new KnowledgeBaseEntity();
        kb2.setName(prefix + "-2");
        kb2 = dataManager.save(KnowledgeBaseEntity.class, kb2);

        // Act
        List<KnowledgeBaseEntity> bases = restClient().get()
            .uri("/knowledge/bases")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(bases).isNotNull();
        assertThat(bases.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(KnowledgeBaseEntity.class, kb1.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb2.getId());
    }

    @Test
    void shouldReturnKnowledgeBase_whenGetById() {
        // Arrange - create KB via DataManager
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("get-kb-" + UUID.randomUUID());
        kb.setDescription("Get test KB");
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        // Act
        KnowledgeBaseEntity found = restClient().get()
            .uri("/knowledge/bases/{id}", kb.getId())
            .retrieve()
            .body(KnowledgeBaseEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(kb.getId());
        assertThat(found.getName()).isEqualTo(kb.getName());
        assertThat(found.getDescription()).isEqualTo("Get test KB");

        // Cleanup
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    @Test
    void shouldUpdateKnowledgeBase_whenPutValidRequest() {
        // Arrange - create KB
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("update-kb-" + UUID.randomUUID());
        kb.setDescription("Original description");
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        UpdateKnowledgeBaseRequest request = new UpdateKnowledgeBaseRequest();
        request.setName("updated-kb-" + UUID.randomUUID());
        request.setDescription("Updated description");

        // Act
        KnowledgeBaseEntity updated = restClient().put()
            .uri("/knowledge/bases/{id}", kb.getId())
            .body(request)
            .retrieve()
            .body(KnowledgeBaseEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(kb.getId());
        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getDescription()).isEqualTo("Updated description");

        // Cleanup
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    @Test
    void shouldSoftDeleteKnowledgeBase_whenDeleteById() {
        // Arrange - create KB
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("delete-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/knowledge/bases/{id}", kb.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedKbId = kb.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/knowledge/bases/{id}", deletedKbId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Document Management ====================

    @Test
    void shouldUploadDocument_whenPostMultipart() {
        assumeOllamaAvailable();

        // Arrange - create KB first
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("upload-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new ByteArrayResource("test content for knowledge base".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        });
        formData.add("title", "Test Document");

        // Act
        DocumentEntity doc = restClient().post()
            .uri("/knowledge/bases/{id}/documents", kb.getId())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(formData)
            .retrieve()
            .body(DocumentEntity.class);

        // Assert
        assertThat(doc).isNotNull();
        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getKnowledgeBaseId()).isEqualTo(kb.getId());
        assertThat(doc.getTitle()).isEqualTo("Test Document");
        assertThat(doc.getStatus()).isNotNull();
        assertThat(doc.getFileType()).isEqualTo("txt");

        // Cleanup - delete chunks, then document, then KB
        List<DocumentChunkEntity> chunks = dataManager.entity(DocumentChunkEntity.class)
            .query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(DocumentChunkEntity.class)
                .eq(DocumentChunkEntity::getDocumentId, doc.getId())
                .build())
            .list();
        for (DocumentChunkEntity c : chunks) {
            dataManager.deleteById(DocumentChunkEntity.class, c.getId());
        }
        dataManager.deleteById(DocumentEntity.class, doc.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    @Test
    void shouldListDocuments_whenGetByKnowledgeBase() {
        // Arrange - create KB + documents via DataManager
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("doc-list-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        DocumentEntity doc1 = new DocumentEntity();
        doc1.setKnowledgeBaseId(kb.getId());
        doc1.setTitle("Document 1");
        doc1.setStatus("COMPLETED");
        doc1 = dataManager.save(DocumentEntity.class, doc1);

        DocumentEntity doc2 = new DocumentEntity();
        doc2.setKnowledgeBaseId(kb.getId());
        doc2.setTitle("Document 2");
        doc2.setStatus("COMPLETED");
        doc2 = dataManager.save(DocumentEntity.class, doc2);

        // Act
        List<DocumentEntity> docs = restClient().get()
            .uri("/knowledge/bases/{id}/documents", kb.getId())
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(docs).isNotNull();
        assertThat(docs.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(DocumentEntity.class, doc1.getId());
        dataManager.deleteById(DocumentEntity.class, doc2.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    @Test
    void shouldReturnDocument_whenGetById() {
        // Arrange - create KB + document via DataManager
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("doc-get-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        DocumentEntity doc = new DocumentEntity();
        doc.setKnowledgeBaseId(kb.getId());
        doc.setTitle("Get Test Doc");
        doc.setStatus("COMPLETED");
        doc = dataManager.save(DocumentEntity.class, doc);

        // Act
        DocumentEntity found = restClient().get()
            .uri("/knowledge/bases/{id}/documents/{docId}", kb.getId(), doc.getId())
            .retrieve()
            .body(DocumentEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(doc.getId());
        assertThat(found.getTitle()).isEqualTo("Get Test Doc");
        assertThat(found.getKnowledgeBaseId()).isEqualTo(kb.getId());

        // Cleanup
        dataManager.deleteById(DocumentEntity.class, doc.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    @Test
    void shouldSoftDeleteDocument_whenDeleteById() {
        // Arrange - create KB + document via DataManager
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("doc-delete-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        DocumentEntity doc = new DocumentEntity();
        doc.setKnowledgeBaseId(kb.getId());
        doc.setTitle("Delete Test Doc");
        doc.setStatus("COMPLETED");
        doc = dataManager.save(DocumentEntity.class, doc);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/knowledge/bases/{id}/documents/{docId}", kb.getId(), doc.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedKbId2 = kb.getId();
        Long deletedDocId = doc.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/knowledge/bases/{id}/documents/{docId}", deletedKbId2, deletedDocId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);

        // Cleanup
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    // ==================== Chunks ====================

    @Test
    void shouldListChunks_whenGetByDocument() {
        // Arrange - create KB + document + chunk via DataManager
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("chunk-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        DocumentEntity doc = new DocumentEntity();
        doc.setKnowledgeBaseId(kb.getId());
        doc.setTitle("Chunk Test Doc");
        doc.setStatus("COMPLETED");
        doc.setChunkCount(1);
        doc = dataManager.save(DocumentEntity.class, doc);

        DocumentChunkEntity chunk = new DocumentChunkEntity();
        chunk.setDocumentId(doc.getId());
        chunk.setKnowledgeBaseId(kb.getId());
        chunk.setContent("Test chunk content for knowledge base");
        chunk.setChunkIndex(0);
        chunk = dataManager.save(DocumentChunkEntity.class, chunk);

        // Act
        List<DocumentChunkEntity> chunks = restClient().get()
            .uri("/knowledge/bases/{id}/documents/{docId}/chunks", kb.getId(), doc.getId())
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(chunks).isNotNull();
        assertThat(chunks.size()).isGreaterThanOrEqualTo(1);

        // Cleanup
        dataManager.deleteById(DocumentChunkEntity.class, chunk.getId());
        dataManager.deleteById(DocumentEntity.class, doc.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }

    // ==================== Search ====================

    @Test
    void shouldSearchKnowledge_whenPostSearchRequest() {
        assumeOllamaAvailable();

        // Arrange - create KB and upload a document first
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName("search-kb-" + UUID.randomUUID());
        kb = dataManager.save(KnowledgeBaseEntity.class, kb);

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new ByteArrayResource("Machine learning is a subset of artificial intelligence".getBytes()) {
            @Override
            public String getFilename() {
                return "search-test.txt";
            }
        });
        formData.add("title", "ML Intro");

        DocumentEntity doc = restClient().post()
            .uri("/knowledge/bases/{id}/documents", kb.getId())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(formData)
            .retrieve()
            .body(DocumentEntity.class);

        assertThat(doc).isNotNull();
        assertThat(doc.getStatus()).isEqualTo("COMPLETED");

        // Act - search
        KnowledgeSearchRequest searchRequest = new KnowledgeSearchRequest();
        searchRequest.setKnowledgeBaseIds(List.of(String.valueOf(kb.getId())));
        searchRequest.setQuery("What is machine learning?");
        searchRequest.setTopK(3);

        List<Map> results = restClient().post()
            .uri("/knowledge/search")
            .body(searchRequest)
            .retrieve()
            .body(List.class);

        // Assert - search should return results
        assertThat(results).isNotNull();

        // Cleanup - delete chunks, then document, then KB
        List<DocumentChunkEntity> chunks = dataManager.entity(DocumentChunkEntity.class)
            .query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(DocumentChunkEntity.class)
                .eq(DocumentChunkEntity::getDocumentId, doc.getId())
                .build())
            .list();
        for (DocumentChunkEntity c : chunks) {
            dataManager.deleteById(DocumentChunkEntity.class, c.getId());
        }
        dataManager.deleteById(DocumentEntity.class, doc.getId());
        dataManager.deleteById(KnowledgeBaseEntity.class, kb.getId());
    }
}
