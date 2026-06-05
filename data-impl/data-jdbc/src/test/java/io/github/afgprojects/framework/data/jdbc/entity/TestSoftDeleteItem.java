package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 测试用软删除实体
 */
@Getter
@Setter
public class TestSoftDeleteItem extends SoftDeleteEntity implements LifecycleCallbacks {

    private String name;
    private Integer quantity;

    public static TestSoftDeleteItem create(String name, int quantity) {
        TestSoftDeleteItem item = new TestSoftDeleteItem();
        item.setName(name);
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
