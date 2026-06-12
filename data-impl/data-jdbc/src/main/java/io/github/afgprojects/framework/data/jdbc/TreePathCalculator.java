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
package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import io.github.afgprojects.framework.data.core.entity.Treeable;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityTrait;
import org.jspecify.annotations.Nullable;

/**
 * 树形实体路径计算器
 * <p>
 * 根据 parentId 自动计算 TreeEntity 的 level 和 path 字段。
 * <p>
 * 路径格式：/1/5/12/（前后都有 /），根节点为 /
 * 层级格式：根节点 level=1，每深入一级加 1
 *
 * @see Treeable
 * @see io.github.afgprojects.framework.data.core.entity.TreeEntity
 */
public final class TreePathCalculator {

    private TreePathCalculator() {
        // 工具类不允许实例化
    }

    /**
     * 计算树形实体的 level 和 path
     *
     * @param entity       树形实体
     * @param metadata     实体元数据
     * @param parentEntity 父实体（如果 parentId 不为 null），可为 null
     * @param <T>          实体类型
     */
    public static <T> void calculatePath(T entity, EntityMetadata<T> metadata, @Nullable T parentEntity) {
        if (!metadata.hasTrait(EntityTrait.TREEABLE)) {
            return;
        }
        if (!(entity instanceof Treeable<?> treeable)) {
            return;
        }

        Long parentId = treeable.getParentId();
        if (parentId == null) {
            // 根节点
            treeable.setLevel(1);
            treeable.setPath("/");
        } else if (parentEntity instanceof Treeable<?> parent) {
            // 子节点：从父节点派生
            int parentLevel = parent.getLevel() != null ? parent.getLevel() : 1;
            String parentPath = parent.getPath() != null ? parent.getPath() : "/";
            Long parentIdValue = parentEntity instanceof BaseEntity be ? be.getId() : parentId;
            treeable.setLevel(parentLevel + 1);
            treeable.setPath(parentPath + parentIdValue + "/");
        }
        // 如果 parentId 已设置但 parentEntity 为 null，则无法计算路径
        // 这将由 handler 中的数据库查找处理
    }
}
