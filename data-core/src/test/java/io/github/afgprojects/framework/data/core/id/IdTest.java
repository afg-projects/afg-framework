package io.github.afgprojects.framework.data.core.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ID 包测试
 */
@DisplayName("ID 包测试")
class IdTest {

    // ==================== IdType 枚举测试 ====================

    @Nested
    @DisplayName("IdType 枚举测试")
    class IdTypeTest {

        @Test
        @DisplayName("应包含所有 ID 类型")
        void shouldContainAllIdTypes() {
            assertThat(IdType.values()).containsExactly(
                    IdType.AUTO, IdType.NONE, IdType.INPUT,
                    IdType.ID_WORKER, IdType.ID_WORKER_STR,
                    IdType.UUID, IdType.UUID_HEX,
                    IdType.SNOWFLAKE, IdType.TIMESTAMP
            );
        }

        @Test
        @DisplayName("应正确返回描述信息")
        void shouldReturnDescription() {
            assertThat(IdType.AUTO.getDescription()).isEqualTo("数据库自增");
            assertThat(IdType.NONE.getDescription()).isEqualTo("无");
            assertThat(IdType.INPUT.getDescription()).isEqualTo("用户输入");
            assertThat(IdType.SNOWFLAKE.getDescription()).isEqualTo("雪花算法");
            assertThat(IdType.UUID.getDescription()).isEqualTo("UUID");
            assertThat(IdType.TIMESTAMP.getDescription()).isEqualTo("时间戳ID");
        }

        @Test
        @DisplayName("isDbGenerated 应正确判断")
        void shouldCheckDbGenerated() {
            assertThat(IdType.AUTO.isDbGenerated()).isTrue();
            assertThat(IdType.NONE.isDbGenerated()).isFalse();
            assertThat(IdType.SNOWFLAKE.isDbGenerated()).isFalse();
            assertThat(IdType.UUID.isDbGenerated()).isFalse();
        }

        @Test
        @DisplayName("isAppGenerated 应正确判断")
        void shouldCheckAppGenerated() {
            // AUTO, NONE, INPUT 不是应用生成
            assertThat(IdType.AUTO.isAppGenerated()).isFalse();
            assertThat(IdType.NONE.isAppGenerated()).isFalse();
            assertThat(IdType.INPUT.isAppGenerated()).isFalse();

            // 其他都是应用生成
            assertThat(IdType.SNOWFLAKE.isAppGenerated()).isTrue();
            assertThat(IdType.UUID.isAppGenerated()).isTrue();
            assertThat(IdType.UUID_HEX.isAppGenerated()).isTrue();
            assertThat(IdType.ID_WORKER.isAppGenerated()).isTrue();
            assertThat(IdType.ID_WORKER_STR.isAppGenerated()).isTrue();
            assertThat(IdType.TIMESTAMP.isAppGenerated()).isTrue();
        }
    }

    // ==================== SnowflakeConfig 测试 ====================

    @Nested
    @DisplayName("SnowflakeConfig 测试")
    class SnowflakeConfigTest {

        @Test
        @DisplayName("应使用默认配置构造")
        void shouldCreateWithDefaultConfig() {
            SnowflakeConfig config = new SnowflakeConfig();

            assertThat(config.getWorkerId()).isEqualTo(0L);
            assertThat(config.getDatacenterId()).isEqualTo(0L);
            assertThat(config.getEpoch()).isEqualTo(1704038400000L); // 2024-01-01
            assertThat(config.getSequenceBits()).isEqualTo(12);
            assertThat(config.getWorkerIdBits()).isEqualTo(5);
            assertThat(config.getDatacenterIdBits()).isEqualTo(5);
        }

        @Test
        @DisplayName("应使用指定 workerId 构造")
        void shouldCreateWithWorkerId() {
            SnowflakeConfig config = new SnowflakeConfig(1L, 0L);

            assertThat(config.getWorkerId()).isEqualTo(1L);
            assertThat(config.getDatacenterId()).isEqualTo(0L);
        }

