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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TreeEntity 测试
 */
@DisplayName("TreeEntity 测试")
class TreeEntityTest {

    /**
     * 测试用具体子类
     */
    static class TestTreeNode extends TreeEntity<TestTreeNode> {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("新建实体 parentId 默认为 null")
        void shouldDefaultToNull_whenNewEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity.getParentId()).isNull();
        }

        @Test
        @DisplayName("新建实体 level 默认为 1")
        void shouldDefaultToOne_whenNewEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("新建实体 path 默认为 /")
        void shouldDefaultToSlash_whenNewEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("新建实体 sortOrder 默认为 0")
        void shouldDefaultToZero_whenNewEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("新建实体 children 默认为 null")
        void childrenShouldDefaultToNull_whenNewEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity.getChildren()).isNull();
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetterTests {

        @Test
        @DisplayName("应能设置和获取 parentId")
        void shouldSetAndGetParentId() {
            TestTreeNode entity = new TestTreeNode();
            entity.setParentId(100L);
            assertThat(entity.getParentId()).isEqualTo(100L);
            entity.setParentId(null);
            assertThat(entity.getParentId()).isNull();
        }

        @Test
        @DisplayName("应能设置和获取 level")
        void shouldSetAndGetLevel() {
            TestTreeNode entity = new TestTreeNode();
            entity.setLevel(3);
            assertThat(entity.getLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("应能设置和获取 path")
        void shouldSetAndGetPath() {
            TestTreeNode entity = new TestTreeNode();
            entity.setPath("/1/5/12/");
            assertThat(entity.getPath()).isEqualTo("/1/5/12/");
        }

        @Test
        @DisplayName("应能设置和获取 sortOrder")
        void shouldSetAndGetSortOrder() {
            TestTreeNode entity = new TestTreeNode();
            entity.setSortOrder(10);
            assertThat(entity.getSortOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("应能设置和获取 children")
        void shouldSetAndGetChildren() {
            TestTreeNode entity = new TestTreeNode();
            List<TestTreeNode> children = new ArrayList<>();
            children.add(new TestTreeNode());
            entity.setChildren(children);
            assertThat(entity.getChildren()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Treeable 接口")
    class TreeableInterfaceTests {

        @Test
        @DisplayName("TreeEntity 应实现 Treeable 接口")
        void shouldImplementTreeable() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity).isInstanceOf(Treeable.class);
        }

        @Test
        @DisplayName("Treeable 接口方法应正常工作")
        void treeableMethodsShouldWork() {
            Treeable<TestTreeNode> entity = new TestTreeNode();
            entity.setParentId(50L);
            entity.setLevel(2);
            entity.setPath("/1/");
            entity.setSortOrder(5);

            assertThat(entity.getParentId()).isEqualTo(50L);
            assertThat(entity.getLevel()).isEqualTo(2);
            assertThat(entity.getPath()).isEqualTo("/1/");
            assertThat(entity.getSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("BaseEntity 继承")
    class BaseEntityInheritanceTests {

        @Test
        @DisplayName("TreeEntity 应继承 BaseEntity")
        void shouldExtendBaseEntity() {
            TestTreeNode entity = new TestTreeNode();
            assertThat(entity).isInstanceOf(BaseEntity.class);
        }

        @Test
        @DisplayName("应能设置和获取 id（来自 BaseEntity）")
        void shouldSetAndGetId() {
            TestTreeNode entity = new TestTreeNode();
            entity.setId(1L);
            assertThat(entity.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("equals 应基于 id 判断（来自 BaseEntity）")
        void equalsShouldBeBasedOnId() {
            TestTreeNode entity1 = new TestTreeNode();
            entity1.setId(1L);
            TestTreeNode entity2 = new TestTreeNode();
            entity2.setId(1L);

            assertThat(entity1).isEqualTo(entity2);
        }

        @Test
        @DisplayName("hashCode 应基于 id 计算（来自 BaseEntity）")
        void hashCodeShouldBeBasedOnId() {
            TestTreeNode entity1 = new TestTreeNode();
            entity1.setId(1L);
            TestTreeNode entity2 = new TestTreeNode();
            entity2.setId(1L);

            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("新建实体（id=null）equals 应返回 false")
        void newEntitiesShouldNotBeEqual() {
            TestTreeNode entity1 = new TestTreeNode();
            TestTreeNode entity2 = new TestTreeNode();

            assertThat(entity1).isNotEqualTo(entity2);
        }
    }

    @Nested
    @DisplayName("树形结构构建")
    class TreeStructureTests {

        @Test
        @DisplayName("应能构建父子关系")
        void shouldBuildParentChildRelationship() {
            // 创建根节点
            TestTreeNode root = new TestTreeNode();
            root.setId(1L);
            root.setName("root");
            root.setParentId(null);
            root.setLevel(1);
            root.setPath("/");

            // 创建子节点
            TestTreeNode child1 = new TestTreeNode();
            child1.setId(2L);
            child1.setName("child1");
            child1.setParentId(1L);
            child1.setLevel(2);
            child1.setPath("/1/");

            TestTreeNode child2 = new TestTreeNode();
            child2.setId(3L);
            child2.setName("child2");
            child2.setParentId(1L);
            child2.setLevel(2);
            child2.setPath("/1/");

            // 设置 children
            List<TestTreeNode> children = new ArrayList<>();
            children.add(child1);
            children.add(child2);
            root.setChildren(children);

            // 验证
            assertThat(root.getParentId()).isNull();
            assertThat(root.getLevel()).isEqualTo(1);
            assertThat(root.getPath()).isEqualTo("/");
            assertThat(root.getChildren()).hasSize(2);
            assertThat(root.getChildren().get(0).getParentId()).isEqualTo(1L);
            assertThat(root.getChildren().get(1).getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应能构建多级树形结构")
        void shouldBuildMultiLevelTree() {
            // Level 1: root
            TestTreeNode root = new TestTreeNode();
            root.setId(1L);
            root.setLevel(1);
            root.setPath("/");

            // Level 2: child
            TestTreeNode child = new TestTreeNode();
            child.setId(2L);
            child.setParentId(1L);
            child.setLevel(2);
            child.setPath("/1/");

            // Level 3: grandchild
            TestTreeNode grandchild = new TestTreeNode();
            grandchild.setId(3L);
            grandchild.setParentId(2L);
            grandchild.setLevel(3);
            grandchild.setPath("/1/2/");

            // 设置 children
            List<TestTreeNode> rootChildren = new ArrayList<>();
            rootChildren.add(child);
            root.setChildren(rootChildren);

            List<TestTreeNode> childChildren = new ArrayList<>();
            childChildren.add(grandchild);
            child.setChildren(childChildren);

            // 验证层级
            assertThat(root.getLevel()).isEqualTo(1);
            assertThat(child.getLevel()).isEqualTo(2);
            assertThat(grandchild.getLevel()).isEqualTo(3);

            // 验证路径
            assertThat(root.getPath()).isEqualTo("/");
            assertThat(child.getPath()).isEqualTo("/1/");
            assertThat(grandchild.getPath()).isEqualTo("/1/2/");
        }

        @Test
        @DisplayName("sortOrder 应影响同级排序")
        void sortOrderShouldAffectOrdering() {
            TestTreeNode node1 = new TestTreeNode();
            node1.setSortOrder(3);

            TestTreeNode node2 = new TestTreeNode();
            node2.setSortOrder(1);

            TestTreeNode node3 = new TestTreeNode();
            node3.setSortOrder(2);

            // 按 sortOrder 排序
            List<TestTreeNode> nodes = new ArrayList<>();
            nodes.add(node1);
            nodes.add(node2);
            nodes.add(node3);
            nodes.sort((a, b) -> {
                int orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
                int orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
                return Integer.compare(orderA, orderB);
            });

            assertThat(nodes.get(0).getSortOrder()).isEqualTo(1);
            assertThat(nodes.get(1).getSortOrder()).isEqualTo(2);
            assertThat(nodes.get(2).getSortOrder()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("路径格式")
    class PathFormatTests {

        @Test
        @DisplayName("根节点 path 应为 /")
        void rootPathShouldBeSlash() {
            TestTreeNode root = new TestTreeNode();
            root.setParentId(null);
            root.setPath("/");
            assertThat(root.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("一级子节点 path 应为 /parentId/")
        void firstLevelChildPathShouldBeSlashParentIdSlash() {
            TestTreeNode child = new TestTreeNode();
            child.setParentId(1L);
            child.setPath("/1/");
            assertThat(child.getPath()).isEqualTo("/1/");
        }

        @Test
        @DisplayName("二级子节点 path 应为 /grandparentId/parentId/")
        void secondLevelChildPathShouldContainAncestors() {
            TestTreeNode grandchild = new TestTreeNode();
            grandchild.setParentId(5L);
            grandchild.setPath("/1/5/");
            assertThat(grandchild.getPath()).isEqualTo("/1/5/");
        }

        @Test
        @DisplayName("path 应支持 LIKE 子树查询格式")
        void pathShouldSupportLikeQuery() {
            // 模拟子树查询条件
            TestTreeNode node = new TestTreeNode();
            node.setPath("/1/5/12/");

            // 验证 LIKE '/1/5/%' 能匹配
            assertThat(node.getPath()).startsWith("/1/5/");
        }
    }
}
