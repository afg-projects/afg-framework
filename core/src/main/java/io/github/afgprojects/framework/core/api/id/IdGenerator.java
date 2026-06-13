package io.github.afgprojects.framework.core.api.id;

/**
 * 分布式 ID 生成器接口
 * <p>
 * 定义统一的分布式 ID 生成接口，支持多种 ID 生成策略。
 * 核心语义：{@code nextId} 生成数值型 ID，{@code nextIdAsString} 生成字符串型 ID，
 * {@code getType} 返回生成器类型。
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private IdGenerator idGenerator;
 *
 * // 生成数值型 ID
 * long id = idGenerator.nextId();
 *
 * // 生成字符串型 ID
 * String idStr = idGenerator.nextIdAsString();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface IdGenerator {

    /**
     * 生成下一个数值型 ID
     * <p>
     * 注意：UUID 类型的生成器不支持数值型 ID，调用将抛出 {@link UnsupportedOperationException}。
     * </p>
     *
     * @return 数值型 ID
     * @throws UnsupportedOperationException 如果生成器类型不支持数值型 ID（如 UUID）
     */
    long nextId();

    /**
     * 生成下一个字符串型 ID
     *
     * @return 字符串型 ID
     */
    String nextIdAsString();

    /**
     * 获取生成器类型
     *
     * @return ID 生成器类型
     */
    IdGeneratorType getType();
}