        @Test
        @DisplayName("应使用完整参数构造")
        void shouldCreateWithFullParams() {
            SnowflakeConfig config = new SnowflakeConfig(1L, 2L, 1704038400000L);

            assertThat(config.getWorkerId()).isEqualTo(1L);
            assertThat(config.getDatacenterId()).isEqualTo(2L);
            assertThat(config.getEpoch()).isEqualTo(1704038400000L);
        }

        @Test
        @DisplayName("应正确计算最大值")
        void shouldCalculateMaxValues() {
            SnowflakeConfig config = new SnowflakeConfig();

            // workerIdBits = 5, maxWorkerId = 31
            assertThat(config.getMaxWorkerId()).isEqualTo(31L);
            // datacenterIdBits = 5, maxDatacenterId = 31
            assertThat(config.getMaxDatacenterId()).isEqualTo(31L);
        }

        @Test
        @DisplayName("应正确计算位移量")
        void shouldCalculateShifts() {
            SnowflakeConfig config = new SnowflakeConfig();

            // workerIdShift = sequenceBits = 12
            assertThat(config.getWorkerIdShift()).isEqualTo(12L);
            // datacenterIdShift = sequenceBits + workerIdBits = 12 + 5 = 17
            assertThat(config.getDatacenterIdShift()).isEqualTo(17L);
            // timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits = 12 + 5 + 5 = 22
            assertThat(config.getTimestampLeftShift()).isEqualTo(22L);
        }

        @Test
        @DisplayName("应正确计算序列号掩码")
        void shouldCalculateSequenceMask() {
            SnowflakeConfig config = new SnowflakeConfig();

            // sequenceBits = 12, sequenceMask = 4095
            assertThat(config.getSequenceMask()).isEqualTo(4095L);
        }

