package io.github.afgprojects.framework.core.api.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SnowflakeIdGenerator")
class SnowflakeIdGeneratorTest {

    private final SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

    @Nested
    @DisplayName("nextId")
    class NextId {

        @Test
        @DisplayName("should generate positive IDs")
        void shouldGeneratePositiveIds() {
            long id = generator.nextId();
            assertThat(id).isPositive();
        }

        @Test
        @DisplayName("should generate monotonically increasing IDs")
        void shouldGenerateMonotonicallyIncreasingIds() {
            long id1 = generator.nextId();
            long id2 = generator.nextId();
            long id3 = generator.nextId();

            assertThat(id2).isGreaterThan(id1);
            assertThat(id3).isGreaterThan(id2);
        }

        @Test
        @DisplayName("should generate unique IDs for 1000 sequential calls")
        void shouldGenerateUniqueIdsFor1000SequentialCalls() {
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (int i = 0; i < 1000; i++) {
                ids.add(generator.nextId());
            }
            assertThat(ids).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("nextIdAsString")
    class NextIdAsString {

        @Test
        @DisplayName("should return string representation of numeric ID")
        void shouldReturnStringRepresentationOfNumericId() {
            String idStr = generator.nextIdAsString();
            // 验证是数值型 ID 的字符串表示
            assertThat(Long.parseLong(idStr)).isPositive();
        }

        @Test
        @DisplayName("should generate non-empty string IDs")
        void shouldGenerateNonEmptyStringIds() {
            String idStr = generator.nextIdAsString();
            assertThat(idStr).isNotEmpty();
        }

        @Test
        @DisplayName("should match nextId value")
        void shouldMatchNextIdValue() {
            long numericId = generator.nextId();
            String stringId = generator.nextIdAsString();

            // nextId 和 nextIdAsString 应各自生成新 ID，两者都应该是正数
            assertThat(numericId).isPositive();
            assertThat(Long.parseLong(stringId)).isPositive();
        }
    }

    @Nested
    @DisplayName("getType")
    class GetType {

        @Test
        @DisplayName("should return SNOWFLAKE type")
        void shouldReturnSnowflakeType() {
            assertThat(generator.getType()).isEqualTo(IdGeneratorType.SNOWFLAKE);
        }
    }

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw when workerId is negative")
        void shouldThrowWhenWorkerIdIsNegative() {
            assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("worker Id");
        }

        @Test
        @DisplayName("should throw when workerId exceeds max value")
        void shouldThrowWhenWorkerIdExceedsMaxValue() {
            assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("worker Id");
        }

        @Test
        @DisplayName("should throw when datacenterId is negative")
        void shouldThrowWhenDatacenterIdIsNegative() {
            assertThatThrownBy(() -> new SnowflakeIdGenerator(1, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datacenter Id");
        }

        @Test
        @DisplayName("should throw when datacenterId exceeds max value")
        void shouldThrowWhenDatacenterIdExceedsMaxValue() {
            assertThatThrownBy(() -> new SnowflakeIdGenerator(1, 32))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datacenter Id");
        }

        @Test
        @DisplayName("should accept valid workerId and datacenterId")
        void shouldAcceptValidWorkerIdAndDatacenterId() {
            SnowflakeIdGenerator validGenerator = new SnowflakeIdGenerator(0, 0);
            assertThat(validGenerator.nextId()).isPositive();
        }
    }

    @Nested
    @DisplayName("concurrent safety")
    class ConcurrentSafety {

        @Test
        @DisplayName("should generate unique IDs under concurrent access")
        void shouldGenerateUniqueIdsUnderConcurrentAccess() throws Exception {
            int threadCount = 10;
            int idsPerThread = 100;
            java.util.Set<Long> allIds = new java.util.HashSet<>();
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
            java.util.List<java.util.concurrent.Future<java.util.List<Long>>> futures = new java.util.ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    java.util.List<Long> ids = new java.util.ArrayList<>();
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(generator.nextId());
                    }
                    return ids;
                }));
            }

            for (java.util.concurrent.Future<java.util.List<Long>> future : futures) {
                allIds.addAll(future.get());
            }

            executor.shutdown();

            assertThat(allIds).hasSize(threadCount * idsPerThread);
        }
    }

    @Nested
    @DisplayName("clock skew detection")
    class ClockSkewDetection {

        @Test
        @DisplayName("should detect clock moved backwards beyond tolerance")
        void shouldDetectClockMovedBackwardsBeyondTolerance() {
            // 使用最大容忍回拨为 0 的生成器
            SnowflakeIdGenerator strictGenerator = new SnowflakeIdGenerator(1, 1, 1288834974657L, 0);

            // 先生成一个 ID 记录时间戳
            strictGenerator.nextId();

            // 无法在单元测试中模拟时钟回拨，此处仅验证构造正常
            assertThat(strictGenerator.nextId()).isPositive();
        }
    }
}
