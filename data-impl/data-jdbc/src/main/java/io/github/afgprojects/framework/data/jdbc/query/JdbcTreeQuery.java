/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.jdbc.query;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import io.github.afgprojects.framework.data.core.entity.Treeable;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.TreeNode;
import io.github.afgprojects.framework.data.core.query.TreeQuery;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy;
import io.github.afgprojects.framework.data.jdbc.TreePathCalculator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC TreeQuery 实现
 * <p>
 * 组合 JdbcDataManager 和 TreePathCalculator，提供树形实体的便捷查询操作。
 * 使用 path 字段进行高效的后代查询，使用内存组装策略构建树结构。
 *
 * @param <T> 实体类型，必须实现 Treeable 接口
 * @see TreeQuery
 * @see TreePathCalculator
 */
@Slf4j
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "unchecked"})
public class JdbcTreeQuery<T> implements TreeQuery<Treeable<?>> {

    private final Class<T> entityClass;
    private final JdbcEntityProxy<T> proxy;
    private final JdbcDataManager dataManager;
    private final EntityMetadata<T> metadata;

    public JdbcTreeQuery(JdbcEntityProxy<T> proxy) {
        this.entityClass = proxy.getEntityClass();
        this.proxy = proxy;
        this.dataManager = proxy.getDataManager();
        this.metadata = dataManager.getEntityMetadata(entityClass);

        if (!metadata.hasTrait(EntityTrait.TREEABLE)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                    "Entity " + entityClass.getSimpleName() + " does not implement Treeable interface");
        }
    }

    @Override
    public @NonNull List<Treeable<?>> findChildren(@Nullable Long parentId) {
        String tableName = metadata.getTableName();
        String quotedTableName = proxy.getDialect().quoteIdentifier(tableName);
        String parentIdColumn = proxy.getDialect().quoteIdentifier(
                metadata.getField("parentId").getColumnName());
        String sortOrderColumn = proxy.getDialect().quoteIdentifier(
                metadata.getField("sortOrder").getColumnName());

        String sql;
        List<Object> params;
        if (parentId == null) {
            sql = "SELECT * FROM " + quotedTableName + " WHERE " + parentIdColumn + " IS NULL ORDER BY " + sortOrderColumn + " ASC";
            params = List.of();
        } else {
            sql = "SELECT * FROM " + quotedTableName + " WHERE " + parentIdColumn + " = ? ORDER BY " + sortOrderColumn + " ASC";
            params = List.of(parentId);
        }

        List<T> results = proxy.getJdbcClient().sql(sql)
                .params(params)
                .query(proxy.getRowMapper())
                .list();
        return (List<Treeable<?>>) (List<?>) results;
    }

    @Override
    public @NonNull List<Treeable<?>> findDescendants(@NonNull Long parentId) {
        // 查询父节点获取其 path
        T parent = proxy.findById(parentId).orElseThrow(() ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Parent node not found with id: " + parentId));

        if (!(parent instanceof Treeable<?> treeable)) {
            return List.of();
        }

        String parentPath = treeable.getPath();
        if (parentPath == null || parentPath.isEmpty()) {
            return List.of();
        }

        // 使用 LIKE 查询所有后代（path 以 parentPath + parentId/ 开头）
        String descendantPathPattern = parentPath + parentId + "/%";

        // 查询 path LIKE 'parentPath/parentId/%' 的所有节点
        String tableName = metadata.getTableName();
        String quotedTableName = proxy.getDialect().quoteIdentifier(tableName);
        String pathColumn = proxy.getDialect().quoteIdentifier(
                metadata.getField("path").getColumnName());

        String sql = "SELECT * FROM " + quotedTableName + " WHERE " + pathColumn + " LIKE ?";
        List<Object> params = List.of(descendantPathPattern);

        List<T> results = proxy.getJdbcClient().sql(sql)
                .params(params)
                .query(proxy.getRowMapper())
                .list();
        return (List<Treeable<?>>) (List<?>) results;
    }

    @Override
    public @NonNull List<Treeable<?>> findAncestors(@NonNull Long id) {
        // 查询当前节点获取其 path
        T node = proxy.findById(id).orElseThrow(() ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Node not found with id: " + id));

        if (!(node instanceof Treeable<?> treeable)) {
            return List.of();
        }

        String path = treeable.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            // 根节点没有祖先
            return List.of();
        }

        // 解析 path 获取祖先 ID 列表
        // 格式为 /1/5/12/，需要提取 [1, 5]
        List<Long> ancestorIds = parseAncestorIds(path);
        if (ancestorIds.isEmpty()) {
            return List.of();
        }

        // 批量查询祖先节点，保持从根到父的顺序
        List<Treeable<?>> ancestors = new ArrayList<>();
        for (Long ancestorId : ancestorIds) {
            proxy.findById(ancestorId).ifPresent(a -> ancestors.add((Treeable<?>) a));
        }

        return ancestors;
    }

    @Override
    public @NonNull List<Treeable<?>> findRoots() {
        String tableName = metadata.getTableName();
        String quotedTableName = proxy.getDialect().quoteIdentifier(tableName);
        String parentIdColumn = proxy.getDialect().quoteIdentifier(
                metadata.getField("parentId").getColumnName());
        String sortOrderColumn = proxy.getDialect().quoteIdentifier(
                metadata.getField("sortOrder").getColumnName());

        String sql = "SELECT * FROM " + quotedTableName + " WHERE " + parentIdColumn + " IS NULL ORDER BY " + sortOrderColumn + " ASC";
        List<Object> params = List.of();

        List<T> results = proxy.getJdbcClient().sql(sql)
                .params(params)
                .query(proxy.getRowMapper())
                .list();
        return (List<Treeable<?>>) (List<?>) results;
    }

    @Override
    public @NonNull List<TreeNode<Treeable<?>>> buildTree() {
        // 一次查询所有节点
        List<T> allNodes = proxy.findAll();
        return doBuildTree(allNodes, null);
    }

    @Override
    public @NonNull List<TreeNode<Treeable<?>>> buildTree(@NonNull Long rootId) {
        // 查询指定节点及其所有后代
        T rootNode = proxy.findById(rootId).orElseThrow(() ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Root node not found with id: " + rootId));

        List<Treeable<?>> descendants = findDescendants(rootId);

        // 合并根节点和后代
        List<T> allNodes = new ArrayList<>();
        allNodes.add(rootNode);
        descendants.forEach(d -> allNodes.add((T) d));

        return doBuildTree(allNodes, rootId);
    }

    @Override
    public void moveNode(@NonNull Long id, @Nullable Long newParentId) {
        // 查询要移动的节点
        T node = proxy.findById(id).orElseThrow(() ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                        "Node not found with id: " + id));

        // 防止将节点移动到自身或其后代下
        if (newParentId != null) {
            if (newParentId.equals(id)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                        "Cannot move node to itself");
            }
            // 检查 newParentId 是否是 id 的后代
            List<Treeable<?>> descendants = findDescendants(id);
            Set<Long> descendantIds = descendants.stream()
                    .map(d -> d instanceof BaseEntity be ? be.getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (descendantIds.contains(newParentId)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                        "Cannot move node to its descendant");
            }
        }

        // 查询新的父节点（如果非根级别）
        T newParent = newParentId != null
                ? proxy.findById(newParentId).orElse(null)
                : null;

        // 更新移动节点的 parentId、level、path
        if (node instanceof Treeable<?> treeable) {
            treeable.setParentId(newParentId);
            TreePathCalculator.calculatePath(node, metadata, newParent);
        }
        proxy.save(node);

        // 更新所有后代的 level 和 path
        List<Treeable<?>> descendants = findDescendants(id);
        for (Treeable<?> descendant : descendants) {
            recalculateDescendantPath(descendant, node);
            proxy.save((T) descendant);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析 path 字段获取祖先 ID 列表
     * <p>
     * 格式为 /1/5/12/，提取 [1, 5]（不含自身 ID 12）
     *
     * @param path 祖先路径
     * @return 祖先 ID 列表（从根到父排列）
     */
    private List<Long> parseAncestorIds(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return List.of();
        }

        String[] segments = path.split("/");
        List<Long> ids = new ArrayList<>();
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                try {
                    ids.add(Long.parseLong(segment));
                } catch (NumberFormatException e) {
                    log.warn("Invalid segment in path '{}': {}", path, segment);
                }
            }
        }
        return ids;
    }

    /**
     * 内存中构建树结构
     *
     * @param allNodes 所有节点列表
     * @param rootId   指定根节点 ID，null 表示构建完整树
     * @return 树结构列表
     */
    private List<TreeNode<Treeable<?>>> doBuildTree(List<T> allNodes, @Nullable Long rootId) {
        // 按 ID 建立索引
        Map<Long, Treeable<?>> nodeMap = new LinkedHashMap<>();
        Map<Long, List<Treeable<?>>> childrenMap = new LinkedHashMap<>();

        for (T node : allNodes) {
            if (node instanceof Treeable<?> treeable) {
                Long nodeId = node instanceof BaseEntity be ? be.getId() : null;
                if (nodeId != null) {
                    nodeMap.put(nodeId, treeable);
                }

                Long parentId = treeable.getParentId();
                if (parentId != null) {
                    childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(treeable);
                }
            }
        }

        // 确定根节点
        List<Treeable<?>> rootNodes;
        if (rootId != null) {
            rootNodes = nodeMap.containsKey(rootId) ? List.of(nodeMap.get(rootId)) : List.of();
        } else {
            // 构建完整树：parentId 为 null 的节点为根
            rootNodes = new ArrayList<>();
            for (T node : allNodes) {
                if (node instanceof Treeable<?> t && t.getParentId() == null) {
                    rootNodes.add(t);
                }
            }
            rootNodes.sort(Comparator.comparingInt(n -> n.getSortOrder() != null ? n.getSortOrder() : 0));
        }

        // 递归构建树
        List<TreeNode<Treeable<?>>> tree = new ArrayList<>();
        for (Treeable<?> rootNode : rootNodes) {
            tree.add(buildTreeNode(rootNode, childrenMap));
        }

        return tree;
    }

    /**
     * 递归构建树节点
     */
    private TreeNode<Treeable<?>> buildTreeNode(Treeable<?> node, Map<Long, List<Treeable<?>>> childrenMap) {
        Long nodeId = node instanceof BaseEntity be ? be.getId() : null;
        List<Treeable<?>> childEntities = nodeId != null ? childrenMap.getOrDefault(nodeId, List.of()) : List.of();

        // 排序子节点
        List<Treeable<?>> sortedChildren = childEntities.stream()
                .sorted(Comparator.comparingInt(n -> n.getSortOrder() != null ? n.getSortOrder() : 0))
                .toList();

        TreeNode<Treeable<?>> treeNode = new TreeNode<>(node);
        if (!sortedChildren.isEmpty()) {
            List<TreeNode<Treeable<?>>> childNodes = new ArrayList<>();
            for (Treeable<?> child : sortedChildren) {
                childNodes.add(buildTreeNode(child, childrenMap));
            }
            treeNode.setChildren(childNodes);
        }

        return treeNode;
    }

    /**
     * 重新计算后代节点的 level 和 path
     *
     * @param descendant     后代节点
     * @param movedNode      被移动的节点（已更新 path）
     */
    private void recalculateDescendantPath(Treeable<?> descendant, Object movedNode) {
        if (!(movedNode instanceof Treeable<?> movedTreeable)) {
            return;
        }

        Long movedId = movedNode instanceof BaseEntity be ? be.getId() : null;
        if (movedId == null) {
            return;
        }

        String movedNewPath = movedTreeable.getPath();
        int movedLevel = movedTreeable.getLevel() != null ? movedTreeable.getLevel() : 1;

        String descendantPath = descendant.getPath();
        if (descendantPath == null) {
            return;
        }

        // 查找 movedId 在 path 中的位置
        String movedIdStr = "/" + movedId + "/";
        int index = descendantPath.indexOf(movedIdStr);
        if (index < 0) {
            return;
        }

        // 替换前缀
        String suffix = descendantPath.substring(index + movedIdStr.length() - 1);
        String newPath = movedNewPath + movedId + suffix;
        descendant.setPath(newPath);

        // 重新计算 level
        long slashCount = newPath.chars().filter(c -> c == '/').count();
        int newLevel = (int) (slashCount - 1);
        descendant.setLevel(Math.max(1, newLevel));
    }
}
