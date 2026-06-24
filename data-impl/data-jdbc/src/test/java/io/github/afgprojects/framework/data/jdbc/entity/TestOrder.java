package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.relation.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用订单实体（用于关联加载测试）
 */
@Getter
@Setter
public class TestOrder extends BaseEntity implements LifecycleCallbacks {

    private String orderNo;
    private String userId;

    /**
     * 订单项列表（一对多关联）
     */
    @OneToMany(mappedBy = "order", targetEntity = TestOrderItem.class)
    private List<TestOrderItem> items = new ArrayList<>();

    public static TestOrder create(String orderNo, String userId) {
        TestOrder order = new TestOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        return order;
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
