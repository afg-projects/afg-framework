package io.github.afgprojects.framework.data.core.id;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;

/**
 * 时间戳ID生成器
 * <p>
 * 格式：yyyyMMddHHmmssSSS + 3位序号
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class TimestampIdGenerator implements IdentifierGenerator {

    /**
     * 日期时间格式
     */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 序号（线程安全）
     */
    private volatile int sequence = 0;

    /**
     * 上一次生成时间
     */
    private volatile String lastTimestamp = "";

    /**
     * 构造时间戳ID生成器
     */
    public TimestampIdGenerator() {
    }

    @Override
    public synchronized @NonNull Object generate() {
        String timestamp = getTimestamp();

        // 如果时间相同，递增序号
        if (timestamp.equals(lastTimestamp)) {
            sequence = (sequence + 1) % 1000;
            if (sequence == 0) {
                // 序号溢出，等待下一毫秒
                timestamp = waitForNextMillis(timestamp);
            }
        } else {
            // 时间变化，重置序号
            lastTimestamp = timestamp;
            sequence = 0;
        }

        return timestamp + String.format("%03d", sequence);
    }

    @Override
    public @NonNull String generateString() {
        return (String) generate();
    }

    @Override
    public @NonNull IdType getIdType() {
        return IdType.TIMESTAMP;
    }

    @Override
    public long parseTimestamp(@NonNull Object id) {
        if (id instanceof String str && str.length() >= 17) {
            try {
                String timestampStr = str.substring(0, 17);
                LocalDateTime ldt = LocalDateTime.parse(timestampStr, FORMATTER);
                return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public boolean supportsTimestampParsing() {
        return true;
    }

    /**
     * 获取当前时间戳字符串
     *
     * @return 时间戳字符串
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /**
     * 等待下一毫秒
     *
     * @param currentTimestamp 当前时间戳
     * @return 新时间戳
     */
    private String waitForNextMillis(String currentTimestamp) {
        String timestamp;
        do {
            timestamp = getTimestamp();
        } while (timestamp.equals(currentTimestamp));
        lastTimestamp = timestamp;
        sequence = 0;
        return timestamp;
    }
}