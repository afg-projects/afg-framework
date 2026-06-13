package io.github.afgprojects.framework.core.api.id;

import lombok.extern.slf4j.Slf4j;

/**
 * Twitter Snowflake ID 生成器实现
 * <p>
 * 标准 Twitter Snowflake 算法，生成 64-bit 有序数值 ID。
 * 结构：1 bit sign + 41 bits timestamp + 5 bits datacenter + 5 bits worker + 12 bits sequence。
 * </p>
 * <p>
 * 特性：
 * <ul>
 *   <li>支持每毫秒生成 4096 个 ID（单节点）</li>
 *   <li>ID 总体趋势递增（毫秒级有序）</li>
 *   <li>支持时钟回拨检测，回拨超过阈值时抛出异常</li>
 *   <li>线程安全</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class SnowflakeIdGenerator implements IdGenerator {

    // ========== 位数分配 ==========

    /**
     * 机器 ID 位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心 ID 位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器 ID 最大值：31
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 数据中心 ID 最大值：31
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号掩码：4095
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器 ID 左移位数：12
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心 ID 左移位数：17
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数：22
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // ========== 实例字段 ==========

    private final long workerId;
    private final long datacenterId;
    private final long twepoch;
    private final long maxTolerateClockSkewMs;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param workerId     机器 ID（0-31）
     * @param datacenterId 数据中心 ID（0-31）
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this(workerId, datacenterId, 1288834974657L, 5L);
    }

    /**
     * 构造函数（完整参数）
     *
     * @param workerId              机器 ID（0-31）
     * @param datacenterId          数据中心 ID（0-31）
     * @param twepoch               起始纪元（毫秒）
     * @param maxTolerateClockSkewMs 最大容忍时钟回拨（毫秒）
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId, long twepoch, long maxTolerateClockSkewMs) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    "worker Id can't be greater than %d or less than 0".formatted(MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    "datacenter Id can't be greater than %d or less than 0".formatted(MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.twepoch = twepoch;
        this.maxTolerateClockSkewMs = maxTolerateClockSkewMs;

        log.debug("SnowflakeIdGenerator initialized: workerId={}, datacenterId={}, twepoch={}, maxTolerateClockSkewMs={}",
                workerId, datacenterId, twepoch, maxTolerateClockSkewMs);
    }

    @Override
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= maxTolerateClockSkewMs) {
                // 回拨在容忍范围内，等待时钟追上
                try {
                    wait(offset);
                    timestamp = currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new IllegalStateException(
                                "Clock moved backwards. Refusing to generate id for %d milliseconds".formatted(
                                        lastTimestamp - timestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Clock moved backwards. Refusing to generate id for %d milliseconds".formatted(offset), e);
                }
            } else {
                throw new IllegalStateException(
                        "Clock moved backwards. Refusing to generate id for %d milliseconds".formatted(offset));
            }
        }

        // 同一毫秒内序列递增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列重置为 0
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    @Override
    public String nextIdAsString() {
        return String.valueOf(nextId());
    }

    @Override
    public IdGeneratorType getType() {
        return IdGeneratorType.SNOWFLAKE;
    }

    /**
     * 阻塞到下一毫秒
     *
     * @param lastTimestamp 上次生成 ID 的时间戳
     * @return 下一毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
