package io.github.afgprojects.framework.data.core.id;

import org.jspecify.annotations.NonNull;

/**
 * 雪花算法ID生成器配置
 */
public class SnowflakeConfig {

    /**
     * 默认工作节点ID
     */
    public static final long DEFAULT_WORKER_ID = 0L;

    /**
     * 默认数据中心ID
     */
    public static final long DEFAULT_DATACENTER_ID = 0L;

    /**
     * 工作节点ID（0-31）
     */
    private final long workerId;

    /**
     * 数据中心ID（0-31）
     */
    private final long datacenterId;

    /**
     * 起始时间戳（2024-01-01）
     */
    private final long epoch;

    /**
     * 序列号位数
     */
    private final int sequenceBits;

    /**
     * 工作节点ID位数
     */
    private final int workerIdBits;

    /**
     * 数据中心ID位数
     */
    private final int datacenterIdBits;

    /**
     * 使用默认配置构造
     */
    public SnowflakeConfig() {
        this(DEFAULT_WORKER_ID, DEFAULT_DATACENTER_ID);
    }

    /**
     * 构造配置
     *
     * @param workerId     工作节点ID
     * @param datacenterId 数据中心ID
     */
    public SnowflakeConfig(long workerId, long datacenterId) {
        this(workerId, datacenterId, 1704038400000L); // 2024-01-01 00:00:00 UTC
    }

    /**
     * 构造配置
     *
     * @param workerId     工作节点ID
     * @param datacenterId 数据中心ID
     * @param epoch        起始时间戳
     */
    public SnowflakeConfig(long workerId, long datacenterId, long epoch) {
        this(workerId, datacenterId, epoch, 12, 5, 5);
    }

    /**
     * 完整构造
     *
     * @param workerId       工作节点ID
     * @param datacenterId   数据中心ID
     * @param epoch          起始时间戳
     * @param sequenceBits   序列号位数
     * @param workerIdBits   工作节点ID位数
     * @param datacenterIdBits 数据中心ID位数
     */
    public SnowflakeConfig(long workerId, long datacenterId, long epoch,
                           int sequenceBits, int workerIdBits, int datacenterIdBits) {
        validateWorkerId(workerId, workerIdBits);
        validateDatacenterId(datacenterId, datacenterIdBits);

        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.epoch = epoch;
        this.sequenceBits = sequenceBits;
        this.workerIdBits = workerIdBits;
        this.datacenterIdBits = datacenterIdBits;
    }

    private void validateWorkerId(long workerId, int workerIdBits) {
        long maxWorkerId = ~(-1L << workerIdBits);
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException(
                    "worker Id can't be greater than " + maxWorkerId + " or less than 0");
        }
    }

    private void validateDatacenterId(long datacenterId, int datacenterIdBits) {
        long maxDatacenterId = ~(-1L << datacenterIdBits);
        if (datacenterId < 0 || datacenterId > maxDatacenterId) {
            throw new IllegalArgumentException(
                    "datacenter Id can't be greater than " + maxDatacenterId + " or less than 0");
        }
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getEpoch() {
        return epoch;
    }

    public int getSequenceBits() {
        return sequenceBits;
    }

    public int getWorkerIdBits() {
        return workerIdBits;
    }

    public int getDatacenterIdBits() {
        return datacenterIdBits;
    }

    public long getMaxWorkerId() {
        return ~(-1L << workerIdBits);
    }

    public long getMaxDatacenterId() {
        return ~(-1L << datacenterIdBits);
    }

    public long getWorkerIdShift() {
        return sequenceBits;
    }

    public long getDatacenterIdShift() {
        return sequenceBits + workerIdBits;
    }

    public long getTimestampLeftShift() {
        return sequenceBits + workerIdBits + datacenterIdBits;
    }

    public long getSequenceMask() {
        return ~(-1L << sequenceBits);
    }
}