        @Test
        @DisplayName("无效 workerId 应抛出异常")
        void shouldThrowExceptionForInvalidWorkerId() {
            assertThatThrownBy(() -> new SnowflakeConfig(32L, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("worker Id");

            assertThatThrownBy(() -> new SnowflakeConfig(-1L, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("worker Id");
        }

        @Test
        @DisplayName("无效 datacenterId 应抛出异常")
        void shouldThrowExceptionForInvalidDatacenterId() {
            assertThatThrownBy(() -> new SnowflakeConfig(0L, 32L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datacenter Id");

            assertThatThrownBy(() -> new SnowflakeConfig(0L, -1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datacenter Id");
        }
    }

    // ==================== SnowflakeIdGenerator 测试 ====================

    @Nested
    @DisplayName("SnowflakeIdGenerator 测试")
    class SnowflakeIdGeneratorTest {

        @Test
        @DisplayName("应使用默认配置构造")
        void shouldCreateWithDefaultConfig() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            assertThat(generator.getIdType()).isEqualTo(IdType.SNOWFLAKE);
            assertThat(generator.getConfig()).isNotNull();
            assertThat(generator.getConfig().getWorkerId()).isEqualTo(0L);
        }

        @Test
        @DisplayName("应使用指定 workerId 构造")
        void shouldCreateWithWorkerId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L);

            assertThat(generator.getConfig().getWorkerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应使用指定 workerId 和 datacenterId 构造")
        void shouldCreateWithWorkerIdAndDatacenterId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, 2L);

            assertThat(generator.getConfig().getWorkerId()).isEqualTo(1L);
            assertThat(generator.getConfig().getDatacenterId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("应使用 SnowflakeConfig 构造")
        void shouldCreateWithConfig() {
            SnowflakeConfig config = new SnowflakeConfig(5L, 3L);
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(config);

            assertThat(generator.getConfig()).isSameAs(config);
            assertThat(generator.getConfig().getWorkerId()).isEqualTo(5L);
            assertThat(generator.getConfig().getDatacenterId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("应使用最大 workerId 构造")
        void shouldCreateWithMaxWorkerId() {
            // 最大 workerId = 31 (5 bits)
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(31L);

            assertThat(generator.getConfig().getWorkerId()).isEqualTo(31L);
            assertThat(generator.generateLong()).isPositive();
        }

        @Test
        @DisplayName("应使用最大 datacenterId 构造")
        void shouldCreateWithMaxDatacenterId() {
            // 最大 datacenterId = 31 (5 bits)
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0L, 31L);

            assertThat(generator.getConfig().getDatacenterId()).isEqualTo(31L);
            assertThat(generator.generateLong()).isPositive();
        }

        @Test
        @DisplayName("应生成唯一的 Long 类型 ID")
        void shouldGenerateUniqueLongIds() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
            Set<Long> ids = new HashSet<>();

            for (int i = 0; i < 10000; i++) {
                long id = generator.generateLong();
                assertThat(ids).doesNotContain(id);
                ids.add(id);
            }

            assertThat(ids).hasSize(10000);
        }

        @Test
        @DisplayName("应生成正数 ID")
        void shouldGeneratePositiveIds() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            for (int i = 0; i < 100; i++) {
                long id = generator.generateLong();
                assertThat(id).isPositive();
            }
        }

        @Test
        @DisplayName("generate 应返回 Long 类型")
        void shouldReturnLongFromGenerate() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            Object id = generator.generate();
            assertThat(id).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("generateString 应返回字符串")
        void shouldReturnStringFromGenerateString() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            String id = generator.generateString();
            assertThat(id).isNotEmpty();
            assertThat(Long.parseLong(id)).isPositive();
        }

        @Test
        @DisplayName("应支持时间戳解析")
        void shouldSupportTimestampParsing() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            assertThat(generator.supportsTimestampParsing()).isTrue();
        }

        @Test
        @DisplayName("应正确解析 ID 中的时间戳")
        void shouldParseTimestampFromId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            long beforeGenerate = System.currentTimeMillis();
            long id = generator.generateLong();
            long afterGenerate = System.currentTimeMillis();

            long parsedTimestamp = generator.parseTimestamp(id);

            // 解析的时间戳应在生成时间附近（允许一定误差）
            assertThat(parsedTimestamp).isBetween(beforeGenerate - 1000, afterGenerate + 1000);
        }

        @Test
        @DisplayName("应正确解析字符串 ID 中的时间戳")
        void shouldParseTimestampFromStringId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            long beforeGenerate = System.currentTimeMillis();
            String id = generator.generateString();
            long afterGenerate = System.currentTimeMillis();

            long parsedTimestamp = generator.parseTimestamp(id);

            assertThat(parsedTimestamp).isBetween(beforeGenerate - 1000, afterGenerate + 1000);
        }

        @Test
        @DisplayName("解析无效 ID 应返回 -1")
        void shouldReturnNegativeOneForInvalidId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            assertThat(generator.parseTimestamp("invalid")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("parseTimestamp 应解析 Long 类型 ID")
        void shouldParseTimestampFromLongId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            long id = generator.generateLong();
            Long idObj = Long.valueOf(id);

            long parsedTimestamp = generator.parseTimestamp(idObj);

            assertThat(parsedTimestamp).isPositive();
        }

        @Test
        @DisplayName("parseTimestamp 应解析 Integer 类型 ID")
        void shouldParseTimestampFromIntegerId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            // Integer 类型会调用 Number.longValue()
            long id = generator.generateLong();
            Integer truncatedId = (int) (id & Integer.MAX_VALUE);

            long parsedTimestamp = generator.parseTimestamp(truncatedId);

            // 由于截断，结果可能不正确，但不应抛异常
            assertThat(parsedTimestamp).isNotNegative();
        }

        @Test
        @DisplayName("parseTimestamp 应解析 Double 类型 ID")
        void shouldParseTimestampFromDoubleId() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            long id = generator.generateLong();
            Double idAsDouble = Double.valueOf(id);

            long parsedTimestamp = generator.parseTimestamp(idAsDouble);

            assertThat(parsedTimestamp).isPositive();
        }

        @Test
        @DisplayName("parseTimestamp 对不支持类型应返回 -1")
        void shouldReturnNegativeOneForUnsupportedType() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            // 非 Number 和非 String 类型
            assertThat(generator.parseTimestamp(new Object())).isEqualTo(-1L);
            assertThat(generator.parseTimestamp(List.of("test"))).isEqualTo(-1L);
            assertThat(generator.parseTimestamp(Map.of())).isEqualTo(-1L);
        }

