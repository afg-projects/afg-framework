package io.github.afgprojects.framework.data.core.entity;

/**
 * 乐观锁实体类
 * <p>
 * 支持乐观锁的实体，继承 BaseEntity，增加 version 字段
 *
 * @param <ID> 主键类型
 */
public abstract class VersionedEntity<ID> extends BaseEntity<ID> implements Versioned {

    /**
     * 版本号（乐观锁）
     */
    protected long version = 0L;

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", version=" + version + '}';
    }
}