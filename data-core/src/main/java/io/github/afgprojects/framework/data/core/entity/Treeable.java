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
package io.github.afgprojects.framework.data.core.entity;

import java.util.List;

/**
 * 树形结构实体特征接口
 * <p>
 * 标记实体具有树形层级结构，支持 parentId/level/path/sortOrder/children 字段。
 * 配合 {@link TreeEntity} 基类使用，框架通过此接口检测树形结构特征，
 * 自动提供路径查询、子树查询等能力。
 *
 * <h3>路径格式</h3>
 * <p>
 * {@code path} 字段使用 {@code /} 分隔的祖先 ID 路径，格式为 {@code /1/5/12/}（前后都有 {@code /}）。
 * 根节点的 path 为 {@code /}。路径格式支持高效的子树查询：
 * <pre>
 * -- 查询 ID=5 的节点及其所有子孙节点
 * WHERE path LIKE '/1/5/%'
 * </pre>
 *
 * <h3>层级深度</h3>
 * <p>
 * {@code level} 从 1 开始，根节点 level=1，每深入一级加 1。
 *
 * @param <T> 实体类型（自引用泛型，支持类型安全的 children 列表）
 * @see TreeEntity
 */
public interface Treeable<T> {

    /**
     * 获取父节点 ID
     * <p>
     * 根节点的 parentId 为 null
     *
     * @return 父节点 ID，根节点返回 null
     */
    Long getParentId();

    /**
     * 设置父节点 ID
     *
     * @param parentId 父节点 ID，根节点设为 null
     */
    void setParentId(Long parentId);

    /**
     * 获取层级深度
     * <p>
     * 根节点 level=1，每深入一级加 1
     *
     * @return 层级深度
     */
    Integer getLevel();

    /**
     * 设置层级深度
     *
     * @param level 层级深度
     */
    void setLevel(Integer level);

    /**
     * 获取祖先 ID 路径
     * <p>
     * 格式为 {@code /1/5/12/}（前后都有 {@code /}），根节点为 {@code /}。
     * 路径中的 ID 为从根到当前节点的祖先 ID 序列。
     *
     * @return 祖先 ID 路径
     */
    String getPath();

    /**
     * 设置祖先 ID 路径
     *
     * @param path 祖先 ID 路径，格式 {@code /id1/id2/.../}
     */
    void setPath(String path);

    /**
     * 获取同级排序号
     * <p>
     * 同一父节点下的子节点按 sortOrder 升序排列
     *
     * @return 排序号
     */
    Integer getSortOrder();

    /**
     * 设置同级排序号
     *
     * @param sortOrder 排序号
     */
    void setSortOrder(Integer sortOrder);

    /**
     * 获取子节点列表
     * <p>
     * 子节点列表是瞬态数据，不持久化到数据库。
     * 通过 {@code dataManager.entity(Xxx.class).fetch(parent, "children")} 或
     * {@code query().withAssociation("children")} 加载。
     *
     * @return 子节点列表，可能为 null（未加载时）
     */
    List<T> getChildren();

    /**
     * 设置子节点列表
     *
     * @param children 子节点列表
     */
    void setChildren(List<T> children);
}
