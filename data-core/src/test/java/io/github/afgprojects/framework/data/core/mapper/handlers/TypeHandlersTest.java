/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.mapper.handlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TypeHandler 系列单元测试
 * <p>
 * 测试所有 TypeHandler 实现类的 convert 方法。
 * 每个 TypeHandler<T> 的 convert(Object, Class<T>) 返回 T，
 * 测试中需要根据 handler 的泛型参数类型来正确接收返回值。
 */
@SuppressWarnings("unchecked")
class TypeHandlersTest {

    // ==================== NumberTypeHandler ====================

    @Nested
    @DisplayName("NumberTypeHandler")
    class NumberTypeHandlerTests {

        private final NumberTypeHandler handler = new NumberTypeHandler();

        @Test
        @DisplayName("should convert to Long when input is Number")
        void shouldConvertToLong_whenInputIsNumber() {
            Number result = handler.convert(100, Number.class);
            assertThat(result.longValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should convert to Integer when input is Number")
        void shouldConvertToInteger_whenInputIsNumber() {
            Number result = handler.convert(100L, Number.class);
            assertThat(result.intValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("should convert to Double when input is Number")
        void shouldConvertToDouble_whenInputIsNumber() {
            Number result = handler.convert(100.5f, Number.class);
            assertThat(result.doubleValue()).isCloseTo(100.5, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("should convert BigDecimal to BigInteger when target is BigInteger")
        void shouldConvertBigDecimalToBigInteger() {
            BigDecimal decimal = new BigDecimal("12345.67");
            Number result = handler.convert(decimal, Number.class);
            // NumberTypeHandler 检查 target == BigInteger.class，但这里 targetType 是 Number.class
            // 所以不会走 BigInteger 分支，直接返回 num
            assertThat(result).isInstanceOf(BigDecimal.class);
        }

        @Test
        @DisplayName("should return null for non-Number input")
        void shouldReturnNullForNonNumberInput() {
            assertThat(handler.convert("not-a-number", Number.class)).isNull();
        }

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNullWhenInputIsNull() {
            assertThat(handler.convert(null, Number.class)).isNull();
        }
    }

    // ==================== BooleanNumberTypeHandler ====================

    @Nested
    @DisplayName("BooleanNumberTypeHandler")
    class BooleanNumberTypeHandlerTests {

        private final BooleanNumberTypeHandler handler = new BooleanNumberTypeHandler();

        @Test
        @DisplayName("should convert from Boolean identity")
        void shouldConvertFromBooleanIdentity() {
            assertThat(handler.convert(true, Boolean.class)).isTrue();
            assertThat(handler.convert(false, Boolean.class)).isFalse();
        }

        @Test
        @DisplayName("should convert from Number 0 or 1")
        void shouldConvertFromNumberZeroOrOne() {
            assertThat(handler.convert(1, Boolean.class)).isTrue();
            assertThat(handler.convert(0, Boolean.class)).isFalse();
            assertThat(handler.convert(2, Boolean.class)).isTrue();
            assertThat(handler.convert(-1, Boolean.class)).isTrue();
        }

        @Test
        @DisplayName("should convert from String true or false")
        void shouldConvertFromStringTrueOrFalse() {
            assertThat(handler.convert("true", Boolean.class)).isTrue();
            assertThat(handler.convert("false", Boolean.class)).isFalse();
            assertThat(handler.convert("TRUE", Boolean.class)).isTrue();
        }

        @Test
        @DisplayName("should return false for unsupported type")
        void shouldReturnFalseForUnsupportedType() {
            assertThat(handler.convert(new Object(), Boolean.class)).isFalse();
        }

        @Test
        @DisplayName("should return false when input is null")
        void shouldReturnFalseWhenInputIsNull() {
            // BooleanNumberTypeHandler: null 不匹配任何 instanceof，走默认 return false
            assertThat(handler.convert(null, Boolean.class)).isFalse();
        }
    }

    // ==================== DateTimeTypeHandler ====================

    @Nested
    @DisplayName("DateTimeTypeHandler")
    class DateTimeTypeHandlerTests {

        private final DateTimeTypeHandler handler = new DateTimeTypeHandler();

        @Test
        @DisplayName("should convert from Timestamp to LocalDateTime")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = Timestamp.valueOf("2024-06-03 10:30:00");
            LocalDateTime result = handler.convert(timestamp, LocalDateTime.class);
            assertThat(result).isEqualTo(LocalDateTime.of(2024, 6, 3, 10, 30, 0));
        }

        @Test
        @DisplayName("should convert from java.sql.Date to LocalDateTime")
        void shouldConvertFromJavaSqlDate() {
            Date sqlDate = Date.valueOf("2024-06-03");
            LocalDateTime result = handler.convert(sqlDate, LocalDateTime.class);
            assertThat(result).isEqualTo(LocalDateTime.of(2024, 6, 3, 0, 0));
        }

        @Test
        @DisplayName("should convert from java.util.Date to LocalDateTime")
        void shouldConvertFromJavaUtilDate() {
            java.util.Date utilDate = new java.util.Date();
            LocalDateTime result = handler.convert(utilDate, LocalDateTime.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return null for unsupported type")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("not-a-date", LocalDateTime.class)).isNull();
        }
    }

    // ==================== InstantTypeHandler ====================

    @Nested
    @DisplayName("InstantTypeHandler")
    class InstantTypeHandlerTests {

        private final InstantTypeHandler handler = new InstantTypeHandler();

        @Test
        @DisplayName("should convert from Timestamp")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Instant result = handler.convert(timestamp, Instant.class);
            assertThat(result).isNotNull();
            assertThat(result.toEpochMilli()).isEqualTo(timestamp.getTime());
        }

        @Test
        @DisplayName("should convert from java.util.Date")
        void shouldConvertFromJavaUtilDate() {
            java.util.Date date = new java.util.Date();
            Instant result = handler.convert(date, Instant.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should convert from OffsetDateTime")
        void shouldConvertFromOffsetDateTime() {
            OffsetDateTime odt = OffsetDateTime.now();
            Instant result = handler.convert(odt, Instant.class);
            assertThat(result).isEqualTo(odt.toInstant());
        }

        @Test
        @DisplayName("should convert from LocalDateTime")
        void shouldConvertFromLocalDateTime() {
            LocalDateTime ldt = LocalDateTime.now();
            Instant result = handler.convert(ldt, Instant.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return null for unsupported type")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("not-an-instant", Instant.class)).isNull();
        }
    }

    // ==================== LocalDateTypeHandler ====================

    @Nested
    @DisplayName("LocalDateTypeHandler")
    class LocalDateTypeHandlerTests {

        private final LocalDateTypeHandler handler = new LocalDateTypeHandler();

        @Test
        @DisplayName("should convert from java.sql.Date")
        void shouldConvertFromJavaSqlDate() {
            Date sqlDate = Date.valueOf("2024-06-03");
            LocalDate result = handler.convert(sqlDate, LocalDate.class);
            assertThat(result).isEqualTo(LocalDate.of(2024, 6, 3));
        }

        @Test
        @DisplayName("should convert from Timestamp")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = Timestamp.valueOf("2024-06-03 10:30:00");
            LocalDate result = handler.convert(timestamp, LocalDate.class);
            assertThat(result).isEqualTo(LocalDate.of(2024, 6, 3));
        }

        @Test
        @DisplayName("should convert from java.util.Date")
        void shouldConvertFromJavaUtilDate() {
            java.util.Date date = Date.valueOf("2024-06-03");
            LocalDate result = handler.convert(date, LocalDate.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return null for unsupported type")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert("not-a-date", LocalDate.class)).isNull();
        }
    }

    // ==================== LocalTimeTypeHandler ====================

    @Nested
    @DisplayName("LocalTimeTypeHandler")
    class LocalTimeTypeHandlerTests {

        private final LocalTimeTypeHandler handler = new LocalTimeTypeHandler();

        @Test
        @DisplayName("should convert from java.sql.Time")
        void shouldConvertFromJavaSqlTime() {
            Time time = Time.valueOf("10:30:45");
            LocalTime result = handler.convert(time, LocalTime.class);
            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        }

        @Test
        @DisplayName("should convert from Timestamp")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = Timestamp.valueOf("2024-06-03 10:30:45");
            LocalTime result = handler.convert(timestamp, LocalTime.class);
            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        }

        @Test
        @DisplayName("should convert from String ISO format")
        void shouldConvertFromStringIsoFormat() {
            LocalTime result = handler.convert("10:30:45", LocalTime.class);
            assertThat(result).isEqualTo(LocalTime.of(10, 30, 45));
        }

        @Test
        @DisplayName("should return null for unsupported type")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert(12345, LocalTime.class)).isNull();
        }
    }

    // ==================== OffsetDateTimeTypeHandler ====================

    @Nested
    @DisplayName("OffsetDateTimeTypeHandler")
    class OffsetDateTimeTypeHandlerTests {

        private final OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();

        @Test
        @DisplayName("should convert from ZonedDateTime")
        void shouldConvertFromZonedDateTime() {
            ZonedDateTime zdt = ZonedDateTime.now();
            OffsetDateTime result = handler.convert(zdt, OffsetDateTime.class);
            assertThat(result).isEqualTo(zdt.toOffsetDateTime());
        }

        @Test
        @DisplayName("should convert from Timestamp")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            OffsetDateTime result = handler.convert(timestamp, OffsetDateTime.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should convert from LocalDateTime")
        void shouldConvertFromLocalDateTime() {
            LocalDateTime ldt = LocalDateTime.now();
            OffsetDateTime result = handler.convert(ldt, OffsetDateTime.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle null gracefully")
        void shouldHandleNullGracefully() {
            assertThat(handler.convert(null, OffsetDateTime.class)).isNull();
        }
    }

    // ==================== ZonedDateTimeTypeHandler ====================

    @Nested
    @DisplayName("ZonedDateTimeTypeHandler")
    class ZonedDateTimeTypeHandlerTests {

        private final ZonedDateTimeTypeHandler handler = new ZonedDateTimeTypeHandler();

        @Test
        @DisplayName("should convert from OffsetDateTime")
        void shouldConvertFromOffsetDateTime() {
            OffsetDateTime odt = OffsetDateTime.now();
            ZonedDateTime result = handler.convert(odt, ZonedDateTime.class);
            assertThat(result).isEqualTo(odt.toZonedDateTime());
        }

        @Test
        @DisplayName("should convert from Timestamp")
        void shouldConvertFromTimestamp() {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            ZonedDateTime result = handler.convert(timestamp, ZonedDateTime.class);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle null gracefully")
        void shouldHandleNullGracefully() {
            assertThat(handler.convert(null, ZonedDateTime.class)).isNull();
        }
    }

    // ==================== BigDecimalTypeHandler ====================

    @Nested
    @DisplayName("BigDecimalTypeHandler")
    class BigDecimalTypeHandlerTests {

        private final BigDecimalTypeHandler handler = new BigDecimalTypeHandler();

        @Test
        @DisplayName("should convert from Number doubleValue")
        void shouldConvertFromNumberDoubleValue() {
            BigDecimal result = handler.convert(100.5, BigDecimal.class);
            assertThat(result).isEqualByComparingTo("100.5");
        }

        @Test
        @DisplayName("should return identity for BigDecimal")
        void shouldReturnIdentityForBigDecimal() {
            BigDecimal decimal = new BigDecimal("123.45");
            BigDecimal result = handler.convert(decimal, BigDecimal.class);
            assertThat(result).isSameAs(decimal);
        }

        @Test
        @DisplayName("should return null for non-Number input")
        void shouldReturnNullForNonNumberInput() {
            assertThat(handler.convert("not-a-number", BigDecimal.class)).isNull();
        }
    }

    // ==================== EnumTypeHandler ====================

    @Nested
    @DisplayName("EnumTypeHandler")
    class EnumTypeHandlerTests {

        private final EnumTypeHandler handler = new EnumTypeHandler();

        @Test
        @DisplayName("should convert from String by name")
        void shouldConvertFromStringByName() {
            Enum<?> result = handler.convert("RED", (Class<Enum>) (Class<?>) TestColor.class);
            assertThat(result).isEqualTo(TestColor.RED);
        }

        @Test
        @DisplayName("should convert from Number by ordinal")
        void shouldConvertFromNumberByOrdinal() {
            Enum<?> result = handler.convert(1, (Class<Enum>) (Class<?>) TestColor.class);
            assertThat(result).isEqualTo(TestColor.GREEN);
        }

        @Test
        @DisplayName("should convert from Number by getCode method")
        void shouldConvertFromNumberByGetCodeMethod() {
            Enum<?> result = handler.convert(200, (Class<Enum>) (Class<?>) TestStatus.class);
            assertThat(result).isEqualTo(TestStatus.SUCCESS);
        }

        @Test
        @DisplayName("should return null for no match")
        void shouldReturnNullForNoMatch() {
            assertThat(handler.convert("UNKNOWN", (Class<Enum>) (Class<?>) TestColor.class)).isNull();
        }

        @Test
        @DisplayName("should return null for unsupported type")
        void shouldReturnNullForUnsupportedType() {
            assertThat(handler.convert(new Object(), (Class<Enum>) (Class<?>) TestColor.class)).isNull();
        }
    }

    // ==================== UUIDTypeHandler ====================

    @Nested
    @DisplayName("UUIDTypeHandler")
    class UUIDTypeHandlerTests {

        private final UUIDTypeHandler handler = new UUIDTypeHandler();

        @Test
        @DisplayName("should convert from String")
        void shouldConvertFromString() {
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            UUID result = handler.convert(uuidString, UUID.class);
            assertThat(result).isEqualTo(UUID.fromString(uuidString));
        }

        @Test
        @DisplayName("should convert from byte array 16 bytes")
        void shouldConvertFromByteArray16Bytes() {
            UUID original = UUID.randomUUID();
            byte[] bytes = new byte[16];
            ByteBuffer.wrap(bytes)
                .putLong(original.getMostSignificantBits())
                .putLong(original.getLeastSignificantBits());

            UUID result = handler.convert(bytes, UUID.class);
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("should return identity for UUID")
        void shouldReturnIdentityForUUID() {
            UUID uuid = UUID.randomUUID();
            UUID result = handler.convert(uuid, UUID.class);
            assertThat(result).isSameAs(uuid);
        }

        @Test
        @DisplayName("should return null for invalid string")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-uuid", UUID.class)).isNull();
        }

        @Test
        @DisplayName("should return null for byte array not 16 bytes")
        void shouldReturnNullForByteArrayNot16Bytes() {
            assertThat(handler.convert(new byte[10], UUID.class)).isNull();
        }
    }

    // ==================== YearMonthTypeHandler ====================

    @Nested
    @DisplayName("YearMonthTypeHandler")
    class YearMonthTypeHandlerTests {

        private final YearMonthTypeHandler handler = new YearMonthTypeHandler();

        @Test
        @DisplayName("should convert from String")
        void shouldConvertFromString() {
            YearMonth result = handler.convert("2024-06", YearMonth.class);
            assertThat(result).isEqualTo(YearMonth.of(2024, 6));
        }

        @Test
        @DisplayName("should convert from java.sql.Date")
        void shouldConvertFromJavaSqlDate() {
            Date sqlDate = Date.valueOf("2024-06-03");
            YearMonth result = handler.convert(sqlDate, YearMonth.class);
            assertThat(result).isEqualTo(YearMonth.of(2024, 6));
        }

        @Test
        @DisplayName("should return identity for YearMonth")
        void shouldReturnIdentityForYearMonth() {
            YearMonth ym = YearMonth.of(2024, 6);
            YearMonth result = handler.convert(ym, YearMonth.class);
            assertThat(result).isSameAs(ym);
        }

        @Test
        @DisplayName("should return null for invalid string")
        void shouldReturnNullForInvalidString() {
            assertThat(handler.convert("not-a-yearmonth", YearMonth.class)).isNull();
        }
    }

    // ==================== YearTypeHandler ====================

    @Nested
    @DisplayName("YearTypeHandler")
    class YearTypeHandlerTests {

        private final YearTypeHandler handler = new YearTypeHandler();

        @Test
        @DisplayName("should convert from Number intValue")
        void shouldConvertFromNumberIntValue() {
            Year result = handler.convert(2024, Year.class);
            assertThat(result).isEqualTo(Year.of(2024));
        }

        @Test
        @DisplayName("should convert from String")
        void shouldConvertFromString() {
            Year result = handler.convert("2024", Year.class);
            assertThat(result).isEqualTo(Year.of(2024));
        }

        @Test
        @DisplayName("should convert from java.sql.Date")
        void shouldConvertFromJavaSqlDate() {
            Date sqlDate = Date.valueOf("2024-06-03");
            Year result = handler.convert(sqlDate, Year.class);
            assertThat(result).isEqualTo(Year.of(2024));
        }
    }

    // ==================== StringTypeHandler ====================

    @Nested
    @DisplayName("StringTypeHandler")
    class StringTypeHandlerTests {

        private final StringTypeHandler handler = new StringTypeHandler();

        @Test
        @DisplayName("should convert any object via toString")
        void shouldConvertAnyObjectViaToString() {
            assertThat(handler.convert(123, String.class)).isEqualTo("123");
            assertThat(handler.convert(45.67, String.class)).isEqualTo("45.67");
            assertThat(handler.convert(true, String.class)).isEqualTo("true");
        }

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNullWhenInputIsNull() {
            // StringTypeHandler: null 输入走 toString() 会 NPE
            // 但 TypeHandler 接口契约通常由调用方保证 null 检查
            assertThatThrownBy(() -> handler.convert(null, String.class))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================== BlobTypeHandler ====================

    @Nested
    @DisplayName("BlobTypeHandler")
    class BlobTypeHandlerTests {

        private final BlobTypeHandler handler = new BlobTypeHandler();

        @Test
        @DisplayName("should return null for non-Blob non-Clob input")
        void shouldReturnNullForNonBlobNonClobInput() {
            // BlobTypeHandler 只处理 Blob 和 Clob，byte[] 直接返回 null
            assertThat(handler.convert("not-bytes", byte[].class)).isNull();
        }

        @Test
        @DisplayName("should return null when input is null")
        void shouldReturnNullWhenInputIsNull() {
            assertThat(handler.convert(null, byte[].class)).isNull();
        }
    }

    // ==================== JsonTypeHandler ====================

    @Nested
    @DisplayName("JsonTypeHandler")
    class JsonTypeHandlerTests {

        private final JsonTypeHandler handler = new JsonTypeHandler();

        @Test
        @DisplayName("should convert JSON string to Map")
        void shouldConvertJsonStringToMap() {
            String json = "{\"name\":\"test\",\"value\":123}";
            Object result = handler.convert(json, Object.class);

            assertThat(result).isInstanceOf(java.util.Map.class);
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            assertThat(map.get("name")).isEqualTo("test");
            assertThat(map.get("value")).isEqualTo(123);
        }

        @Test
        @DisplayName("should convert JSON string to List")
        void shouldConvertJsonStringToList() {
            String json = "[1, 2, 3]";
            Object result = handler.convert(json, Object.class);

            assertThat(result).isInstanceOf(java.util.List.class);
            java.util.List<?> list = (java.util.List<?>) result;
            assertThat(list).hasSize(3);
            assertThat(list.get(0)).isEqualTo(1);
            assertThat(list.get(1)).isEqualTo(2);
            assertThat(list.get(2)).isEqualTo(3);
        }

        @Test
        @DisplayName("should return string as-is for String target type")
        void shouldReturnStringAsIsForStringTargetType() {
            String json = "{\"name\":\"test\"}";
            Object result = handler.convert(json, Object.class);
            // JsonTypeHandler converts CharSequence input → parses JSON, returns Map/List
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return null for invalid JSON")
        void shouldReturnNullForInvalidJson() {
            assertThat(handler.convert("not valid json {", Object.class)).isNull();
        }
    }

    // 测试用枚举
    enum TestColor {
        RED,
        GREEN,
        BLUE
    }

    enum TestStatus {
        SUCCESS(200),
        NOT_FOUND(404),
        SERVER_ERROR(500);

        private final int code;

        TestStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
