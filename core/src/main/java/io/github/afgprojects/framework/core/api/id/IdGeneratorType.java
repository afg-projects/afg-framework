package io.github.afgprojects.framework.core.api.id;

/**
 * ID 生成器类型枚举
 * <p>
 * 定义支持的分布式 ID 生成策略。
 * </p>
 *
 * <ul>
 *   <li>{@link #NONE} — NoOp 降级，本地自增计数器，不具备分布式安全性</li>
 *   <li>{@link #SNOWFLAKE} — Twitter Snowflake 算法，64-bit 有序数值 ID</li>
 *   <li>{@link #SEGMENT} — 号段模式，从数据库预分配 ID 段（计划支持）</li>
 *   <li>{@link #UUID} — UUID 随机字符串 ID</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum IdGeneratorType {

    /**
     * 无 ID 生成策略（NoOp 降级）
     * <p>
     * 表示当前 ID 生成器为本地降级实现，不具备分布式安全性。
     * 仅适用于单机降级场景或不需要分布式 ID 的场景。
     * </p>
     */
    NONE,

    /**
     * Twitter Snowflake 算法
     * <p>
     * 64-bit 结构：1 bit sign + 41 bits timestamp + 5 bits datacenter + 5 bits worker + 12 bits sequence。
     * 支持每毫秒生成 4096 个 ID，总体趋势递增。
     * </p>
     */
    SNOWFLAKE,

    /**
     * 号段模式
     * <p>
     * 从数据库预分配 ID 段，减少数据库访问次数。
     * 适用于高并发场景（计划支持）。
     * </p>
     */
    SEGMENT,

    /**
     * UUID 模式
     * <p>
     * 基于 {@link java.util.UUID#randomUUID()} 生成，仅支持字符串型 ID。
     * 不保证有序性，适用于不需要排序的场景。
     * </p>
     */
    UUID
}
