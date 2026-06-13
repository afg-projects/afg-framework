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

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 树节点模型
 * <p>
 * 封装树形实体数据及其子节点列表，用于构建和表示树结构。
 *
 * <p>使用示例：
 * <pre>
 * List&lt;TreeNode&lt;Dept&gt;&gt; tree = dataManager.entity(Dept.class)
 *     .treeQuery()
 *     .buildTree();
 *
 * for (TreeNode&lt;Dept&gt; node : tree) {
 *     Dept dept = node.getData();
 *     List&lt;TreeNode&lt;Dept&gt;&gt; children = node.getChildren();
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @see TreeQuery#buildTree()
 * @see TreeQuery#buildTree(Long)
 */
@Data
public class TreeNode<T> {

    /**
     * 节点数据
     */
    private T data;

    /**
     * 子节点列表
     */
    @Nullable
    private List<TreeNode<T>> children;

    /**
     * 创建一个不含子节点的树节点
     *
     * @param data 节点数据
     */
    public TreeNode(T data) {
        this.data = data;
    }

    /**
     * 创建一个含子节点的树节点
     *
     * @param data     节点数据
     * @param children 子节点列表
     */
    public TreeNode(T data, List<TreeNode<T>> children) {
        this.data = data;
        this.children = children;
    }

    /**
     * 添加子节点
     *
     * @param child 子节点
     */
    public void addChild(TreeNode<T> child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    /**
     * 判断是否为叶子节点（无子节点）
     *
     * @return 如果没有子节点返回 true
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }
}
