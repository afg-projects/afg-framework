package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.data.core.entity.Treeable;
import io.github.afgprojects.framework.data.core.query.TreeNode;
import io.github.afgprojects.framework.data.core.query.TreeQuery;
import io.github.afgprojects.framework.data.jdbc.entity.TestCategory;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcTreeQuery 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * 验证 TreeEntity 的 children 字段不会被当作持久化列。
 * </p>
 */
class JdbcTreeQueryTest extends BaseDataTest {

    @Nested
    @DisplayName("TreeEntity 元数据")
    class MetadataValidation {

        @Test
        @DisplayName("should not include children as database column when entity extends TreeEntity")
        void shouldNotIncludeChildrenAsColumn_whenEntityExtendsTreeEntity() {
            // 验证 TreeEntity 子类可以正常保存，children 不出现在 INSERT SQL 中
            TestCategory root = TestCategory.create("Root", "Root category");
            TestCategory saved = dataManager.save(TestCategory.class, root);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Root");
        }

        @Test
        @DisplayName("should detect TREEABLE trait when entity extends TreeEntity")
        void shouldDetectTreeableTrait_whenEntityExtendsTreeEntity() {
            // 验证实体元数据正确检测 TREEABLE 特征
            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            assertThat(treeQuery).isNotNull();
        }
    }

    @Nested
    @DisplayName("findChildren")
    class FindChildren {

        @Test
        @DisplayName("should return direct children when findChildren with parentId")
        void shouldReturnDirectChildren_whenFindChildrenWithParentId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child1 = saveCategory("Child-1", root.getId());
            TestCategory child2 = saveCategory("Child-2", root.getId());
            saveCategory("Grandchild-1", child1.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> children = treeQuery.findChildren(root.getId());

            assertThat(children).hasSize(2);
            assertThat(children.stream().map(c -> ((TestCategory) c).getName()))
                .containsExactly("Child-1", "Child-2");
        }

        @Test
        @DisplayName("should return root nodes when findChildren with null parentId")
        void shouldReturnRootNodes_whenFindChildrenWithNullParentId() {
            saveCategory("Root-A", null);
            saveCategory("Root-B", null);
            TestCategory child = saveCategory("Child", null);
            child.setParentId("99999"); // non-root
            dataManager.save(TestCategory.class, child);

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> roots = treeQuery.findChildren(null);

            assertThat(roots).hasSizeGreaterThanOrEqualTo(2);
            assertThat(roots.stream().map(r -> ((TestCategory) r).getName()))
                .contains("Root-A", "Root-B");
        }

