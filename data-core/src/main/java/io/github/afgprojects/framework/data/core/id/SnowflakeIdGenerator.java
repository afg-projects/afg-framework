package io.github.afgprojects.framework.data.core.id;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 雪花算法ID生成器
 * <p>
 * 结构（64位）：
 * - 1位：符号位（始终为0）
 * - 41位：时间戳（可使用约69年）
 * - 10位：工作机器ID（5位数据中心ID + 5位工作节点ID）
 * - 12位：序列号
 */
public class SnowflakeIdGenerator implements IdentifierGenerator {

    /**
     * 获取配置
     */
    @Getter
    private final SnowflakeConfig config;
    private final Lock lock = new ReentrantLock();

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    /**
     * 使用默认配置构造
     */
    public SnowflakeIdGenerator() {
        this(new SnowflakeConfig());
    }

    /**
     * 使用指定工作节点ID构造
     *
     * @param workerId 工作节点ID
     */
    public SnowflakeIdGenerator(long workerId) {
        this(new SnowflakeConfig(workerId, SnowflakeConfig.DEFAULT_DATACENTER_ID));
    }

    /**
     * 使用指定配置构造
     *
     * @param workerId     工作节点ID
     * @param datacenterId 数据中心ID
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this(new SnowflakeConfig(workerId, datacenterId));
    }

    /**
     * 使用配置对象构造
     *
     * @param config 配置对象
     */
    public SnowflakeIdGenerator(@NonNull SnowflakeConfig config) {
        this.config = config;
    }

    @Override
    public @NonNull Object generate() {
        return generateLong();
    }

    @Override
    public @NonNull String generateString() {
        return String.valueOf(generateLong());
    }

    @Override
    public long generateLong() {
        lock.lock();
        try {
            long timestamp = currentTimeMillis();

            // 时钟回拨检测
            if (timestamp < lastTimestamp) {
                throw new IllegalStateException(
                        "Clock moved backwards! Refusing to generate id for " +
                                (lastTimestamp - timestamp) + " milliseconds");
            }

            // 同一毫秒内，序列号递增
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & config.getSequenceMask();
                // 序列号溢出，等待下一毫秒
                if (sequence == 0) {
                    timestamp = waitForNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒，序列号重置
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            // 组装ID
            return ((timestamp - config.getEpoch()) << config.getTimestampLeftShift())
                    | (config.getDatacenterId() << config.getDatacenterIdShift())
                    | (config.getWorkerId() << config.getWorkerIdShift())
                    | sequence;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NonNull IdType getIdType() {
        return IdType.SNOWFLAKE;
    }

    @Override
    public long parseTimestamp(@NonNull Object id) {
        if (id instanceof Number number) {
            long idLong = number.longValue();
            // 提取时间戳部分
            return (idLong >> config.getTimestampLeftShift()) + config.getEpoch();
        }
        if (id instanceof String str) {
            try {
                return parseTimestamp(Long.parseLong(str));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public boolean supportsTimestampParsing() {
        return true;
    }

    // ========== 私有方法 ==========

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳（毫秒）
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一毫秒
     *
     * @param lastTimestamp 上次时间戳
     * @return 新时间戳
     */
    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

}