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
 * 树形结构实体基类
 * <p>
 * 提供树形层级结构的通用字段实现，包括父节点 ID、层级深度、祖先路径、同级排序和子节点列表。
 * 继承此基类的实体自动具备 {@link Treeable} 特征，框架通过
 * {@link io.github.afgprojects.framework.data.core.metadata.EntityTrait#TREEABLE}
 * 检测树形结构特征。
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;Getter &#064;Setter
 * &#064;AfEntity
 * &#064;Table(name = "sys_dept")
 * public class Department extends TreeEntity&lt;Department&gt; {
 *     &#064;Column(name = "dept_name", nullable = false, length = 100)
 *     private String deptName;
 * }
 * </pre>
 *
 * <h3>路径格式</h3>
 * <p>
 * {@code path} 字段使用 {@code /} 分隔的祖先 ID 路径（如 {@code /1/5/12/}），
 * 根节点 path 为 {@code /}。框架在保存树形实体时自动计算和更新 path 字段。
 * 路径格式支持高效的 LIKE 子树查询：{@code WHERE path LIKE '/1/5/%'}。
 *
 * <h3>子节点关联</h3>
 * <p>
 * 子节点关联通过 {@code @OneToMany(mappedBy = "parentId")} 声明（在子类中），
 * 或通过 {@code dataManager.entity(Xxx.class).fetch(parent, "children")} 显式加载。
 * {@code children} 字段为瞬态数据，不持久化到数据库。
 *
 * @param <T> 实体类型（自引用泛型，支持类型安全的 children 列表）
 * @see Treeable
 * @see io.github.afgprojects.framework.data.core.metadata.EntityTrait#TREEABLE
 */
public abstract class
TreeEntity<T> extends BaseEntity implements Treeable<T> {

    /**
     * 父节点 ID
     * <p>
     * 根节点的 parentId 为 null。非根节点的 parentId 指向其直接父节点的 id。
     */
    protected String parentId;

    /**
     * 层级深度
     * <p>
     * 根节点 level=1，每深入一级加 1。
     * 框架在保存时自动计算：子节点 level = 父节点 level + 1。
     */
    protected Integer level = 1;

    /**
     * 祖先 ID 路径
     * <p>
     * 格式为 {@code /1/5/12/}（前后都有 {@code /}），根节点为 {@code /}。
     * 框架在保存时自动计算：子节点 path = 父节点 path + 父节点 id + "/"。
     */
    protected String path = "/";

    /**
     * 同级排序号
     * <p>
     * 同一父节点下的子节点按 sortOrder 升序排列，默认为 0。
     */
    protected Integer sortOrder = 0;

    /**
     * 子节点列表（瞬态，不持久化）
     * <p>
     * 通过关联查询加载，不在 INSERT/UPDATE 时写入数据库。
     * 使用 {@code @Transient} 注解标记为非持久化字段，
     * 元数据提取（APT 和反射）将自动跳过此字段。
     */
    @jakarta.persistence.Transient
    protected List<T> children;

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public Integer getLevel() {
        return level;
    }

    @Override
    public void setLevel(Integer level) {
        this.level = level;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Integer getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public List<T> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<T> children) {
        this.children = children;
    }
}
