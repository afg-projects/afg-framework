package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrder;
import io.github.afgprojects.framework.data.jdbc.entity.TestOrderItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 关联加载集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerAssociationTest extends BaseDataTest {

    @Nested
    @DisplayName("ManyToOne 关联加载")
    class ManyToOneAssociation {

        @Test
        @DisplayName("should load many-to-one association when fetch by name")
        void shouldLoadManyToOneAssociation_whenFetchByName() {
            // 准备：创建订单和订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-001", null));
            TestOrderItem item = dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "Product-A", new java.math.BigDecimal("29.99"), 2));

            // 执行：加载订单项的 order 关联
            TestOrderItem loadedItem = dataManager.findById(TestOrderItem.class, item.getId()).orElseThrow();
            TestOrder associatedOrder = dataManager.entity(TestOrderItem.class).fetch(loadedItem, "order");

            // 验证
            assertThat(associatedOrder).isNotNull();
            assertThat(associatedOrder.getId()).isEqualTo(order.getId());
            assertThat(associatedOrder.getOrderNo()).isEqualTo("ORD-001");
        }
    }

    @Nested
    @DisplayName("OneToMany 关联加载")
    class OneToManyAssociation {

        @Test
        @DisplayName("should load one-to-many association when fetch by name")
        void shouldLoadOneToManyAssociation_whenFetchByName() {
            // 准备：创建订单和多个订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-002", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "Product-B1", new java.math.BigDecimal("19.99"), 1));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "Product-B2", new java.math.BigDecimal("39.99"), 3));

            // 执行：加载订单的 items 关联
            TestOrder loadedOrder = dataManager.findById(TestOrder.class, order.getId()).orElseThrow();
            List<TestOrderItem> items = dataManager.entity(TestOrder.class).fetch(loadedOrder, "items");

            // 验证
            assertThat(items).isNotEmpty();
            assertThat(items).hasSize(2);
            assertThat(items).allMatch(item -> item.getOrderId().equals(order.getId()));
        }
    }

    @Nested
    @DisplayName("急加载关联")
    class EagerFetchAssociation {

        @Test
        @DisplayName("should eagerly load associations when query with withAssociation")
        void shouldEagerlyLoadAssociations_whenQueryWithWithAssociation() {
            // 准备：创建订单和订单项
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-003", null));
            dataManager.save(TestOrderItem.class,
                TestOrderItem.create(order.getId(), "Product-C", new java.math.BigDecimal("59.99"), 5));

            // 执行：使用 withAssociation 急加载
            List<TestOrder> orders = dataManager.entity(TestOrder.class)
                .query()
                .withAssociation("items")
                .list();

            // 验证：订单项应该已经被加载
            assertThat(orders).isNotEmpty();
            TestOrder loadedOrder = orders.stream()
                .filter(o -> o.getId().equals(order.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(loadedOrder.getItems()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("关联加载异常场景")
    class AssociationException {

        @Test
        @DisplayName("should throw exception when fetch non-existing association name")
        void shouldThrowException_whenFetchNonExistingAssociationName() {
            TestOrder order = dataManager.save(TestOrder.class, TestOrder.create("ORD-004", null));

            assertThatThrownBy(() -> dataManager.entity(TestOrder.class).fetch(order, "nonExisting"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Association 'nonExisting' not found");
        }

        @Test
        @DisplayName("should throw exception when fetch association on entity without id")
        void shouldThrowException_whenFetchAssociationOnEntityWithoutId() {
            TestOrder newOrder = new TestOrder();
            newOrder.setOrderNo("ORD-NEW");

            assertThatThrownBy(() -> dataManager.entity(TestOrder.class).fetch(newOrder, "items"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must have an ID");
        }
    }
}