        @Test
        @DisplayName("parseTimestamp 对空字符串应返回 -1")
        void shouldReturnNegativeOneForEmptyString() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            assertThat(generator.parseTimestamp("")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("parseTimestamp 对 null 字符串格式应返回 -1")
        void shouldReturnNegativeOneForMalformedString() {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

            // 空格和特殊字符
            assertThat(generator.parseTimestamp("  ")).isEqualTo(-1L);
            assertThat(generator.parseTimestamp("abc123")).isEqualTo(-1L);
            assertThat(generator.parseTimestamp("123abc")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("时钟回拨时应抛出 IllegalStateException")
        void shouldThrowExceptionWhenClockMovesBackwards() {
            // 创建一个可控时间的生成器
            TestableSnowflakeIdGenerator generator = new TestableSnowflakeIdGenerator();

            // 先生成一个 ID，记录时间戳
            generator.setCurrentTime(1000L);
            generator.generateLong();

            // 模拟时钟回拨
            generator.setCurrentTime(500L);

            assertThatThrownBy(generator::generateLong)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Clock moved backwards");
        }

        @Test
        @DisplayName("同一毫秒内序列号溢出时应等待下一毫秒")
        void shouldWaitForNextMillisecondWhenSequenceOverflow() {
            TestableSnowflakeIdGenerator generator = new TestableSnowflakeIdGenerator();

            // 设置初始时间
            generator.setCurrentTime(1000L);

            // 在同一毫秒内生成大量 ID，触发序列溢出
            // sequenceBits = 12，最大序列号 = 4095
            // 生成 4096 个 ID 后，序列号会溢出
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 5000; i++) {
                // 保持时间不变，直到序列溢出
                if (i < 4096) {
                    generator.setCurrentTime(1000L);
                } else {
                    // 序列溢出后，时间应该前进
                    generator.setCurrentTime(1001L);
                }
                long id = generator.generateLong();
                ids.add(id);
            }

            // 所有 ID 应唯一
            assertThat(ids).hasSize(5000);
        }

        @Test
        @DisplayName("ID 应包含正确的工作节点ID信息")
        void shouldContainCorrectWorkerId() {
            long workerId = 5L;
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(workerId);

            long id = generator.generateLong();
            SnowflakeConfig config = generator.getConfig();

            // 提取 workerId 部分
            long extractedWorkerId = (id >> config.getWorkerIdShift()) & config.getMaxWorkerId();
            assertThat(extractedWorkerId).isEqualTo(workerId);
        }

        @Test
        @DisplayName("ID 应包含正确的数据中心ID信息")
        void shouldContainCorrectDatacenterId() {
            long datacenterId = 7L;
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0L, datacenterId);

            long id = generator.generateLong();
            SnowflakeConfig config = generator.getConfig();

            // 提取 datacenterId 部分
            long extractedDatacenterId = (id >> config.getDatacenterIdShift()) & config.getMaxDatacenterId();
            assertThat(extractedDatacenterId).isEqualTo(datacenterId);
        }

        @Test
        @DisplayName("多线程环境下应生成唯一 ID")
        void shouldGenerateUniqueIdsInMultiThread() throws InterruptedException {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
            Set<Long> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(10);

            for (int t = 0; t < 10; t++) {
                new Thread(() -> {
                    for (int i = 0; i < 1000; i++) {
                        long id = generator.generateLong();
                        ids.add(id);
                    }
                    latch.countDown();
                }).start();
            }

            latch.await();
            assertThat(ids).hasSize(10000);
        }
    }

    /**
     * 可测试的雪花算法生成器，允许控制时间
     */
    static class TestableSnowflakeIdGenerator extends SnowflakeIdGenerator {
        private long currentTime = System.currentTimeMillis();

        public void setCurrentTime(long time) {
            this.currentTime = time;
        }

        @Override
        protected long currentTimeMillis() {
            return currentTime;
        }
    }

    // ==================== UuidGenerator 测试 ====================

    @Nested
    @DisplayName("UuidGenerator 测试")
    class UuidGeneratorTest {

        @Test
        @DisplayName("应生成带连字符的 UUID")
        void shouldGenerateUuidWithHyphens() {
            UuidGenerator generator = new UuidGenerator();

            Object id = generator.generate();

            assertThat(id).isInstanceOf(String.class);
            String uuid = (String) id;
            assertThat(uuid).hasSize(36);
            assertThat(uuid).contains("-");
            assertThat(uuid.chars().filter(ch -> ch == '-').count()).isEqualTo(4);
        }

