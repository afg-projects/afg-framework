package io.github.afgprojects.framework.data.core.id;

import org.jspecify.annotations.NonNull;

/**
 * 标识符生成器接口
 * <p>
 * 参考 Hibernate IdentifierGenerator 设计
 */
public interface IdentifierGenerator {

    /**
     * 生成标识符
     *
     * @return 标识符
     */
    @NonNull Object generate();

    /**
     * 生成字符串标识符
     *
     * @return 字符串标识符
     */
    default @NonNull String generateString() {
        return generate().toString();
    }

    /**
     * 生成 Long 类型标识符
     *
     * @return Long 类型标识符
     * @throws UnsupportedOperationException 如果不支持 Long 类型
     */
    default long generateLong() {
        Object id = generate();
        if (id instanceof Number number) {
            return number.longValue();
        }
        throw new UnsupportedOperationException("This generator does not support Long type");
    }

    /**
     * 获取ID类型
     *
     * @return ID类型
     */
    @NonNull IdType getIdType();

    /**
     * 解析ID生成时间（如果支持）
     *
     * @param id 标识符
     * @return 生成时间戳（毫秒），如果不支持返回 -1
     */
    default long parseTimestamp(@NonNull Object id) {
        return -1;
    }

    /**
     * 判断是否支持时间解析
     *
     * @return true表示支持
     */
    default boolean supportsTimestampParsing() {
        return false;
    }
}