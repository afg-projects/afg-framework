package io.github.afgprojects.framework.data.core.entity;

/**
 * 版本化接口
 * <p>
 * 实现此接口的实体支持乐观锁
 */
public interface Versioned {

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    long getVersion();

    /**
     * 设置版本号
     *
     * @param version 版本号
     */
    void setVersion(long version);

    /**
     * 递增版本号
     */
    default void incrementVersion() {
        setVersion(getVersion() + 1);
    }
}