        @Test
        @DisplayName("应生成无连字符的 UUID")
        void shouldGenerateUuidWithoutHyphens() {
            UuidGenerator generator = new UuidGenerator(true);

            Object id = generator.generate();

            assertThat(id).isInstanceOf(String.class);
            String uuid = (String) id;
            assertThat(uuid).hasSize(32);
            assertThat(uuid).doesNotContain("-");
        }

        @Test
        @DisplayName("应生成唯一的 UUID")
        void shouldGenerateUniqueUuids() {
            UuidGenerator generator = new UuidGenerator();
            Set<String> ids = new HashSet<>();

            for (int i = 0; i < 10000; i++) {
                String id = generator.generateString();
                assertThat(ids).doesNotContain(id);
                ids.add(id);
            }

            assertThat(ids).hasSize(10000);
        }

        @Test
        @DisplayName("generateString 应返回相同结果")
        void shouldReturnSameFromGenerateString() {
            UuidGenerator generator = new UuidGenerator();

            Object id1 = generator.generate();
            String id2 = generator.generateString();

            assertThat(id1).isInstanceOf(String.class);
            assertThat(id2).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("应返回正确的 ID 类型")
        void shouldReturnCorrectIdType() {
            UuidGenerator withHyphens = new UuidGenerator(false);
            UuidGenerator withoutHyphens = new UuidGenerator(true);

            assertThat(withHyphens.getIdType()).isEqualTo(IdType.UUID);
            assertThat(withoutHyphens.getIdType()).isEqualTo(IdType.UUID_HEX);
        }

        @Test
        @DisplayName("不应支持时间戳解析")
        void shouldNotSupportTimestampParsing() {
            UuidGenerator generator = new UuidGenerator();

            assertThat(generator.supportsTimestampParsing()).isFalse();
            assertThat(generator.parseTimestamp("any-id")).isEqualTo(-1L);
        }
    }

    // ==================== TimestampIdGenerator 测试 ====================

    @Nested
    @DisplayName("TimestampIdGenerator 测试")
    class TimestampIdGeneratorTest {

        @Test
        @DisplayName("应生成时间戳 ID")
        void shouldGenerateTimestampId() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            Object id = generator.generate();

            assertThat(id).isInstanceOf(String.class);
            String timestampId = (String) id;
            // 格式：yyyyMMddHHmmssSSS + 3位序号 = 17 + 3 = 20 位
            assertThat(timestampId).hasSize(20);
            assertThat(timestampId).matches("\\d{20}");
        }

        @Test
        @DisplayName("应生成唯一的 ID")
        void shouldGenerateUniqueIds() {
            TimestampIdGenerator generator = new TimestampIdGenerator();
            Set<String> ids = new HashSet<>();

            for (int i = 0; i < 1000; i++) {
                String id = generator.generateString();
                assertThat(ids).doesNotContain(id);
                ids.add(id);
            }

            assertThat(ids).hasSize(1000);
        }

        @Test
        @DisplayName("应返回正确的 ID 类型")
        void shouldReturnCorrectIdType() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            assertThat(generator.getIdType()).isEqualTo(IdType.TIMESTAMP);
        }

