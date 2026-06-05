package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.VersionedEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 测试用乐观锁实体
 */
@Getter
@Setter
public class TestVersionedItem extends VersionedEntity implements LifecycleCallbacks {

    private String name;
    private Integer stock;

    public static TestVersionedItem create(String name, int stock) {
        TestVersionedItem item = new TestVersionedItem();
        item.setName(name);
        item.setStock(stock);
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
