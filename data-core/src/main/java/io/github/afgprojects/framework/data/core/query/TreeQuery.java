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
package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.entity.Treeable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 树形结构查询接口
 * <p>
 * 提供树形实体的便捷查询操作，包括子节点查询、后代查询、祖先查询、
 * 根节点查询、树构建和节点移动等操作。
 * <p>
 * 使用示例：
 * <pre>
 * // 查询直接子节点
 * TreeQuery&lt;Dept&gt; treeQuery = dataManager.entity(Dept.class).treeQuery();
 * List&lt;Dept&gt; children = treeQuery.findChildren("1");
 *
 * // 查询所有后代（含自身）
 * List&lt;Dept&gt; descendants = treeQuery.findDescendants("1");
 *
 * // 构建完整树
 * List&lt;TreeNode&lt;Dept&gt;&gt; tree = treeQuery.buildTree();
 *
 * // 从指定节点构建子树
 * List&lt;TreeNode&lt;Dept&gt;&gt; subTree = treeQuery.buildTree("1");
 *
 * // 移动节点
 * treeQuery.moveNode("5", "10");
 * </pre>
 *
 * @param <T> 实体类型，必须实现 {@link Treeable} 接口
 * @see Treeable
 * @see TreeNode
 * @see io.github.afgprojects.framework.data.core.entity.TreeEntity
 */
public interface TreeQuery<T extends Treeable<?>> {

    /**
     * 查询指定节点的直接子节点
     * <p>
     * 返回 parentId 等于指定 ID 的所有节点，按 sortOrder 升序排列。
     *
     * @param parentId 父节点 ID，null 表示查询根节点
     * @return 直接子节点列表
     */
    @NonNull List<T> findChildren(@Nullable String parentId);

    /**
     * 查询指定节点的所有后代节点（不含自身）
     * <p>
     * 使用 path 字段进行高效查询：{@code WHERE path LIKE '/1/5/%'}。
     * 仅当节点已持久化（有 ID 和 path）时才能查询后代。
     *
     * @param parentId 祖先节点 ID
     * @return 所有后代节点列表
     */
    @NonNull List<T> findDescendants(@NonNull String parentId);

    /**
     * 查询指定节点的所有祖先节点（不含自身）
     * <p>
     * 通过解析 path 字段获取祖先 ID 列表，然后批量查询。
     * 祖先节点按从根到父的顺序排列（根节点在前）。
     *
     * @param id 节点 ID
     * @return 所有祖先节点列表（从根到父排列）
     */
    @NonNull List<T> findAncestors(@NonNull String id);

    /**
     * 查询所有根节点
     * <p>
     * 返回 parentId 为 null 的所有节点，按 sortOrder 升序排列。
     *
     * @return 根节点列表
     */
    @NonNull List<T> findRoots();

    /**
     * 构建完整树结构
     * <p>
     * 查询所有节点，按 parentId 关系组装为树结构。
     * 根节点的 TreeNode 列表构成多棵树的根。
     *
     * @return 树结构列表（每个根节点对应一棵树）
     */
    @NonNull List<TreeNode<T>> buildTree();

    /**
     * 从指定节点构建子树
     * <p>
     * 查询指定节点及其所有后代，组装为树结构。
     * 返回的列表以指定节点作为根。
     *
     * @param rootId 根节点 ID
     * @return 子树结构列表（以指定节点为根）
     */
    @NonNull List<TreeNode<T>> buildTree(@NonNull String rootId);

    /**
     * 移动节点到新的父节点下
     * <p>
     * 更新被移动节点的 parentId、level 和 path，同时更新其所有后代的 level 和 path。
     * 如果 newParentId 为 null，则移动到根级别。
     *
     * @param id          要移动的节点 ID
     * @param newParentId 新的父节点 ID，null 表示移动到根级别
     */
    void moveNode(@NonNull String id, @Nullable String newParentId);
}
