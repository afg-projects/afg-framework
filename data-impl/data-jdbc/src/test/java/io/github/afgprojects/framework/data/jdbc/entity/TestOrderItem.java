package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.relation.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 测试用订单项实体（用于关联加载测试）
 */
@Getter
@Setter
public class TestOrderItem extends BaseEntity implements LifecycleCallbacks {

    private String orderId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;

    /**
     * 所属订单（多对一关联）
     */
    @ManyToOne(targetEntity = TestOrder.class, foreignKey = "order_id")
    private TestOrder order;

    public static TestOrderItem create(String orderId, String productName, BigDecimal price, int quantity) {
        TestOrderItem item = new TestOrderItem();
        item.setOrderId(orderId);
        item.setProductName(productName);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    @Override
    public void beforeCreate() {
        setCreatedAt(Instant.now());
        setUpdatedAt(Instant.now());
    }

    @Override
    public void beforeUpdate() {
        setUpdatedAt(Instant.now());
    }
}