        @Test
        @DisplayName("应支持时间戳解析")
        void shouldSupportTimestampParsing() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            assertThat(generator.supportsTimestampParsing()).isTrue();
        }

        @Test
        @DisplayName("应正确解析 ID 中的时间戳")
        void shouldParseTimestampFromId() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            long beforeGenerate = System.currentTimeMillis();
            String id = generator.generateString();
            long afterGenerate = System.currentTimeMillis();

            long parsedTimestamp = generator.parseTimestamp(id);

            // 解析的时间戳应在生成时间附近（允许一定误差）
            assertThat(parsedTimestamp).isBetween(beforeGenerate - 60000, afterGenerate + 60000);
        }

        @Test
        @DisplayName("解析无效 ID 应返回 -1")
        void shouldReturnNegativeOneForInvalidId() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            assertThat(generator.parseTimestamp("invalid")).isEqualTo(-1L);
            assertThat(generator.parseTimestamp("short")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("generateString 应返回相同结果")
        void shouldReturnSameFromGenerateString() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            Object id1 = generator.generate();
            String id2 = generator.generateString();

            assertThat(id1).isInstanceOf(String.class);
            assertThat(id2).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("parseTimestamp 应处理边界长度")
        void shouldHandleBoundaryLength() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            // 长度刚好 17 位
            assertThat(generator.parseTimestamp("20240101120000000")).isNotNegative();

            // 长度小于 17 位
            assertThat(generator.parseTimestamp("2024010112000000")).isEqualTo(-1L);

            // 长度大于 17 位（正常 ID）
            assertThat(generator.parseTimestamp("20240101120000000123")).isNotNegative();
        }

        @Test
        @DisplayName("parseTimestamp 应处理无效日期格式")
        void shouldHandleInvalidDateFormat() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            // 无效月份
            assertThat(generator.parseTimestamp("20241301120000000")).isEqualTo(-1L);

            // 无效日期
            assertThat(generator.parseTimestamp("20240132120000000")).isEqualTo(-1L);

            // 非数字字符
            assertThat(generator.parseTimestamp("20240101abc000000")).isEqualTo(-1L);
        }

        @Test
        @DisplayName("parseTimestamp 应处理非字符串类型")
        void shouldHandleNonStringType() {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            // 数字类型
            assertThat(generator.parseTimestamp(12345L)).isEqualTo(-1L);

            // null 类型（通过 Object 参数）
            assertThat(generator.parseTimestamp(new Object())).isEqualTo(-1L);
        }

        @Test
        @DisplayName("同一毫秒内应递增序号")
        void shouldIncrementSequenceInSameMillisecond() throws InterruptedException {
            TimestampIdGenerator generator = new TimestampIdGenerator();

            // 快速生成多个 ID，部分可能在同一毫秒内
            String firstId = generator.generateString();
            String timestampPart = firstId.substring(0, 17);
            int firstSeq = Integer.parseInt(firstId.substring(17));

            // 立即生成第二个 ID
            String secondId = generator.generateString();
            String secondTimestampPart = secondId.substring(0, 17);
            int secondSeq = Integer.parseInt(secondId.substring(17));

            // 如果时间相同，序号应递增
            if (timestampPart.equals(secondTimestampPart)) {
                assertThat(secondSeq).isEqualTo(firstSeq + 1);
            }
        }
    }

    // ==================== IdentifierGenerator 接口测试 ====================

    @Nested
    @DisplayName("IdentifierGenerator 接口测试")
    class IdentifierGeneratorInterfaceTest {

        @Test
        @DisplayName("SnowflakeIdGenerator 应实现 IdentifierGenerator")
        void snowflakeShouldImplementInterface() {
            IdentifierGenerator generator = new SnowflakeIdGenerator();

            assertThat(generator.generate()).isNotNull();
            assertThat(generator.generateString()).isNotNull();
            assertThat(generator.generateLong()).isPositive();
            assertThat(generator.getIdType()).isEqualTo(IdType.SNOWFLAKE);
        }

        @Test
        @DisplayName("UuidGenerator 应实现 IdentifierGenerator")
        void uuidShouldImplementInterface() {
            IdentifierGenerator generator = new UuidGenerator();

            assertThat(generator.generate()).isNotNull();
            assertThat(generator.generateString()).isNotNull();
            assertThat(generator.getIdType()).isEqualTo(IdType.UUID);
        }

        @Test
        @DisplayName("TimestampIdGenerator 应实现 IdentifierGenerator")
        void timestampShouldImplementInterface() {
            IdentifierGenerator generator = new TimestampIdGenerator();

            assertThat(generator.generate()).isNotNull();
            assertThat(generator.generateString()).isNotNull();
            assertThat(generator.getIdType()).isEqualTo(IdType.TIMESTAMP);
        }

        @Test
        @DisplayName("UuidGenerator generateLong 应抛出异常")
        void uuidGenerateLongShouldThrow() {
            IdentifierGenerator generator = new UuidGenerator();

            assertThatThrownBy(generator::generateLong)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("does not support Long type");
        }
    }
}
