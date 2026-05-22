package io.github.afgprojects.framework.data.jdbc.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.data.core.vector.VectorDocument;
import io.github.afgprojects.framework.data.core.vector.VectorOperations;
import io.github.afgprojects.framework.data.core.vector.VectorResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC 向量操作实现（基于 PostgreSQL pgvector）
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class JdbcVectorOperations implements VectorOperations {

    private static final Logger log = LoggerFactory.getLogger(JdbcVectorOperations.class);

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final ObjectMapper objectMapper;

    public JdbcVectorOperations(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void add(@NonNull String id, @NonNull String content,
                    @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata) {
        add(id, content, embedding, metadata, null, null);
    }

    @Override
    public void add(@NonNull String id, @NonNull String content,
                    @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata,
                    @Nullable String tenantId, @Nullable String userId) {
        String sql = """
            INSERT INTO %s (id, content, embedding, metadata, tenant_id, user_id, created_at)
            VALUES (?, ?, ?::vector, ?::jsonb, ?, ?, NOW())
            ON CONFLICT (id) DO UPDATE SET
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                metadata = EXCLUDED.metadata,
                tenant_id = EXCLUDED.tenant_id,
                user_id = EXCLUDED.user_id
            """.formatted(tableName);

        String vectorStr = vectorToString(embedding);
        String metadataJson = metadataToJson(metadata);

        jdbcTemplate.update(sql, id, content, vectorStr, metadataJson, tenantId, userId);
        log.debug("Added vector document: id={}, tenant={}", id, tenantId);
    }

    @Override
    public void addAll(@NonNull List<VectorDocument> documents) {
        addAll(documents, null, null);
    }

    @Override
    public void addAll(@NonNull List<VectorDocument> documents, @Nullable String tenantId, @Nullable String userId) {
        for (VectorDocument doc : documents) {
            String docTenantId = doc.tenantId() != null ? doc.tenantId() : tenantId;
            String docUserId = doc.userId() != null ? doc.userId() : userId;
            add(doc.id(), doc.content(), doc.embedding(), doc.metadata(), docTenantId, docUserId);
        }
    }

    @Override
    public @NonNull List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k) {
        return similaritySearch(embedding, k, null, null);
    }

    @Override
    public @NonNull List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k,
                                                         @Nullable String tenantId, @Nullable String userId) {
        StringBuilder whereClause = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();

        if (tenantId != null) {
            whereClause.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        if (userId != null) {
            whereClause.append(" AND user_id = ?");
            params.add(userId);
        }

        String vectorStr = vectorToString(embedding);
        params.add(vectorStr);
        params.add(vectorStr);
        params.add(k);

        String sql = """
            SELECT id, content, embedding, metadata,
                   1 - (embedding <=> ?::vector) as similarity,
                   tenant_id, user_id
            FROM %s
            WHERE %s
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """.formatted(tableName, whereClause);

        return jdbcTemplate.query(sql, params.toArray(), this::mapToVectorResult);
    }

    @Override
    public @NonNull List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k,
                                                         @Nullable Map<String, Object> filter) {
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (filter != null && !filter.isEmpty()) {
            for (Map.Entry<String, Object> entry : filter.entrySet()) {
                if (!whereClause.isEmpty()) {
                    whereClause.append(" AND ");
                }
                whereClause.append("metadata->>'").append(entry.getKey()).append("' = ?");
                params.add(String.valueOf(entry.getValue()));
            }
        } else {
            whereClause.append("1=1");
        }

        String vectorStr = vectorToString(embedding);
        params.add(vectorStr);
        params.add(vectorStr);
        params.add(k);

        String sql = """
            SELECT id, content, embedding, metadata,
                   1 - (embedding <=> ?::vector) as similarity,
                   tenant_id, user_id
            FROM %s
            WHERE %s
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """.formatted(tableName, whereClause);

        return jdbcTemplate.query(sql, params.toArray(), this::mapToVectorResult);
    }

    @Override
    public @Nullable VectorDocument getById(@NonNull String id) {
        String sql = "SELECT id, content, embedding, metadata, tenant_id, user_id FROM %s WHERE id = ?".formatted(tableName);

        List<VectorDocument> results = jdbcTemplate.query(sql,
            ps -> ps.setString(1, id),
            (rs, rowNum) -> {
                String content = rs.getString("content");
                String embeddingStr = rs.getString("embedding");
                String metadataJson = rs.getString("metadata");
                String tenantId = rs.getString("tenant_id");
                String userId = rs.getString("user_id");

                return new VectorDocument(
                    id,
                    content,
                    parseVector(embeddingStr),
                    parseMetadata(metadataJson),
                    tenantId,
                    userId
                );
            });

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void delete(@NonNull String id) {
        String sql = "DELETE FROM %s WHERE id = ?".formatted(tableName);
        jdbcTemplate.update(sql, id);
        log.debug("Deleted vector document: id={}", id);
    }

    @Override
    public void deleteAll(@NonNull List<String> ids) {
        if (ids.isEmpty()) return;

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM %s WHERE id IN (%s)".formatted(tableName, placeholders);
        jdbcTemplate.update(sql, ids.toArray());
        log.debug("Deleted {} vector documents", ids.size());
    }

    @Override
    public void deleteByTenant(@NonNull String tenantId) {
        String sql = "DELETE FROM %s WHERE tenant_id = ?".formatted(tableName);
        int deleted = jdbcTemplate.update(sql, tenantId);
        log.info("Deleted {} vector documents for tenant: {}", deleted, tenantId);
    }

    @Override
    public void update(@NonNull String id, @NonNull String content,
                       @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata) {
        add(id, content, embedding, metadata);
    }

    @Override
    public boolean exists(@NonNull String id) {
        String sql = "SELECT COUNT(*) FROM %s WHERE id = ?".formatted(tableName);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM %s".formatted(tableName);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public long countByTenant(@NonNull String tenantId) {
        String sql = "SELECT COUNT(*) FROM %s WHERE tenant_id = ?".formatted(tableName);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
        return count != null ? count : 0;
    }

    @Override
    public void clear() {
        String sql = "TRUNCATE TABLE %s".formatted(tableName);
        jdbcTemplate.execute(sql);
        log.info("Cleared vector table: {}", tableName);
    }

    // ==================== 辅助方法 ====================

    private String vectorToString(List<Double> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private List<Double> parseVector(String vector) {
        if (vector == null || vector.isEmpty()) {
            return List.of();
        }
        String cleaned = vector.replaceAll("[\\[\\]]", "");
        return Arrays.stream(cleaned.split(","))
            .map(String::trim)
            .map(Double::parseDouble)
            .toList();
    }

    private String metadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private VectorResult mapToVectorResult(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String id = rs.getString("id");
        String content = rs.getString("content");
        String embeddingStr = rs.getString("embedding");
        String metadataJson = rs.getString("metadata");
        double similarity = rs.getDouble("similarity");
        String tenantId = rs.getString("tenant_id");
        String userId = rs.getString("user_id");

        return new VectorResult(
            id,
            content,
            parseVector(embeddingStr),
            parseMetadata(metadataJson),
            similarity,
            tenantId,
            userId
        );
    }
}