        @Test
        @DisplayName("should return empty list when findChildren with non-existing parentId")
        void shouldReturnEmptyList_whenFindChildrenWithNonExistingParentId() {
            saveCategory("Root", null);

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> children = treeQuery.findChildren("99999");

            assertThat(children).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDescendants")
    class FindDescendants {

        @Test
        @DisplayName("should return all descendants when findDescendants with parentId")
        void shouldReturnAllDescendants_whenFindDescendantsWithParentId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child1 = saveCategory("Child-1", root.getId());
            TestCategory child2 = saveCategory("Child-2", root.getId());
            TestCategory grandchild1 = saveCategory("Grandchild-1", child1.getId());
            saveCategory("Grandchild-2", child2.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> descendants = treeQuery.findDescendants(root.getId());

            assertThat(descendants).hasSize(4);
            assertThat(descendants.stream().map(d -> ((TestCategory) d).getName()))
                .containsExactlyInAnyOrder("Child-1", "Child-2", "Grandchild-1", "Grandchild-2");
        }

        @Test
        @DisplayName("should return only direct children when no deeper descendants exist")
        void shouldReturnOnlyDirectChildren_whenNoDeeperDescendantsExist() {
            TestCategory root = saveCategory("Root", null);
            saveCategory("Child-1", root.getId());
            saveCategory("Child-2", root.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> descendants = treeQuery.findDescendants(root.getId());

            assertThat(descendants).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when findDescendants with non-existing id")
        void shouldThrowException_whenFindDescendantsWithNonExistingId() {
            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();

            assertThatThrownBy(() -> treeQuery.findDescendants("99999"))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("findAncestors")
    class FindAncestors {

        @Test
        @DisplayName("should return all ancestors when findAncestors with deep node id")
        void shouldReturnAllAncestors_whenFindAncestorsWithDeepNodeId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());
            TestCategory grandchild = saveCategory("Grandchild", child.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> ancestors = treeQuery.findAncestors(grandchild.getId());

            assertThat(ancestors).hasSize(2);
            assertThat(((TestCategory) ancestors.get(0)).getName()).isEqualTo("Root");
            assertThat(((TestCategory) ancestors.get(1)).getName()).isEqualTo("Child");
        }

        @Test
        @DisplayName("should return empty list when findAncestors for root node")
        void shouldReturnEmptyList_whenFindAncestorsForRootNode() {
            TestCategory root = saveCategory("Root", null);

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> ancestors = treeQuery.findAncestors(root.getId());

            assertThat(ancestors).isEmpty();
        }

        @Test
        @DisplayName("should return parent only when findAncestors for direct child")
        void shouldReturnParentOnly_whenFindAncestorsForDirectChild() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> ancestors = treeQuery.findAncestors(child.getId());

            assertThat(ancestors).hasSize(1);
            assertThat(((TestCategory) ancestors.get(0)).getName()).isEqualTo("Root");
        }
    }

    @Nested
    @DisplayName("findRoots")
    class FindRoots {

        @Test
        @DisplayName("should return all root nodes when findRoots")
        void shouldReturnAllRootNodes_whenFindRoots() {
            saveCategory("Root-A", null);
            saveCategory("Root-B", null);
            TestCategory rootC = saveCategory("Root-C", null);
            saveCategory("Child-of-C", rootC.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<Treeable<?>> roots = treeQuery.findRoots();

            assertThat(roots.stream().map(r -> ((TestCategory) r).getName()))
                .contains("Root-A", "Root-B", "Root-C");
        }
    }

    @Nested
    @DisplayName("buildTree")
    class BuildTree {

        @Test
        @DisplayName("should build complete tree when buildTree without rootId")
        void shouldBuildCompleteTree_whenBuildTreeWithoutRootId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child1 = saveCategory("Child-1", root.getId());
            TestCategory child2 = saveCategory("Child-2", root.getId());
            saveCategory("Grandchild-1", child1.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<TreeNode<Treeable<?>>> tree = treeQuery.buildTree();

            assertThat(tree).hasSizeGreaterThanOrEqualTo(1);
            // Find the "Root" node in the tree
            TreeNode<Treeable<?>> rootNode = tree.stream()
                .filter(n -> ((TestCategory) n.getData()).getName().equals("Root"))
                .findFirst()
                .orElseThrow();
            assertThat(rootNode.getChildren()).hasSize(2);
            assertThat(rootNode.getChildren().stream()
                .map(n -> ((TestCategory) n.getData()).getName()))
                .containsExactly("Child-1", "Child-2");

            // Child-1 has 1 grandchild
            TreeNode<Treeable<?>> child1Node = rootNode.getChildren().stream()
                .filter(n -> ((TestCategory) n.getData()).getName().equals("Child-1"))
                .findFirst()
                .orElseThrow();
            assertThat(child1Node.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("should build subtree when buildTree with rootId")
        void shouldBuildSubtree_whenBuildTreeWithRootId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());
            saveCategory("Grandchild", child.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            List<TreeNode<Treeable<?>>> subtree = treeQuery.buildTree(child.getId());

            assertThat(subtree).hasSize(1);
            assertThat(((TestCategory) subtree.get(0).getData()).getName()).isEqualTo("Child");
            assertThat(subtree.get(0).getChildren()).hasSize(1);
            assertThat(((TestCategory) subtree.get(0).getChildren().get(0).getData()).getName()).isEqualTo("Grandchild");
        }
    }

    @Nested
    @DisplayName("moveNode")
    class MoveNode {

        @Test
        @DisplayName("should update parent when moveNode to new parent")
        void shouldUpdateParent_whenMoveNodeToNewParent() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child1 = saveCategory("Child-1", root.getId());
            TestCategory child2 = saveCategory("Child-2", root.getId());
            TestCategory grandchild = saveCategory("Grandchild", child1.getId());

            // Move grandchild from child1 to child2
            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            treeQuery.moveNode(grandchild.getId(), child2.getId());

            // Verify the move
            TestCategory moved = dataManager.findById(TestCategory.class, grandchild.getId()).orElseThrow();
            assertThat(moved.getParentId()).isEqualTo(child2.getId());
            assertThat(moved.getLevel()).isEqualTo(child2.getLevel() + 1);
            assertThat(moved.getPath()).isEqualTo("/" + root.getId() + "/" + child2.getId() + "/");
        }

        @Test
        @DisplayName("should move node to root level when moveNode with null newParentId")
        void shouldMoveNodeToRootLevel_whenMoveNodeWithNullNewParentId() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            treeQuery.moveNode(child.getId(), null);

            TestCategory moved = dataManager.findById(TestCategory.class, child.getId()).orElseThrow();
            assertThat(moved.getParentId()).isNull();
            assertThat(moved.getLevel()).isEqualTo(1);
            assertThat(moved.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("should throw exception when moveNode to itself")
        void shouldThrowException_whenMoveNodeToItself() {
            TestCategory root = saveCategory("Root", null);

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();

            assertThatThrownBy(() -> treeQuery.moveNode(root.getId(), root.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw exception when moveNode to descendant")
        void shouldThrowException_whenMoveNodeToDescendant() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());

            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();

            assertThatThrownBy(() -> treeQuery.moveNode(root.getId(), child.getId()))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should update child path when moveNode with descendants")
        void shouldUpdateChildPath_whenMoveNodeWithDescendants() {
            TestCategory root1 = saveCategory("Root-1", null);
            TestCategory root2 = saveCategory("Root-2", null);
            TestCategory child = saveCategory("Child", root1.getId());
            saveCategory("Grandchild", child.getId());

            // Move child from root1 to root2
            TreeQuery<Treeable<?>> treeQuery = dataManager.entity(TestCategory.class).treeQuery();
            treeQuery.moveNode(child.getId(), root2.getId());

            // Verify child's new path and parent
            TestCategory movedChild = dataManager.findById(TestCategory.class, child.getId()).orElseThrow();
            assertThat(movedChild.getParentId()).isEqualTo(root2.getId());
            assertThat(movedChild.getPath()).isEqualTo("/" + root2.getId() + "/");
            assertThat(movedChild.getLevel()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Auto path calculation")
    class AutoPathCalculation {

        @Test
        @DisplayName("should set path to slash when saving root node")
        void shouldSetPathToSlash_whenSavingRootNode() {
            TestCategory root = saveCategory("Root", null);

            assertThat(root.getPath()).isEqualTo("/");
            assertThat(root.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("should calculate path from parent when saving child node")
        void shouldCalculatePathFromParent_whenSavingChildNode() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());

            assertThat(child.getPath()).isEqualTo("/" + root.getId() + "/");
            assertThat(child.getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate deep path when saving grandchild node")
        void shouldCalculateDeepPath_whenSavingGrandchildNode() {
            TestCategory root = saveCategory("Root", null);
            TestCategory child = saveCategory("Child", root.getId());
            TestCategory grandchild = saveCategory("Grandchild", child.getId());

            assertThat(grandchild.getPath()).isEqualTo("/" + root.getId() + "/" + child.getId() + "/");
            assertThat(grandchild.getLevel()).isEqualTo(3);
        }
    }

    // --- Helper methods ---

    private TestCategory saveCategory(String name, String parentId) {
        TestCategory category;
        if (parentId != null) {
            category = TestCategory.create(name, "Description of " + name, parentId);
        } else {
            category = TestCategory.create(name, "Description of " + name);
        }
        return dataManager.save(TestCategory.class, category);
    }
}
