package io.github.afgprojects.framework.data.core.vector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 向量操作接口
 * <p>
 * 提供向量存储和相似度搜索功能。
 *
 * <p>示例用法：
 * <pre>{@code
 * // 添加向量
 * vectorOps.add("doc-1", "文档内容", List.of(0.1, 0.2, 0.3), Map.of("category", "tech"));
 *
 * // 相似度搜索
 * List<VectorResult> results = vectorOps.similaritySearch(List.of(0.1, 0.2, 0.3), 5);
 *
 * // 带租户过滤的搜索
 * List<VectorResult> results = vectorOps.similaritySearch(embedding, 5, "tenant-1", null);
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface VectorOperations {

    /**
     * 添加向量文档
     *
     * @param id        文档ID
     * @param content   文档内容
     * @param embedding 向量嵌入
     * @param metadata  元数据
     */
    void add(@NonNull String id, @NonNull String content,
             @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata);

    /**
     * 添加向量文档（带租户和用户信息）
     *
     * @param id        文档ID
     * @param content   文档内容
     * @param embedding 向量嵌入
     * @param metadata  元数据
     * @param tenantId  租户ID
     * @param userId    用户ID
     */
    void add(@NonNull String id, @NonNull String content,
             @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata,
             @Nullable String tenantId, @Nullable String userId);

    /**
     * 批量添加向量文档
     *
     * @param documents 文档列表
     */
    void addAll(@NonNull List<VectorDocument> documents);

    /**
     * 批量添加向量文档（带租户和用户信息）
     *
     * @param documents 文档列表
     * @param tenantId  租户ID
     * @param userId    用户ID
     */
    void addAll(@NonNull List<VectorDocument> documents, @Nullable String tenantId, @Nullable String userId);

    /**
     * 相似度搜索
     *
     * @param embedding 查询向量
     * @param k         返回数量
     * @return 相似文档列表
     */
    @NonNull
    List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k);

    /**
     * 相似度搜索（带租户和用户过滤）
     *
     * @param embedding 查询向量
     * @param k         返回数量
     * @param tenantId  租户ID（可选）
     * @param userId    用户ID（可选）
     * @return 相似文档列表
     */
    @NonNull
    List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k,
                                        @Nullable String tenantId, @Nullable String userId);

    /**
     * 相似度搜索（带元数据过滤）
     *
     * @param embedding 查询向量
     * @param k         返回数量
     * @param filter    元数据过滤条件
     * @return 相似文档列表
     */
    @NonNull
    List<VectorResult> similaritySearch(@NonNull List<Double> embedding, int k,
                                        @Nullable Map<String, Object> filter);

    /**
     * 根据ID获取文档
     *
     * @param id 文档ID
     * @return 文档，不存在返回 null
     */
    @Nullable
    VectorDocument getById(@NonNull String id);

    /**
     * 删除文档
     *
     * @param id 文档ID
     */
    void delete(@NonNull String id);

    /**
     * 批量删除文档
     *
     * @param ids 文档ID列表
     */
    void deleteAll(@NonNull List<String> ids);

    /**
     * 删除租户的所有文档
     *
     * @param tenantId 租户ID
     */
    void deleteByTenant(@NonNull String tenantId);

    /**
     * 更新文档
     *
     * @param id        文档ID
     * @param content   新内容
     * @param embedding 新向量
     * @param metadata  新元数据
     */
    void update(@NonNull String id, @NonNull String content,
                @NonNull List<Double> embedding, @Nullable Map<String, Object> metadata);

    /**
     * 检查文档是否存在
     *
     * @param id 文档ID
     * @return 是否存在
     */
    boolean exists(@NonNull String id);

    /**
     * 统计文档数量
     *
     * @return 文档数量
     */
    long count();

    /**
     * 统计租户的文档数量
     *
     * @param tenantId 租户ID
     * @return 文档数量
     */
    long countByTenant(@NonNull String tenantId);

    /**
     * 清空所有文档
     */
    void clear();
}
