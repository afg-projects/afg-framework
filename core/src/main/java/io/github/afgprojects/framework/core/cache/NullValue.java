package io.github.afgprojects.framework.core.cache;

/**
 * Null 值包装类
 * <p>
 * 用于缓存 null 值，防止缓存穿透
 * 当缓存值为 null 时，存入此对象作为标记
 * </p>
 */
public final class NullValue {

    /**
     * 单例实例
     */
    public static final NullValue INSTANCE = new NullValue();

    /**
     * 私有构造函数，防止外部创建实例
     */
    private NullValue() {
    }

    @Override
    public String toString() {
        return "NullValue.INSTANCE";
    }
}